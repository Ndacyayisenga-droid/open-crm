package com.openelements.crm.updates;

import com.openelements.spring.base.services.user.UserDto;
import java.time.Instant;
import java.util.UUID;

/**
 * One row in the Updates feed.
 *
 * @param id          audit-log entry id (or id of the latest entry in a merged run of UPDATEs)
 * @param type        update type derived from the audit entityType + action
 * @param entityId    company or contact id; {@code null} for {@code COMPANY_DELETED} / {@code CONTACT_DELETED}
 * @param entityName  current entity name; {@code null} for {@code COMPANY_DELETED} / {@code CONTACT_DELETED} and
 *                    when the referenced entity can no longer be resolved
 * @param user        actor that performed the action
 * @param createdAt   timestamp of the (latest) underlying audit entry
 */
public record UpdateEntryDto(
    UUID id,
    UpdateType type,
    UUID entityId,
    String entityName,
    UserDto user,
    Instant createdAt
) {
}
