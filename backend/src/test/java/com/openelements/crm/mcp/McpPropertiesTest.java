package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

/**
 * Binding tests for {@link McpProperties}. Uses the Spring Boot {@link Binder}
 * directly (no application context, no database) so the test is fast and
 * deterministic.
 */
class McpPropertiesTest {

    private static McpProperties bind(final Map<String, Object> properties) {
        final MockEnvironment environment = new MockEnvironment();
        properties.forEach(environment::setProperty);
        return new Binder(ConfigurationPropertySources.get(environment))
            .bindOrCreate("openelements.mcp", McpProperties.class);
    }

    @Test
    void defaultsAreSafeWhenNothingIsConfigured() {
        final McpProperties props = bind(Map.of());

        assertFalse(props.enabled(), "MCP must be off by default");
        assertEquals("Open CRM", props.serverName());
        assertEquals("0.1.0", props.serverVersion());
        assertEquals(20, props.defaultPageSize());
        assertEquals(50, props.maxPageSize());
        assertTrue(props.auth().apiKey().enabled(), "API-key profile defaults on");
        assertFalse(props.auth().oidc().enabled(), "OIDC profile defaults off");
    }

    @Test
    void valuesBindFromConfiguration() {
        final McpProperties props = bind(Map.of(
            "openelements.mcp.enabled", "true",
            "openelements.mcp.server-name", "CRM Test",
            "openelements.mcp.default-page-size", "10",
            "openelements.mcp.max-page-size", "30",
            "openelements.mcp.auth.api-key.enabled", "false",
            "openelements.mcp.auth.oidc.enabled", "true"
        ));

        assertTrue(props.enabled());
        assertEquals("CRM Test", props.serverName());
        assertEquals(10, props.defaultPageSize());
        assertEquals(30, props.maxPageSize());
        assertFalse(props.auth().apiKey().enabled());
        assertTrue(props.auth().oidc().enabled());
    }
}
