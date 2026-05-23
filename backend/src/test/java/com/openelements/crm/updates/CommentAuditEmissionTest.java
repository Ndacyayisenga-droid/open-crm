package com.openelements.crm.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import com.openelements.spring.base.services.audit.AuditLogDto;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.openelements.spring.base.security.roles.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentAuditEmissionTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogDataService auditLogDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedSystemUser() {
        // AbstractDbTest's @AfterEach truncates the previous test's data;
        // we only need to (re)seed the SystemUser row here.
        if (userRepository.findBySub(SystemUser.SUB).isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO users (id, sub, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SystemUser.ID, SystemUser.SUB, SystemUser.NAME);
        }
    }

    private static MockHttpServletRequestBuilder asUser(final MockHttpServletRequestBuilder builder, final List<String> roles) {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    private CompanyEntity newCompany(final String name) {
        final CompanyEntity c = new CompanyEntity();
        c.setName(name);
        return companyRepository.saveAndFlush(c);
    }

    private ContactEntity newContact() {
        final ContactEntity c = new ContactEntity();
        c.setFirstName("Jane");
        c.setLastName("Doe");
        return contactRepository.saveAndFlush(c);
    }

    private List<AuditLogDto> auditsFor(final String entityType, final UUID entityId) {
        return auditLogDataService.findByEntityType(entityType, PageRequest.of(0, 100)).getContent()
            .stream()
            .filter(a -> entityId.equals(a.entityId()))
            .toList();
    }

    @Test
    void addingCompanyCommentEmitsInsertAudit() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andExpect(status().isCreated());

        final List<AuditLogDto> entries = auditsFor("CompanyComment", company.getId());
        assertEquals(1, entries.size());
        assertEquals(AuditAction.INSERT, entries.get(0).action());
    }

    @Test
    void updatingCompanyCommentEmitsUpdateAudit() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                put("/api/companies/" + company.getId() + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"updated\"}"),
                List.of()))
            .andExpect(status().isOk());

        final List<AuditLogDto> entries = auditsFor("CompanyComment", company.getId());
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(a -> a.action() == AuditAction.UPDATE));
    }

    @Test
    void deletingCompanyCommentEmitsDeleteAudit() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                delete("/api/companies/" + company.getId() + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        final List<AuditLogDto> entries = auditsFor("CompanyComment", company.getId());
        assertTrue(entries.stream().anyMatch(a -> a.action() == AuditAction.DELETE));
    }

    @Test
    void addingContactCommentEmitsInsertAudit() throws Exception {
        final ContactEntity contact = newContact();
        mockMvc.perform(asUser(
                post("/api/contacts/" + contact.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andExpect(status().isCreated());

        final List<AuditLogDto> entries = auditsFor("ContactComment", contact.getId());
        assertEquals(1, entries.size());
        assertEquals(AuditAction.INSERT, entries.get(0).action());
    }

    @Test
    void updatingContactCommentEmitsUpdateAudit() throws Exception {
        final ContactEntity contact = newContact();
        final String body = mockMvc.perform(asUser(
                post("/api/contacts/" + contact.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                put("/api/contacts/" + contact.getId() + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"updated\"}"),
                List.of()))
            .andExpect(status().isOk());

        final List<AuditLogDto> entries = auditsFor("ContactComment", contact.getId());
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(a -> a.action() == AuditAction.UPDATE));
    }

    @Test
    void deletingContactCommentEmitsDeleteAudit() throws Exception {
        final ContactEntity contact = newContact();
        final String body = mockMvc.perform(asUser(
                post("/api/contacts/" + contact.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                delete("/api/contacts/" + contact.getId() + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        final List<AuditLogDto> entries = auditsFor("ContactComment", contact.getId());
        assertTrue(entries.stream().anyMatch(a -> a.action() == AuditAction.DELETE));
    }
}
