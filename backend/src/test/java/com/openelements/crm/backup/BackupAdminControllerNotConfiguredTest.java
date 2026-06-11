package com.openelements.crm.backup;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.spring.base.services.dbbackup.DbBackupClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Verifies the "not configured" behaviour of GET /api/admin/backup/status when the API token is
 * blank. The 'configured' boolean must be false and {@link DbBackupClient} must not be called.
 */
@TestPropertySource(properties = "openelements.db-backup.api-token=")
class BackupAdminControllerNotConfiguredTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DbBackupClient client;

    private static MockHttpServletRequestBuilder asItAdmin(MockHttpServletRequestBuilder builder) {
        final List<String> roles = List.of("IT-ADMIN");
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
    void statusReportsNotConfiguredWithoutCallingClient() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/admin/backup/status")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.healthy").value(false))
            .andExpect(jsonPath("$.info").doesNotExist());

        verify(client, never()).isHealthy();
        verify(client, never()).getInfo();
    }
}
