package com.openelements.crm.translation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.openelements.crm.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Behaviour tests for {@code /api/translate/*} that do not depend on a real upstream
 * translation API. The {@code application-test.yml} profile leaves the translation
 * properties blank, so the service is unconfigured for these tests — which is exactly
 * what the unconfigured-path scenarios in {@code behaviors.md} require.
 */
class TranslationControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        final List<String> roles = List.of("USER");
        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("test-user")
                .claim("preferred_username", "test-user")
                .claim("email", "test@example.com")
                .claim("roles", roles)
                .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    @Test
    void settingsReturnsConfiguredFalseWhenEnvVarsMissing() throws Exception {
        mockMvc.perform(asUser(get("/api/translate/settings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void settingsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/translate/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postTranslateReturns503WhenNotConfigured() throws Exception {
        final String body = "{\"text\":\"Hallo\",\"targetLanguage\":\"en\"}";
        mockMvc.perform(asUser(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void postTranslateRequiresAuthentication() throws Exception {
        final String body = "{\"text\":\"Hallo\",\"targetLanguage\":\"en\"}";
        mockMvc.perform(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postTranslateRejectsBlankText() throws Exception {
        final String body = "{\"text\":\"   \",\"targetLanguage\":\"en\"}";
        mockMvc.perform(asUser(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTranslateRejectsEmptyText() throws Exception {
        final String body = "{\"text\":\"\",\"targetLanguage\":\"en\"}";
        mockMvc.perform(asUser(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTranslateRejectsInvalidTargetLanguage() throws Exception {
        final String body = "{\"text\":\"hello\",\"targetLanguage\":\"fr\"}";
        mockMvc.perform(asUser(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }
}
