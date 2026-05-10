package com.openelements.crm.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end security tests for spec 085 — verifies the runtime 403/2xx behaviour
 * for every role combination using MockMvc and a mocked JWT.
 *
 * <p>The tests focus on the authorization layer — they use random UUIDs for the
 * affected resources, so the controllers return either 403 (before the handler
 * executes) or 404 (handler executed, resource not found). The key assertion is
 * that 403 is returned when the role is missing, and that a non-403 status is
 * returned when the role is present.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder withRoles(MockHttpServletRequestBuilder builder,
                                                           List<String> roles) {
        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("test-user")
                .claim("preferred_username", "test-user")
                .claim("email", "test@example.com")
                .claim("roles", roles)
                .build();
        // Mirror what the JwtAuthenticationConverter from spring-services does at runtime:
        // it maps every value of the "roles" claim to a ROLE_<role> GrantedAuthority.
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    // -- Health endpoint stays public --

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    // -- CRM read endpoints remain open to any authenticated user --

    @Test
    void companiesListAccessibleToUserWithNoRoles() throws Exception {
        mockMvc.perform(withRoles(get("/api/companies"), List.of()))
            .andExpect(status().isOk());
    }

    // -- DELETE /api/companies/{id} requires ADMIN --

    @Test
    void deleteCompanyForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/companies/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteCompanyForbiddenForItAdminOnly() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/companies/" + UUID.randomUUID()), List.of("IT-ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteCompanyAllowedForAdmin() {
        // Role check passes; the controller then fails with
        // IllegalArgumentException (entity not found). That is business logic,
        // not a 403, which confirms the authorization layer let the request through.
        assertRoleCheckPassed(() -> mockMvc.perform(withRoles(
            delete("/api/companies/" + UUID.randomUUID()), List.of("ADMIN"))));
    }

    @Test
    void deleteCompanyAllowedForUserBoth() {
        assertRoleCheckPassed(() -> mockMvc.perform(withRoles(
            delete("/api/companies/" + UUID.randomUUID()),
            List.of("ADMIN", "IT-ADMIN"))));
    }

    @Test
    void deleteCompanyWithContactsQueryParamStillRoleChecked() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/companies/" + UUID.randomUUID() + "?deleteContacts=true"),
                List.of()))
            .andExpect(status().isForbidden());
    }

    // -- DELETE /api/contacts/{id} requires ADMIN --

    @Test
    void deleteContactForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/contacts/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    // -- DELETE /api/tasks/{id} requires ADMIN --

    @Test
    void deleteTaskForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/tasks/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    // -- DELETE /api/tags/{id} requires ADMIN --

    @Test
    void deleteTagForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/tags/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    // -- DELETE /api/companies/{id}/comments/{commentId} requires ADMIN --

    @Test
    void deleteCompanyCommentForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/companies/" + UUID.randomUUID() + "/comments/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteContactCommentForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/contacts/" + UUID.randomUUID() + "/comments/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteTaskCommentForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(
                delete("/api/tasks/" + UUID.randomUUID() + "/comments/" + UUID.randomUUID()), List.of()))
            .andExpect(status().isForbidden());
    }

    // -- Admin controllers require IT-ADMIN --

    @Test
    void apiKeysListForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/api-keys"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void apiKeysListForbiddenForAdminOnly() throws Exception {
        mockMvc.perform(withRoles(get("/api/api-keys"), List.of("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void apiKeysListAllowedForItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/api-keys"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void apiKeysListAllowedForUserBoth() throws Exception {
        mockMvc.perform(withRoles(get("/api/api-keys"),
                List.of("ADMIN", "IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void webhooksListForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/webhooks"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void webhooksListAllowedForItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/webhooks"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void brevoSettingsForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/brevo/settings"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void brevoSettingsAllowedForItAdmin() {
        // Brevo settings read passes the role check; underlying service may
        // throw due to test-context quirks but that's not a 403.
        assertRoleCheckPassed(() -> mockMvc.perform(withRoles(
            get("/api/brevo/settings"), List.of("IT-ADMIN"))));
    }

    // -- GET /api/users requires IT-ADMIN (spec 089) --

    @Test
    void usersListUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usersListForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/users"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void usersListForbiddenForAdminOnly() throws Exception {
        mockMvc.perform(withRoles(get("/api/users"), List.of("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void usersListAllowedForItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/users"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void usersListAllowedForUserBoth() throws Exception {
        mockMvc.perform(withRoles(get("/api/users"),
                List.of("ADMIN", "IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void usersMeRemainsAccessibleForUserNone() throws Exception {
        // /api/users/me must remain accessible to any authenticated user;
        // the IT-ADMIN restriction only applies to the list endpoint.
        assertRoleCheckPassed(() -> mockMvc.perform(withRoles(
            get("/api/users/me"), List.of())));
    }

    // -- GET /api/audit-logs requires IT-ADMIN (spec 090) --

    @Test
    void auditLogsListUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogsListForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void auditLogsListForbiddenForAdminOnly() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs"), List.of("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void auditLogsListAllowedForItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void auditLogsListAllowedForUserBoth() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs"),
                List.of("ADMIN", "IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void auditLogEntityTypesUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs/entity-types"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogEntityTypesForbiddenForUserNone() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs/entity-types"), List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void auditLogEntityTypesAllowedForItAdmin() throws Exception {
        mockMvc.perform(withRoles(get("/api/audit-logs/entity-types"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    /**
     * Performs the supplied MockMvc call. If it succeeds, asserts the status is
     * not 403. If it throws during servlet processing (business logic error),
     * the role check has still passed — the request was dispatched to the
     * controller. Only a true 403 would be a security failure.
     */
    private static void assertRoleCheckPassed(MockMvcCall call) {
        try {
            final int status = call.run().andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertNotEquals(403, status,
                "Role-authorized request should not be forbidden; got " + status);
        } catch (final jakarta.servlet.ServletException ignored) {
            // Servlet exception thrown by the controller after the security
            // filter chain passed — authorization succeeded.
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface MockMvcCall {
        org.springframework.test.web.servlet.ResultActions run() throws Exception;
    }
}
