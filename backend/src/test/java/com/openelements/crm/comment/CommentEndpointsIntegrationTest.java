package com.openelements.crm.comment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.security.roles.Roles;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.comment.CommentService;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the nested comment endpoints introduced by
 * spec 094. Covers happy paths, validation, mismatched ownership, listing, and
 * the cascade behaviour on owner deletion.
 */
class CommentEndpointsIntegrationTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedSystemUser() {
        if (userRepository.findBySub(SystemUser.SUB).isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO users (id, sub, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SystemUser.ID, SystemUser.SUB, SystemUser.NAME);
        }
    }

    // Cleanup is handled by AbstractDbTest's @AfterEach TRUNCATE.

private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, List<String> roles) {
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

    private CompanyEntity newCompany(String name) {
        final CompanyEntity company = new CompanyEntity();
        company.setName(name);
        return companyRepository.saveAndFlush(company);
    }

    private ContactEntity newContact(String firstName, String lastName) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        return contactRepository.saveAndFlush(contact);
    }

    // -- Comment creation / listing for Company --

    @Test
    void postCompanyCommentCreatesCommentAndJoinRow() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"Hello world\"}"),
                List.of()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.text").value("Hello world"))
            .andExpect(jsonPath("$.author.name").value("Test User"))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        org.junit.jupiter.api.Assertions.assertTrue(
            commentRepository.findById(commentId).isPresent(),
            "Comment row should be inserted");

        mockMvc.perform(asUser(get("/api/companies/" + company.getId()), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commentCount").value(1));
    }

    @Test
    void postCompanyCommentRejectsBlankText() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"   \"}"),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postCommentForUnknownCompanyReturns404() throws Exception {
        mockMvc.perform(asUser(
                post("/api/companies/" + UUID.randomUUID() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hi\"}"),
                List.of()))
            .andExpect(status().isNotFound());
    }

    @Test
    void postCompanyCommentUnauthenticatedReturns401() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        mockMvc.perform(post("/api/companies/" + company.getId() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hi\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCompanyCommentsReturnsFlatArray() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(asUser(
                    post("/api/companies/" + company.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"comment " + i + "\"}"),
                    List.of()))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(asUser(get("/api/companies/" + company.getId() + "/comments"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void getCommentsForUnknownCompanyReturns404() throws Exception {
        mockMvc.perform(asUser(get("/api/companies/" + UUID.randomUUID() + "/comments"), List.of()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCommentsForCompanyDoesNotIncludeCommentsOfOtherCompany() throws Exception {
        final CompanyEntity a = newCompany("A");
        final CompanyEntity b = newCompany("B");
        mockMvc.perform(asUser(
                post("/api/companies/" + a.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"only-A\"}"),
                List.of()))
            .andExpect(status().isCreated());
        mockMvc.perform(asUser(
                post("/api/companies/" + b.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"only-B\"}"),
                List.of()))
            .andExpect(status().isCreated());

        final String body = mockMvc.perform(asUser(get("/api/companies/" + a.getId() + "/comments"), List.of()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        final JsonNode arr = objectMapper.readTree(body);
        org.junit.jupiter.api.Assertions.assertEquals(1, arr.size());
        org.junit.jupiter.api.Assertions.assertEquals("only-A", arr.get(0).get("text").asText());
    }

    // -- Update --

    @Test
    void putCommentOfCompanyUpdatesText() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"original\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                put("/api/companies/" + company.getId() + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"updated\"}"),
                List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("updated"));

        org.junit.jupiter.api.Assertions.assertEquals("updated",
            commentRepository.findById(commentId).orElseThrow().getText());
    }

    @Test
    void putCommentRejectsMismatchedOwner() throws Exception {
        final CompanyEntity a = newCompany("A");
        final CompanyEntity b = newCompany("B");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + a.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"on-a\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                put("/api/companies/" + b.getId() + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hijack\"}"),
                List.of()))
            .andExpect(status().isNotFound());
    }

    @Test
    void putCommentRejectsBlankText() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"x\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                put("/api/companies/" + company.getId() + "/comments/" + commentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"\"}"),
                List.of()))
            .andExpect(status().isBadRequest());
    }

    // -- Delete --

    @Test
    void deleteCommentOfCompanyRequiresAdmin() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"x\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                delete("/api/companies/" + company.getId() + "/comments/" + commentId),
                List.of()))
            .andExpect(status().isForbidden());

        mockMvc.perform(asUser(
                delete("/api/companies/" + company.getId() + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(
            commentRepository.findById(commentId).isEmpty(),
            "Comment row should be removed");
    }

    @Test
    void deleteCommentMismatchedOwnerReturns404() throws Exception {
        final CompanyEntity a = newCompany("A");
        final CompanyEntity b = newCompany("B");
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + a.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"x\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(
                delete("/api/companies/" + b.getId() + "/comments/" + commentId),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertTrue(
            commentRepository.findById(commentId).isPresent(),
            "Comment row should remain");
    }

    // -- Owner-deletion cascade --

    @Test
    void deletingCompanyAlsoDeletesItsComments() throws Exception {
        final CompanyEntity company = newCompany("Acme");
        final List<UUID> commentIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final String body = mockMvc.perform(asUser(
                    post("/api/companies/" + company.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"c" + i + "\"}"),
                    List.of()))
                .andReturn().getResponse().getContentAsString();
            commentIds.add(UUID.fromString(objectMapper.readTree(body).get("id").asText()));
        }

        mockMvc.perform(asUser(delete("/api/companies/" + company.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        for (final UUID id : commentIds) {
            org.junit.jupiter.api.Assertions.assertTrue(commentRepository.findById(id).isEmpty(),
                "Comment row " + id + " should be removed by cascade");
        }
    }

    @Test
    void deletingContactAlsoDeletesItsComments() throws Exception {
        final ContactEntity contact = newContact("Jane", "Doe");
        final String body = mockMvc.perform(asUser(
                post("/api/contacts/" + contact.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\"}"),
                List.of()))
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(asUser(delete("/api/contacts/" + contact.getId()), List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(commentRepository.findById(commentId).isEmpty(),
            "Comment row should be removed by cascade");
    }

    // -- Standalone /api/comments/{id} endpoints removed --

    @Test
    void standaloneCommentPutEndpointIsRemoved() throws Exception {
        mockMvc.perform(asUser(
                put("/api/comments/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"x\"}"),
                List.of()))
            .andExpect(status().isNotFound());
    }

    @Test
    void standaloneCommentDeleteEndpointIsRemoved() throws Exception {
        mockMvc.perform(asUser(
                delete("/api/comments/" + UUID.randomUUID()),
                List.of(Roles.ROLE_APP_ADMIN)))
            .andExpect(status().isNotFound());
    }

    // -- SYSTEM-USER author --

    @Test
    void systemUserAuthorIsResolvedInResponse() throws Exception {
        final CompanyEntity company = newCompany("Acme");

        // Create a comment via the API (authenticated test user), then rewrite the
        // entity's authorId directly to SYSTEM-USER to simulate a legacy / migrated row.
        final String body = mockMvc.perform(asUser(
                post("/api/companies/" + company.getId() + "/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"legacy\"}"),
                List.of()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        final UUID commentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        transactionTemplate.executeWithoutResult(status -> {
            final var entity = commentRepository.findByIdOrThrow(commentId);
            entity.setAuthor(userRepository.getReferenceById(SystemUser.ID));
            commentRepository.saveAndFlush(entity);
        });

        mockMvc.perform(asUser(get("/api/companies/" + company.getId() + "/comments"), List.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].author.name").value(SystemUser.NAME));
    }

    @Test
    void systemUserIsHiddenFromAdminUserList() throws Exception {
        mockMvc.perform(asUser(get("/api/users"), List.of(Roles.ROLE_IT_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == '" + SystemUser.ID + "')]").doesNotExist());
    }
}
