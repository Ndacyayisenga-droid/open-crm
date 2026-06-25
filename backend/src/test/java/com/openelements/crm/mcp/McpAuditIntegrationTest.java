package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openelements.crm.AbstractDbTest;
import com.openelements.spring.base.services.apikey.ApiKeyEntity;
import com.openelements.spring.base.services.user.SystemUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Integration tests for the MCP actor + audit abstraction (spec 108, step 3).
 *
 * <p>Drives {@link McpActorResolver}/{@link McpAuditService} with an API-key
 * principal placed in the security context, and asserts the resolved actor and
 * the persisted {@code audit_log} row.
 */
class McpAuditIntegrationTest extends AbstractDbTest {

    @Autowired
    private McpActorResolver actorResolver;

    @Autowired
    private McpAuditService auditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        seedSystemUser();
        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setName("onyx-test");
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(apiKey, null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesApiKeyActorToSystemUserWithKeyLabel() {
        final McpActor actor = actorResolver.resolve();

        assertEquals("apikey:onyx-test", actor.label());
        assertEquals(SystemUser.ID, actor.auditUser().getId(),
            "API-key calls are audited under the SYSTEM user");
    }

    @Test
    void recordSuccessWritesOneMcpAuditRow() {
        auditService.recordSuccess("list_contacts", null, actorResolver.resolve());

        assertEquals(1, mcpAuditCount());
        assertEquals("list_contacts [apikey:onyx-test]", lastMcpAuditName());
        assertEquals(SystemUser.ID,
            jdbcTemplate.queryForObject(
                "SELECT user_id FROM audit_log WHERE entity_type = 'MCP'", java.util.UUID.class));
    }

    @Test
    void recordFailureEncodesToolAndError() {
        auditService.recordFailure("get_contact", "invalid argument", actorResolver.resolve());

        assertEquals(1, mcpAuditCount());
        assertEquals("get_contact: invalid argument [apikey:onyx-test]", lastMcpAuditName());
    }

    private Integer mcpAuditCount() {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM audit_log WHERE entity_type = 'MCP'", Integer.class);
    }

    private String lastMcpAuditName() {
        return jdbcTemplate.queryForObject(
            "SELECT entity_name FROM audit_log WHERE entity_type = 'MCP' ORDER BY created_at DESC LIMIT 1",
            String.class);
    }
}
