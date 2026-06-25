package com.openelements.crm.mcp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Always-on configuration for the MCP feature that registers {@link McpProperties}.
 *
 * <p>Binding the properties unconditionally lets the {@code @ConditionalOnProperty}
 * MCP beans (security chain, server wiring) read the master switch and the
 * per-profile toggles. The actual MCP endpoint and tool beans live in the
 * conditional configurations and are only created when {@code openelements.mcp.enabled=true}.
 */
@Configuration
@EnableConfigurationProperties(McpProperties.class)
public class McpConfiguration {
}
