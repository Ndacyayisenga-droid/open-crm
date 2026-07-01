package com.openelements.spring.base.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.data.image.ImageType;
import com.openelements.spring.base.mcp.McpProperties.Auth;
import com.openelements.spring.base.mcp.McpProperties.Auth.ApiKey;
import com.openelements.spring.base.mcp.McpProperties.Auth.Oidc;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link McpToolSupport}: tool-call dispatch with structured access
 * logging, JSON serialization, JSON-RPC error mapping, and in-memory pagination
 * (spec 108). The {@link McpSyncServerExchange} is mocked; no Spring context or
 * database is required.
 */
class McpToolSupportTest {

    private static final McpSchema.Tool TOOL =
        McpTools.tool("demo", "Demo tool.", Map.of(), List.of());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpPaging paging = new McpPaging(new McpProperties(
        true, "Open CRM", "0.1.0", 50, 20, new Auth(new ApiKey(true), new Oidc(false))));
    private final McpToolSupport support = new McpToolSupport(paging, objectMapper);

    private CallToolResult call(final McpToolLogic logic, final Map<String, Object> args) {
        final McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(
            McpTransportContext.create(Map.of(McpActorLabel.ACTOR_LABEL_KEY, "apikey:test")));
        final SyncToolSpecification spec = support.spec(TOOL, logic);
        return spec.call().apply(exchange, args);
    }

    private static String text(final CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }

    private CallToolResult imageCall(final McpImageLogic logic, final Map<String, Object> args) {
        final McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(
            McpTransportContext.create(Map.of(McpActorLabel.ACTOR_LABEL_KEY, "apikey:test")));
        final SyncToolSpecification spec = support.imageSpec(TOOL, logic);
        return spec.call().apply(exchange, args);
    }

    // -- spec(): success -----------------------------------------------------

    @Test
    void specCarriesTheGivenTool() {
        final SyncToolSpecification spec = support.spec(TOOL, args -> "ok");
        assertEquals("demo", spec.tool().name());
    }

    @Test
    void successResultIsSerializedJsonAndNotAnError() throws Exception {
        final Map<String, Object> payload = Map.of("a", 1, "b", "two");

        final CallToolResult result = call(args -> payload, Map.of());

        assertFalse(result.isError(), "successful call must not be an error result");
        assertEquals(objectMapper.writeValueAsString(payload), text(result));
    }

    @Test
    void nullArgumentsArePassedToLogicAsEmptyMap() {
        final CallToolResult result = call(args -> {
            assertNotNull(args, "logic must never receive null args");
            return args.size();
        }, null);

        assertFalse(result.isError());
        assertEquals("0", text(result));
    }

    @Test
    void missingActorLabelDoesNotBreakDispatch() {
        final McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(McpTransportContext.EMPTY);

        final CallToolResult result = support.spec(TOOL, args -> "ok").call().apply(exchange, Map.of());

        assertFalse(result.isError());
        assertEquals("\"ok\"", text(result));
    }

    // -- spec(): error mapping ----------------------------------------------

