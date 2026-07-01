package com.openelements.spring.base.mcp;

import com.openelements.spring.base.data.image.ImageData;
import java.util.Map;

/**
 * The executable body of an image-returning MCP tool (spec 109): validates and
 * parses its arguments, calls a backing service, and returns the
 * {@link ImageData} to deliver as an MCP {@code ImageContent} result.
 *
 * <p>This is the image counterpart to {@link McpToolLogic}. Thrown exceptions are
 * mapped to JSON-RPC error results by {@link McpToolSupport#imageSpec} using the
 * same rules: {@link IllegalArgumentException} becomes an invalid-argument error,
 * {@link java.util.NoSuchElementException} a not-found error,
 * {@link McpUnavailableException} a temporary-unavailable error, and any other
 * exception a generic internal error.
 */
@FunctionalInterface
public interface McpImageLogic {

    /**
     * Runs the tool.
     *
     * @param args the tool arguments (never {@code null}; empty when none were sent)
     * @return the image to deliver as the tool result (never {@code null})
     * @throws Exception to signal an error, mapped to a JSON-RPC error result
     */
    ImageData run(Map<String, Object> args) throws Exception;
}
