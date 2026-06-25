package com.openelements.crm.mcp;

import com.openelements.spring.base.services.user.UserEntity;

/**
 * The resolved actor behind an MCP tool call.
 *
 * <p>This is the seam between the two auth profiles (spec 108): it decouples the
 * tool/audit layer from how the caller authenticated.
 *
 * <ul>
 *   <li><b>Phase 1 (API key):</b> there is no per-user identity, so
 *       {@code auditUser} is the shared SYSTEM user and {@code label} carries the
 *       API-key name (e.g. {@code "apikey:onyx-prod"}). Per-human accountability
 *       lives in the MCP client's own logs.</li>
 *   <li><b>Phase 2 (OIDC):</b> {@code auditUser} will be the real, JWT-resolved
 *       user and {@code label} the user's identity — added without changing the
 *       tool or audit code.</li>
 * </ul>
 *
 * @param auditUser the user written to the {@code audit_log.user_id} FK (never {@code null})
 * @param label     a human-readable actor label embedded in the audit entry name
 */
public record McpActor(UserEntity auditUser, String label) {
}
