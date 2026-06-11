package com.openelements.crm.backup;

import com.openelements.spring.base.services.dbbackup.BackupServiceInfo;

/**
 * Combined health + info response for {@code GET /api/admin/backup/status}.
 *
 * @param configured {@code false} when {@code openelements.db-backup.api-token} is blank
 * @param healthy    result of {@link com.openelements.spring.base.services.dbbackup.DbBackupClient#isHealthy()};
 *                   always {@code false} when {@code configured == false}
 * @param info       service metadata; {@code null} when not configured, unhealthy, or the info
 *                   call failed while the health probe succeeded
 */
public record BackupStatusDto(boolean configured, boolean healthy, BackupServiceInfo info) {
}
