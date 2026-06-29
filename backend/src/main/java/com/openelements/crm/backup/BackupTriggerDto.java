package com.openelements.crm.backup;

/**
 * Response of {@code POST /api/admin/backup/trigger}.
 *
 * @param jobId          the sidecar job identifier — either a fresh job or the already-running one
 * @param alreadyRunning {@code true} when the sidecar answered {@code 409 Conflict} and the
 *                       {@code jobId} refers to a backup that was already in flight
 */
public record BackupTriggerDto(String jobId, boolean alreadyRunning) {
}
