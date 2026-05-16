package com.openelements.crm.auditlog;

import com.openelements.crm.security.RequiresItAdmin;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import com.openelements.spring.base.services.audit.AuditLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Log")
@SecurityRequirement(name = "oidc")
@RequiresItAdmin
public class AuditLogController {

    private final AuditLogDataService auditLogService;

    public AuditLogController(final AuditLogDataService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List audit log entries",
        description = "Returns a paginated list of audit log entries with optional filtering by entity type and user. "
            + "Sorted by createdAt descending (newest first). Requires the IT-ADMIN role."
    )
    public Page<AuditLogDto> listAuditLogs(
        @Parameter(description = "Filter by entity type (exact match)")
        @RequestParam(required = false) final String entityType,
        @Parameter(description = "Filter by user id (UUID, exact match)")
        @RequestParam(required = false) final UUID user,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) final Pageable pageable) {
        final Page<AuditLogDto> data;
        final boolean hasEntityType = entityType != null && !entityType.isBlank();
        final boolean hasUser = user != null;
        if (hasEntityType && hasUser) {
            data = auditLogService.findByEntityTypeAndUser(entityType, user, pageable);
        } else if (hasEntityType) {
            data = auditLogService.findByEntityType(entityType, pageable);
        } else if (hasUser) {
            data = auditLogService.findByUser(user, pageable);
        } else {
            data = auditLogService.findAll(pageable);
        }
        return data;
    }

    @GetMapping(path = "/entity-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List distinct entity types",
        description = "Returns the distinct entity-type strings present in the audit log, sorted alphabetically. "
            + "Used to populate the entity-type filter dropdown. Requires the IT-ADMIN role."
    )
    public List<String> listEntityTypes() {
        return auditLogService.findAllEntityTypes();
    }
}
