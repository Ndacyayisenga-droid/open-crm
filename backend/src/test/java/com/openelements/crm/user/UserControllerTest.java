package com.openelements.crm.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Behaviour tests for the paginated GET /api/users endpoint added in spec 089.
 *
 * <p>Role-based 401/403/200 coverage lives in
 * {@link com.openelements.crm.security.SecurityRoleIntegrationTest}; this class
 * focuses on the pagination contract (default page size, explicit page/size,
 * response shape).
 */
class UserControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

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
    void listUsersReturnsPagedResponseShape() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/users")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.size").exists())
            .andExpect(jsonPath("$.page.number").exists())
            .andExpect(jsonPath("$.page.totalElements").exists())
            .andExpect(jsonPath("$.page.totalPages").exists());
    }

    @Test
    void listUsersUsesDefaultPageSize20WithoutParams() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/users")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(20))
            .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void listUsersHonoursExplicitPageAndSize() throws Exception {
        mockMvc.perform(asItAdmin(get("/api/users").param("page", "0").param("size", "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(10))
            .andExpect(jsonPath("$.page.number").value(0));
    }
}
