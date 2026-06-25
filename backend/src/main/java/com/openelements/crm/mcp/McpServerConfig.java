package com.openelements.crm.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Wires the MCP server endpoint (spec 108, step 6).
 *
 * <p>Only active when {@code openelements.mcp.enabled=true}. Builds the official
 * Java MCP SDK's {@link WebMvcStreamableServerTransportProvider} (Streamable HTTP
 * at {@code /mcp}), assembles the {@link McpSyncServer} from the Phase-1 tool
 * catalog, and exposes the transport's {@link RouterFunction} so Spring WebMVC
 * serves the endpoint.
 */
@Configuration
@ConditionalOnProperty(prefix = "openelements.mcp", name = "enabled", havingValue = "true")
public class McpServerConfig {

    /** The MCP endpoint path; kept in sync with {@link McpSecurityConfig#MCP_PATHS}. */
    static final String MCP_ENDPOINT = "/mcp";

    /**
     * The Streamable HTTP transport provider. Uses the application's Jackson
     * {@link ObjectMapper} (so date/time and other modules match the REST API)
     * wrapped as the SDK's {@code McpJsonMapper}.
     */
    @Bean
    public WebMvcStreamableServerTransportProvider mcpTransportProvider(final ObjectMapper objectMapper) {
        return WebMvcStreamableServerTransportProvider.builder()
            .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
            .mcpEndpoint(MCP_ENDPOINT)
            .build();
    }

    /**
     * The sync MCP server. Building it registers the session factory on the
     * transport provider, so this bean must be initialized before the endpoint
     * serves requests (the router-function bean depends on it).
     */
    @Bean
    public McpSyncServer mcpSyncServer(final WebMvcStreamableServerTransportProvider transportProvider,
                                       final McpToolFactory toolFactory,
                                       final McpProperties properties) {
        return McpServer.sync(transportProvider)
            .serverInfo(properties.serverName(), properties.serverVersion())
            .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
            .tools(toolFactory.toolSpecifications())
            .build();
    }

    /**
     * Exposes the transport's router function to Spring WebMVC. Depends on
     * {@link #mcpSyncServer} so the transport's session factory is wired before
     * any request is routed.
     *
     * @param transportProvider the transport provider
     * @param mcpSyncServer     the built server (ordering dependency; not used directly)
     * @return the router function serving {@code /mcp}
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(
        final WebMvcStreamableServerTransportProvider transportProvider,
        final McpSyncServer mcpSyncServer) {
        return transportProvider.getRouterFunction();
    }
}
