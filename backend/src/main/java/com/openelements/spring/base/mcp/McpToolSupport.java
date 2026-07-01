package com.openelements.spring.base.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.spring.base.data.image.ImageData;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generic runtime support for MCP tool implementations (spec 108).
 *
 * <p>{@link #spec} wraps a tool's {@link McpToolLogic} into a
 * {@link SyncToolSpecification} that resolves the calling actor, runs the logic,
 * serializes the payload to JSON, logs one structured access line
 * ({@code tool=… actor=…}, never the arguments), and maps failures to JSON-RPC
 * error results. {@link #imageSpec} is the image-returning counterpart (spec 109):
 * it wraps an {@link McpImageLogic} the same way but builds an MCP
 * {@code ImageContent} result (Base64 bytes + MIME type) instead of a JSON text
 * result, sharing the actor resolution, access logging, and error mapping.
 * {@link #paginate} slices a fully materialized list into the
 * {@link McpPage} envelope using the configured paging defaults and clamps.
 *
 * <p>This class carries no domain knowledge; {@link McpToolProvider}
 * implementations build their schemas and parse arguments with {@link McpTools}.
 *
 * <p>MCP reads are <em>not</em> written to an audit log: read access (like
 * viewing a record in a frontend) is recorded only as a structured INFO log line.
 */
@Component
public class McpToolSupport {

    private static final Logger log = LoggerFactory.getLogger(McpToolSupport.class);

    private final McpPaging paging;
    private final ObjectMapper objectMapper;

    public McpToolSupport(final McpPaging paging, final ObjectMapper objectMapper) {
        this.paging = Objects.requireNonNull(paging, "paging must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Wraps tool logic into a sync tool specification with access logging and
     * JSON-RPC error mapping.
     *
     * @param tool  the tool definition (see {@link McpTools#tool})
     * @param logic the tool body
     * @return the sync tool specification
     */
    public SyncToolSpecification spec(final McpSchema.Tool tool, final McpToolLogic logic) {
        return new SyncToolSpecification(tool, (exchange, args) ->
            invoke(tool.name(), McpActorLabel.from(exchange.transportContext()),
                () -> new McpSchema.CallToolResult(json(logic.run(nonNull(args))), false)));
    }

    /**
     * Wraps image-returning tool logic into a sync tool specification, sharing the
     * access logging and JSON-RPC error mapping of {@link #spec} but building an
     * MCP {@code ImageContent} result (Base64 bytes + MIME type) on success.
     *
     * @param tool  the tool definition (see {@link McpTools#tool})
     * @param logic the tool body, returning the image to deliver
     * @return the sync tool specification
     */
    public SyncToolSpecification imageSpec(final McpSchema.Tool tool, final McpImageLogic logic) {
        return new SyncToolSpecification(tool, (exchange, args) ->
            invoke(tool.name(), McpActorLabel.from(exchange.transportContext()),
                () -> imageResult(logic.run(nonNull(args)))));
    }

    private McpSchema.CallToolResult invoke(final String name, final String actor,
                                            final Callable<McpSchema.CallToolResult> producer) {
        try {
            final McpSchema.CallToolResult result = producer.call();
            log.info("MCP tool call tool={} actor={}", name, actor);
            return result;
        } catch (final IllegalArgumentException e) {
            log.info("MCP tool call rejected tool={} actor={} reason=invalid-argument", name, actor);
            return new McpSchema.CallToolResult("Invalid argument: " + e.getMessage(), true);
        } catch (final NoSuchElementException e) {
            log.info("MCP tool call not-found tool={} actor={}", name, actor);
            return new McpSchema.CallToolResult(e.getMessage(), true);
        } catch (final McpUnavailableException e) {
            log.info("MCP tool call unavailable tool={} actor={}", name, actor);
            return new McpSchema.CallToolResult(e.getMessage(), true);
        } catch (final Exception e) {
            log.warn("MCP tool call errored tool={} actor={} error={}", name, actor, e.getClass().getSimpleName());
            return new McpSchema.CallToolResult("Internal error executing tool " + name, true);
        }
    }

    private static Map<String, Object> nonNull(final Map<String, Object> rawArgs) {
        return rawArgs == null ? Map.of() : rawArgs;
    }

    private McpSchema.CallToolResult imageResult(final ImageData image) {
        final String base64 = Base64.getEncoder().encodeToString(image.data());
        final McpSchema.ImageContent content =
            new McpSchema.ImageContent(null, base64, image.contentType());
        return new McpSchema.CallToolResult(List.of(content), false);
    }

    private String json(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MCP tool payload", e);
        }
    }

    /**
     * Slices a fully materialized list into the {@link McpPage} envelope using the
     * configured paging defaults and clamps.
     *
     * @param all     the full result list
     * @param pageReq the requested zero-based page, or {@code null}
     * @param sizeReq the requested page size, or {@code null}
     * @param <T>     the item type
     * @return the page envelope
     */
    public <T> McpPage<T> paginate(final List<T> all, final Integer pageReq, final Integer sizeReq) {
        final int size = paging.resolveSize(sizeReq);
        final int page = paging.resolvePage(pageReq);
        final int from = Math.min(page * size, all.size());
        final int to = Math.min(from + size, all.size());
        return new McpPage<>(new ArrayList<>(all.subList(from, to)), page, size, all.size(), to < all.size());
    }
}
