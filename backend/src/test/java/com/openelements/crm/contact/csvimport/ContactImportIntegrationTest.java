package com.openelements.crm.contact.csvimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.security.roles.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactImportIntegrationTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        seedSystemUser();
    }

    @Test
    void previewParsesCommaDelimitedCsv() throws Exception {
        final String csv = """
            Vorname,Nachname,Email
            Holger,Dyroff,holger@example.com
            Sandra,Parsick,sandra@example.com
            """;

        mockMvc.perform(importMultipart("/api/contacts/import/preview", csv, "UTF-8", true, null)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delimiter").value(","))
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.columns[0]").value("Vorname"))
            .andExpect(jsonPath("$.sampleRows.length()").value(2))
            .andExpect(jsonPath("$.sampleContacts").doesNotExist());
    }

    @Test
    void previewWithMappingReturnsSampleContacts() throws Exception {
        final String csv = """
            Vorname,Nachname,Email
            Holger,Dyroff,holger@example.com
            Sandra,Parsick,not-an-email
            """;

        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME",
            "Email", "EMAIL"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/preview", csv, "UTF-8", true, mapping)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleContacts[0].contact.firstName").value("Holger"))
            .andExpect(jsonPath("$.sampleContacts[0].errors").isEmpty())
            .andExpect(jsonPath("$.sampleContacts[1].errors[0].field").value("email"))
            .andExpect(jsonPath("$.sampleContacts[1].errors[0].reason").value("invalid"));
    }

    @Test
    void commitImportsValidRowsWithPartialSuccess() throws Exception {
        final String csv = """
            Vorname,Nachname,Email
            Holger,Dyroff,holger@example.com
            ,Parsick,missing-first@example.com
            Sandra,Parsick,sandra@example.com
            """;

        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME",
            "Email", "EMAIL"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/commit", csv, "UTF-8", true, mapping)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdCount").value(2))
            .andExpect(jsonPath("$.failedCount").value(1))
            .andExpect(jsonPath("$.failures[0].row").value(2))
            .andExpect(jsonPath("$.failures[0].field").value("firstName"))
            .andExpect(jsonPath("$.failures[0].reason").value("required"));

        assertThat(contactRepository.count()).isEqualTo(2);
    }

    @Test
    void itAdminCanImport() throws Exception {
        final String csv = "Vorname,Nachname\nJohn,Doe\n";
        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/commit", csv, "UTF-8", true, mapping)
                .with(roles(List.of(Roles.ROLE_IT_ADMIN))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(importMultipart("/api/contacts/import/preview", "Vorname,Nachname\nA,B\n", "UTF-8", true, null)
                .with(roles(List.of())))
            .andExpect(status().isForbidden());
    }

    @Test
    void unsupportedEncodingReturns415() throws Exception {
        mockMvc.perform(importMultipart("/api/contacts/import/preview", "Vorname,Nachname\nA,B\n", "UTF-16", true, null)
                .with(appAdmin()))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void tooManyRowsReturns413() throws Exception {
        final StringBuilder csv = new StringBuilder("Vorname,Nachname\n");
        for (int i = 0; i < 5001; i++) {
            csv.append("A,B\n");
        }

        mockMvc.perform(importMultipart("/api/contacts/import/preview", csv.toString(), "UTF-8", true, null)
                .with(appAdmin()))
            .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void previewExtractsLinkedInFromCommaSeparatedSocialLinks() throws Exception {
        final String csv = """
            Vorname,Nachname,Social
            Holger,Dyroff,"https://linkedin.com/in/holger, https://example.com"
            Sandra,Parsick,https://example.com
            """;

        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME",
            "Social", "LINKEDIN_URL"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/preview", csv, "UTF-8", true, mapping)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleContacts[0].contact.linkedInUrl").value("holger"))
            .andExpect(jsonPath("$.sampleContacts[0].errors").isEmpty())
            .andExpect(jsonPath("$.sampleContacts[1].contact.linkedInUrl").doesNotExist())
            .andExpect(jsonPath("$.sampleContacts[1].errors").isEmpty());
    }

    @Test
    void previewSkipsWebsiteOnlyValueWhenMappedToLinkedIn() throws Exception {
        final String csv = """
            Vorname,Nachname,Social
            Thomas,Müller,https://uni-example.de
            """;

        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME",
            "Social", "LINKEDIN_URL"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/preview", csv, "UTF-8", true, mapping)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleContacts[0].contact.linkedInUrl").doesNotExist())
            .andExpect(jsonPath("$.sampleContacts[0].errors").isEmpty());
    }

    @Test
    void commitImportsLinkedInFromCommaSeparatedSocialLinks() throws Exception {
        final String csv = """
            Vorname,Nachname,Social
            Holger,Dyroff,"https://linkedin.com/in/holger, https://example.com"
            """;

        final Map<String, String> mapping = Map.of(
            "Vorname", "FIRST_NAME",
            "Nachname", "LAST_NAME",
            "Social", "LINKEDIN_URL"
        );

        mockMvc.perform(importMultipart("/api/contacts/import/commit", csv, "UTF-8", true, mapping)
                .with(appAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdCount").value(1))
            .andExpect(jsonPath("$.failedCount").value(0));
    }

    private static MockMultipartFile csvFile(final String content) {
        return new MockMultipartFile(
            "file",
            "contacts.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartHttpServletRequestBuilder importMultipart(
        final String url,
        final String csv,
        final String encoding,
        final boolean hasHeader,
        final Map<String, String> mapping) throws Exception {
        final MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(new ContactImportRequest(encoding, hasHeader, mapping))
        );
        return multipart(url)
            .file(csvFile(csv))
            .file(requestPart);
    }

    private static RequestPostProcessor appAdmin() {
        return roles(List.of(Roles.ROLE_APP_ADMIN));
    }

    private static RequestPostProcessor roles(final List<String> roleNames) {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("email", "test@example.com")
            .claim("roles", roleNames)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roleNames) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return jwt().jwt(jwt).authorities(authorities);
    }
}
