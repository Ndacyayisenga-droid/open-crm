package com.openelements.crm.search;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end integration tests for the search endpoint. Boots the full
 * application against a real PostgreSQL + Meilisearch and exercises the
 * controller via MockMvc.
 *
 * <p>Each test calls {@link #waitForBootstrap()} once to make sure the
 * startup reindex has finished; otherwise the controller would 503.
 */
class SearchIntegrationTest extends AbstractSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private SearchIndexState state;

    @BeforeEach
    void prepare() throws Exception {
        seedSystemUser();
        waitForBootstrap();
    }

    private void waitForBootstrap() throws InterruptedException {
        final long deadline = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
        while (state.isBootstrapping() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
    }

    private static <T extends MockHttpServletRequestBuilder> T asUser(final T builder) {
        final List<String> roles = List.of();
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        builder.with(jwt().jwt(jwt).authorities(authorities));
        return builder;
    }

    @Test
    void emptyQueryReturns200WithAllSectionsEmpty() throws Exception {
        mockMvc.perform(asUser(get("/api/search").param("q", "")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value(""))
            .andExpect(jsonPath("$.companies").isArray())
            .andExpect(jsonPath("$.companies.length()").value(0))
            .andExpect(jsonPath("$.contacts.length()").value(0))
            .andExpect(jsonPath("$.tags.length()").value(0))
            .andExpect(jsonPath("$.comments.length()").value(0));
    }

    @Test
    void singleCharacterQueryReturnsEmptySections() throws Exception {
        mockMvc.perform(asUser(get("/api/search").param("q", "a")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companies.length()").value(0))
            .andExpect(jsonPath("$.contacts.length()").value(0));
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "anything"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createdContactIsFoundAfterEventSync() throws Exception {
        final ContactDto created = contactService.save(new ContactDto(
            null, null, "Hendrik", "Ebbers", "hendrik@example.com",
            "Founder", null, List.of(), null, null, null, null,
            0L, false, null, false, false, null, List.of(),
            Instant.now(), Instant.now()));
        assertNotNull(created.id());

        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "Hendrik")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contacts.length()").value(1))
            .andExpect(jsonPath("$.contacts[0].id").value(created.id().toString()))
            .andExpect(jsonPath("$.contacts[0].label").value("Hendrik Ebbers"));
    }

    @Test
    void typoToleranceFindsContactWithMisspelledQuery() throws Exception {
        contactService.save(new ContactDto(
            null, null, "Hendrik", "Ebbers", "hendrik@example.com",
            "Founder", null, List.of(), null, null, null, null,
            0L, false, null, false, false, null, List.of(),
            Instant.now(), Instant.now()));

        waitForIndex();

        // "hendirk" → should find "Hendrik" via Meilisearch's default typo tolerance.
        mockMvc.perform(asUser(get("/api/search").param("q", "hendirk")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contacts.length()").value(1))
            .andExpect(jsonPath("$.contacts[0].label").value("Hendrik Ebbers"));
    }

    @Test
    void deletedContactDisappearsFromSearch() throws Exception {
        final ContactDto created = contactService.save(new ContactDto(
            null, null, "Zenith", "Disappear", "zenith@example.com",
            null, null, List.of(), null, null, null, null,
            0L, false, null, false, false, null, List.of(),
            Instant.now(), Instant.now()));
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "Zenith")))
            .andExpect(jsonPath("$.contacts.length()").value(1));

        contactService.delete(created.id());
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "Zenith")))
            .andExpect(jsonPath("$.contacts.length()").value(0));
    }

    @Test
    void serviceUnavailableWithRetryAfterDuringBootstrap() throws Exception {
        state.markBootstrappingStarted();
        try {
            mockMvc.perform(asUser(get("/api/search").param("q", "anything")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.error").value("search index is initializing"));
        } finally {
            state.markBootstrappingFinished();
        }
    }

    @Test
    void createCompanyAndSearchByName() throws Exception {
        final CompanyDto created = companyService.save(new CompanyDto(
            null, "Open Elements GmbH", "info@open-elements.com", "https://open-elements.com",
            "Bonnstr.", "12", "53111", "Bonn", "DE", null, null, null, null, null, null,
            false, false, 0L, 0L, List.of(),
            Instant.now(), Instant.now()));
        assertNotNull(created.id());

        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "Open")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companies.length()").value(1))
            .andExpect(jsonPath("$.companies[0].id").value(created.id().toString()))
            .andExpect(jsonPath("$.companies[0].label").value("Open Elements GmbH"))
            // The highlight is HTML-safe — must contain literal <em> around the match,
            // not raw markup. Asserts the XSS-safe wrapping path.
            .andExpect(jsonPath("$.companies[0].highlight",
                org.hamcrest.Matchers.containsString("<em>Open</em>")));
    }

    @Test
    void htmlSpecialCharsInCompanyNameAreEscapedInHighlight() throws Exception {
        // A company named with HTML-like text must not produce raw markup
        // in the highlight (XSS guard regression test).
        final CompanyDto created = companyService.save(new CompanyDto(
            null, "<script>alert(1)</script> Co", null, null,
            null, null, null, null, null, null, null, null, null, null, null,
            false, false, 0L, 0L, List.of(),
            Instant.now(), Instant.now()));
        waitForIndex();

        mockMvc.perform(asUser(get("/api/search").param("q", "script")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companies.length()").value(1))
            .andExpect(jsonPath("$.companies[0].id").value(created.id().toString()))
            // Highlight must never contain a literal "<script>" — only the
            // escaped form "&lt;script&gt;".
            .andExpect(jsonPath("$.companies[0].highlight",
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))))
            .andExpect(jsonPath("$.companies[0].highlight",
                org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }
}
