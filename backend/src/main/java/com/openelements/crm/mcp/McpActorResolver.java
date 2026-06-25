package com.openelements.crm.mcp;

import com.openelements.spring.base.services.apikey.ApiKeyEntity;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
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

    private final UserRepository userRepository;

    public McpActorResolver(final UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    /**
     * Resolves the actor for the current security context.
     *
     * @return the resolved {@link McpActor}
     * @throws IllegalStateException if the SYSTEM user is not provisioned (it backs the audit FK)
     */
    public McpActor resolve() {
        final UserEntity systemUser = userRepository.findBySub(SystemUser.SUB)
            .orElseThrow(() -> new IllegalStateException(
                "SYSTEM user (" + SystemUser.SUB + ") is not provisioned; required for MCP audit attribution"));

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final String label;
        if (authentication != null && authentication.getPrincipal() instanceof ApiKeyEntity apiKey) {
            label = "apikey:" + apiKey.getName();
        } else {
            // Phase 1 only authenticates via API key on /mcp; any other principal
            // is unexpected here. Phase 2 adds the JWT branch.
            label = "unknown";
        }
        return new McpActor(systemUser, label);
    }
}
