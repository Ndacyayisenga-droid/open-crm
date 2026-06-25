package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.openelements.crm.AbstractDbTest;
import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Verifies the MCP master switch (spec 108, step 7): with the default
 * {@code openelements.mcp.enabled=false}, no MCP server or endpoint bean is
 * registered, so the feature is fully off.
 */
class McpDisabledTest extends AbstractDbTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void noMcpBeansWhenDisabled() {
        assertEquals(0, context.getBeanNamesForType(McpSyncServer.class).length,
            "no McpSyncServer when disabled");
        assertFalse(context.containsBean("mcpRouterFunction"),
            "no /mcp router function when disabled");
    }
}
