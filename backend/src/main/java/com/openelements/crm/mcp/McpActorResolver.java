package com.openelements.crm.mcp;

import com.openelements.spring.base.services.apikey.ApiKeyEntity;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import io.modelcontextprotocol.common.McpTransportContext;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link McpActor} for the current MCP request from the security context.
 *
 * <p>Phase 1 handles the API-key profile: when the authenticated principal is an
 * {@link ApiKeyEntity}, the actor's audit user is the shared SYSTEM user and the
 * label is {@code "apikey:<key-name>"}. Phase 2 will add a branch that maps a
 * JWT principal to the real user without changing callers.
 */
@Component
public class McpActorResolver {

    /** Key under which the actor label is stored in the {@link McpTransportContext}. */
    public static final String ACTOR_LABEL_KEY = "mcp.actor.label";

    private static final String UNKNOWN_LABEL = "unknown";

    private final UserRepository userRepository;

    public McpActorResolver(final UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    /**
     * Resolves the actor from the MCP transport context (the canonical path for
     * tool calls). The actor label is captured on the request thread by the
     * transport's context extractor (see {@code McpServerConfig}) because the
     * tool handler may run on a thread without the security context.
     *
     * @param context the transport context for the current tool call (may be {@code null})
     * @return the resolved {@link McpActor}
     * @throws IllegalStateException if the SYSTEM user is not provisioned (it backs the audit FK)
     */
    public McpActor resolve(final McpTransportContext context) {
        final Object label = context == null ? null : context.get(ACTOR_LABEL_KEY);
        return new McpActor(systemUser(), label != null ? label.toString() : UNKNOWN_LABEL);
    }

    /**
     * Resolves the actor from the current thread's security context. Used as a
     * fallback and in unit tests where the principal is set directly.
     *
     * @return the resolved {@link McpActor}
     * @throws IllegalStateException if the SYSTEM user is not provisioned
     */
    public McpActor resolve() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final String label;
        if (authentication != null && authentication.getPrincipal() instanceof ApiKeyEntity apiKey) {
            label = "apikey:" + apiKey.getName();
        } else {
            label = UNKNOWN_LABEL;
        }
        return new McpActor(systemUser(), label);
    }

    private UserEntity systemUser() {
        return userRepository.findBySub(SystemUser.SUB)
            .orElseThrow(() -> new IllegalStateException(
                "SYSTEM user (" + SystemUser.SUB + ") is not provisioned; required for MCP audit attribution"));
    }
}
