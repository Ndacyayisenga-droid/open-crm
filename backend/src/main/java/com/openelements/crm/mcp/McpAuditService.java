package com.openelements.crm.mcp;

import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogEntity;
import com.openelements.spring.base.services.audit.AuditLogRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Writes one {@code audit_log} entry per MCP tool call (spec 108).
 *
 * <p>A read over MCP is logged as an {@link AuditAction#INSERT} access event with
 * {@code entityType = "MCP"}. The actor (resolved via {@link McpActorResolver})
 * supplies the {@code user_id} FK and a label embedded in the entry name, e.g.
 * {@code "list_contacts [apikey:onyx-prod]"}.
 *
 * <p>INFO logging records only the tool name and actor label — never tool
 * arguments (search queries / ids can be sensitive).
 */
@Service
public class McpAuditService {

    /** {@code audit_log.entity_type} value for all MCP calls. */
    static final String ENTITY_TYPE = "MCP";

    /** Sentinel {@code entity_id} for calls not tied to a single entity (the NOT-NULL nil UUID). */
    static final UUID NO_ENTITY = new UUID(0L, 0L);

    private static final Logger log = LoggerFactory.getLogger(McpAuditService.class);

    private final McpActorResolver actorResolver;
    private final AuditLogRepository auditLogRepository;

    public McpAuditService(final McpActorResolver actorResolver,
                           final AuditLogRepository auditLogRepository) {
        this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    /**
     * Records a successful tool call.
     *
     * @param toolName the MCP tool name (e.g. {@code "list_contacts"})
     * @param entityId the targeted entity id for {@code get_*} tools, or {@code null} for collections/search
     */
    public void recordSuccess(final String toolName, final UUID entityId) {
        final McpActor actor = actorResolver.resolve();
        save(actor, entityId == null ? NO_ENTITY : entityId,
            toolName + " [" + actor.label() + "]", toolName);
    }

    /**
     * Records a failed tool call.
     *
     * @param toolName     the MCP tool name
     * @param errorSummary a short, non-sensitive failure summary (e.g. an exception class or "invalid argument")
     */
    public void recordFailure(final String toolName, final String errorSummary) {
        final McpActor actor = actorResolver.resolve();
        save(actor, NO_ENTITY,
            toolName + ": " + errorSummary + " [" + actor.label() + "]", toolName);
    }

    private void save(final McpActor actor, final UUID entityId, final String name, final String toolName) {
        final AuditLogEntity entry = new AuditLogEntity();
        entry.setEntityType(ENTITY_TYPE);
        entry.setEntityId(entityId);
        entry.setName(name);
        entry.setAction(AuditAction.INSERT);
        entry.setUser(actor.auditUser());
        auditLogRepository.save(entry);
        log.info("MCP tool call tool={} actor={}", toolName, actor.label());
    }
}
