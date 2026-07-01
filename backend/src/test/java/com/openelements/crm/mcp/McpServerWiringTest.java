package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.AbstractDbTest;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the MCP server beans wire up when the feature is enabled (spec 108,
 * step 6). The full JSON-RPC protocol behavior is covered in step 7 with a real
 * MCP client.
 */
@TestPropertySource(properties = "openelements.mcp.enabled=true")
class McpServerWiringTest extends AbstractDbTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private McpToolFactory toolFactory;

    @Test
    void mcpBeansAreWiredWhenEnabled() {
        assertTrue(context.getBeanNamesForType(McpSyncServer.class).length > 0,
            "McpSyncServer must be registered when enabled");
        assertTrue(context.containsBean("mcpRouterFunction"),
            "the /mcp router function must be registered");
    }

    @Test
    void toolCatalogContainsExactlyThePhase1Tools() {
        final List<SyncToolSpecification> tools = toolFactory.toolSpecifications();
        final Set<String> names = tools.stream()
            .map(t -> t.tool().name())
            .collect(Collectors.toSet());

        assertEquals(Set.of(
            "search", "list_companies", "get_company", "list_contacts", "get_contact",
            "list_tags", "get_tag", "list_company_comments", "list_contact_comments",
            "get_contact_photo", "get_company_logo"
        ), names);
    }
}
