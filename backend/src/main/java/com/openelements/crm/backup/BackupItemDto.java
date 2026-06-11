package com.openelements.crm.backup;

import java.time.Instant;

/**
 * Single entry of the {@code GET /api/admin/backup/backups} response. One-to-one mapping of
 * {@link com.openelements.spring.base.services.dbbackup.BackupMetadata}, with the trigger enum
 * flattened to its string name so the frontend doesn't need to know the upstream enum values.
 */
public record BackupItemDto(
    String id,
    Instant createdAt,
    long sizeBytes,
    String sha256,
    String pgVersion,
    long durationMs,
    String triggeredBy
) {
}