    @Test
    void illegalArgumentBecomesInvalidArgumentError() {
        final CallToolResult result = call(args -> {
            throw new IllegalArgumentException("id is required");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Invalid argument: id is required", text(result));
    }

    @Test
    void noSuchElementBecomesNotFoundErrorWithMessage() {
        final CallToolResult result = call(args -> {
            throw new NoSuchElementException("Company not found: 42");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Company not found: 42", text(result));
    }

    @Test
    void unavailableExceptionBecomesUnavailableErrorWithMessage() {
        final CallToolResult result = call(args -> {
            throw new McpUnavailableException("Search index is initializing; retry shortly.");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Search index is initializing; retry shortly.", text(result));
    }

    @Test
    void unexpectedExceptionBecomesGenericInternalErrorWithoutLeakingDetails() {
        final CallToolResult result = call(args -> {
            throw new IllegalStateException("connection pool exhausted at jdbc://secret");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Internal error executing tool demo", text(result),
            "internal errors must not leak the underlying message");
    }

    // -- imageSpec(): success ------------------------------------------------

    @Test
    void imageSpecCarriesTheGivenTool() {
        final SyncToolSpecification spec = support.imageSpec(TOOL,
            args -> new ImageData(new byte[] {1, 2, 3}, ImageType.JPEG));
        assertEquals("demo", spec.tool().name());
    }

    @Test
    void imageSuccessReturnsImageContentWithBase64DataAndMimeType() {
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final ImageData image = new ImageData(bytes, ImageType.PNG);

        final CallToolResult result = imageCall(args -> image, Map.of("id", "x"));

        assertFalse(result.isError(), "successful image call must not be an error result");
        assertEquals(1, result.content().size(), "image result carries a single content item");
        final ImageContent content = (ImageContent) result.content().get(0);
        assertEquals(Base64.getEncoder().encodeToString(bytes), content.data());
        assertEquals("image/png", content.mimeType());
    }

    @Test
    void imageMimeTypeMirrorsTheStoredContentType() {
        final CallToolResult result =
            imageCall(args -> new ImageData(new byte[] {9}, ImageType.JPEG), Map.of());

        final ImageContent content = (ImageContent) result.content().get(0);
        assertEquals("image/jpeg", content.mimeType());
    }

    @Test
    void imageLogicReceivesNonNullArgsEvenWhenNull() {
        final CallToolResult result = imageCall(args -> {
            assertNotNull(args, "image logic must never receive null args");
            return new ImageData(new byte[] {1}, ImageType.JPEG);
        }, null);

        assertFalse(result.isError());
    }

    // -- imageSpec(): error mapping -----------------------------------------

    @Test
    void imageIllegalArgumentBecomesInvalidArgumentError() {
        final CallToolResult result = imageCall(args -> {
            throw new IllegalArgumentException("id is required");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Invalid argument: id is required", text(result));
    }

    @Test
    void imageNoSuchElementBecomesNotFoundErrorWithMessage() {
        final CallToolResult result = imageCall(args -> {
            throw new NoSuchElementException("Contact has no photo: 42");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Contact has no photo: 42", text(result));
    }

    @Test
    void imageUnavailableExceptionBecomesUnavailableErrorWithMessage() {
        final CallToolResult result = imageCall(args -> {
            throw new McpUnavailableException("temporarily unavailable");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("temporarily unavailable", text(result));
    }

    @Test
    void imageUnexpectedExceptionBecomesGenericInternalErrorWithoutLeakingDetails() {
        final CallToolResult result = imageCall(args -> {
            throw new IllegalStateException("connection pool exhausted at jdbc://secret");
        }, Map.of());

        assertTrue(result.isError());
        assertEquals("Internal error executing tool demo", text(result),
            "internal errors must not leak the underlying message");
    }

    // -- paginate() ----------------------------------------------------------

    @Test
    void paginateFirstPageSignalsMore() {
        final McpPage<Integer> page = support.paginate(List.of(0, 1, 2, 3, 4), 0, 2);

        assertEquals(List.of(0, 1), page.items());
        assertEquals(0, page.page());
        assertEquals(2, page.size());
        assertEquals(5, page.totalCount());
        assertTrue(page.hasMore());
    }

    @Test
    void paginateLastPageSignalsNoMore() {
        final McpPage<Integer> page = support.paginate(List.of(0, 1, 2, 3, 4), 2, 2);

        assertEquals(List.of(4), page.items());
        assertFalse(page.hasMore());
        assertEquals(5, page.totalCount());
    }

    @Test
    void paginateBeyondRangeReturnsEmptyPage() {
        final McpPage<Integer> page = support.paginate(List.of(0, 1, 2, 3, 4), 10, 2);

        assertTrue(page.items().isEmpty());
        assertFalse(page.hasMore());
        assertEquals(5, page.totalCount());
    }

    @Test
    void paginateAppliesDefaultSizeWhenSizeOmitted() {
        final McpPage<Integer> page = support.paginate(List.of(0, 1, 2, 3, 4), null, null);

        assertEquals(5, page.items().size(), "default size (20) covers all 5 items");
        assertEquals(20, page.size());
        assertFalse(page.hasMore());
    }
}
