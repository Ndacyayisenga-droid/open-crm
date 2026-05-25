package com.openelements.crm.updates;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class UpdatesControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogDataService auditLogDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity alice;

    @BeforeEach
    void resetAuditLog() {
        jdbcTemplate.update("DELETE FROM audit_log");
        seedSystemUser();
        alice = ensureUser("alice", "Alice");
    }

    private UserEntity ensureUser(final String sub, final String name) {
        return userRepository.findBySub(sub).orElseGet(() -> {
            final UserEntity entity = new UserEntity();
            entity.setSub(sub);
            entity.setName(name);
            return userRepository.saveAndFlush(entity);
        });
    }

    private static MockHttpServletRequestBuilder asUser(final MockHttpServletRequestBuilder builder, final List<String> roles) {
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
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/updates"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void normalAuthenticatedUserCanReadUpdates() throws Exception {
        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void itAdminCanReadUpdates() throws Exception {
        mockMvc.perform(asUser(get("/api/updates"), List.of("IT-ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void defaultSizeIs20() throws Exception {
        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(20))
            .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void size50IsAccepted() throws Exception {
        mockMvc.perform(asUser(get("/api/updates").param("size", "50"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void size100IsAccepted() throws Exception {
        mockMvc.perform(asUser(get("/api/updates").param("size", "100"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void size200IsAccepted() throws Exception {
        mockMvc.perform(asUser(get("/api/updates").param("size", "200"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(200));
    }

    @Test
    void disallowedSizesReturn400() throws Exception {
        for (final String s : List.of("17", "0", "-5", "1000")) {
            mockMvc.perform(asUser(get("/api/updates").param("size", s), List.of()))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void pageGreaterThanZeroReturnsEmptyContent() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), "Test Name", AuditAction.INSERT, alice);

        mockMvc.perform(asUser(get("/api/updates").param("size", "20").param("page", "2"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void responseShapeIsPage() throws Exception {
        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page.size").exists())
            .andExpect(jsonPath("$.page.number").exists())
            .andExpect(jsonPath("$.page.totalElements").exists())
            .andExpect(jsonPath("$.page.totalPages").exists());
    }

    @Test
    void contentReflectsTotalElements() throws Exception {
        final CompanyEntity c1 = newCompany("Acme");
        final CompanyEntity c2 = newCompany("Globex");
        auditLogDataService.createEntry("CompanyDto", c1.getId(), "Test Name", AuditAction.INSERT, alice);
        auditLogDataService.createEntry("CompanyDto", c2.getId(), "Test Name", AuditAction.INSERT, alice);

        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void companyEntryExposesEntityHasLogoFlag() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        company.setLogo(new byte[]{1, 2, 3});
        company.setLogoContentType("image/png");
        companyRepository.saveAndFlush(company);
        auditLogDataService.createEntry("CompanyDto", company.getId(), "Test Name", AuditAction.UPDATE, alice);

        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].entityHasLogo").value(true))
            .andExpect(jsonPath("$.content[0].entityHasPhoto").value(false));
    }

    @Test
    void deletedCompanyEntryExposesBothFlagsFalse() throws Exception {
        auditLogDataService.createEntry("CompanyDto", java.util.UUID.randomUUID(), "Test Name", AuditAction.DELETE, alice);

        mockMvc.perform(asUser(get("/api/updates"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].entityHasLogo").value(false))
            .andExpect(jsonPath("$.content[0].entityHasPhoto").value(false));
    }

    private CompanyEntity newCompany(final String name) {
        final CompanyEntity company = new CompanyEntity();
        company.setName(name);
        return companyRepository.saveAndFlush(company);
    }
}
