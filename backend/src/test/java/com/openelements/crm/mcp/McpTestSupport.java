package com.openelements.crm.mcp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Shared test helpers for the MCP integration tests.
 */
final class McpTestSupport {

    private McpTestSupport() {
    }

    /**
     * Computes the lowercase SHA-256 hex of the given value — the same hashing
     * the spring-services API-key store uses, so a directly inserted
     * {@code api_keys.key_hash} matches {@code ApiKeyDataService.authenticate}.
     *
     * @param value the raw value
     * @return the SHA-256 hex digest
     */
    static String sha256Hex(final String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
