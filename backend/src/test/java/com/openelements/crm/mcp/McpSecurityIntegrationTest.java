package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.spring.base.services.apikey.ApiKeyDataService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@code /mcp} security chain (spec 108, step 2).
 *
 * <p>Runs with {@code openelements.mcp.enabled=true} so the
 * {@link McpSecurityConfig} chain is active. The MCP request handler itself does
 * not exist yet (step 6), so an authenticated call falls through to a 404 — the
 * tests therefore assert on the security outcome (401 / not-401), not on a tool
 * response.
 */
@TestPropertySource(properties = "openelements.mcp.enabled=true")
class McpSecurityIntegrationTest extends AbstractDbTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String JSON_RPC_BODY =
        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApiKeyDataService apiKeyDataService;

    private String rawKey;

    @BeforeEach
    void seedApiKey() {
        // Insert a key row directly so the test does not depend on an IT-ADMIN
        // security context. The hash must match what ApiKeyDataService computes;
        // the assertion below verifies that via the real authenticate() path.
        rawKey = "crm_" + "mcptestkey".repeat(4) + "abcdefgh";
        final String keyHash = McpTestSupport.sha256Hex(rawKey);
        final java.sql.Timestamp now = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update(
            "INSERT INTO api_keys (id, name, key_hash, key_prefix, created_by, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), "onyx-test", keyHash, rawKey.substring(0, 11), "test", now, now);
        assertTrue(apiKeyDataService.authenticate(rawKey).isPresent(),
            "seeded key hash must match ApiKeyDataService hashing");
    }

    @Test
    void postWithoutApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(JSON_RPC_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithInvalidApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/mcp")
                .header(API_KEY_HEADER, "crm_not-a-real-key")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_RPC_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithValidApiKeyPassesAuthentication() throws Exception {
        // Auth + CSRF must pass; the handler is added in step 6, so a 404 (not a
        // 401/403) proves the security chain let the request through.
        final int statusCode = mockMvc.perform(post("/mcp")
                .header(API_KEY_HEADER, rawKey)
                .contentType(MediaType.APPLICATION_JSON).content(JSON_RPC_BODY))
            .andReturn().getResponse().getStatus();
        assertNotEquals(401, statusCode, "valid key must not be rejected as unauthorized");
        assertNotEquals(403, statusCode, "CSRF must be disabled on the /mcp chain");
    }
}
