#!/bin/sh
set -e

# Database backup script — creates a compressed pg_dump and uploads to S3
# Can be run manually or via cron inside the backup container

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="/tmp/opencrm_${TIMESTAMP}.sql.gz"

echo "[$(date)] Starting database backup..."

# Create compressed dump
PGPASSWORD="${DB_PASSWORD}" pg_dump \
  -h "${DB_HOST:-db}" \
  -p "${DB_INTERNAL_PORT:-5432}" \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  --no-owner \
  --no-privileges \
  | gzip > "${BACKUP_FILE}"

FILESIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date)] Backup created: ${BACKUP_FILE} (${FILESIZE})"

# Upload to S3
if [ -n "${S3_BUCKET}" ]; then
  S3_PATH="s3://${S3_BUCKET}/${S3_PREFIX:-backups}/opencrm_${TIMESTAMP}.sql.gz"
  echo "[$(date)] Uploading to ${S3_PATH}..."

  S3_ENDPOINT_ARG=""
  if [ -n "${S3_ENDPOINT}" ]; then
    S3_ENDPOINT_ARG="--endpoint-url ${S3_ENDPOINT}"
  fi

  aws s3 cp "${BACKUP_FILE}" "${S3_PATH}" \
    ${S3_ENDPOINT_ARG} \
    --no-progress

  echo "[$(date)] Upload complete."

  # Cleanup old backups (keep last N days)
  RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
  echo "[$(date)] Cleaning up backups older than ${RETENTION_DAYS} days..."

  CUTOFF_DATE=$(date -d "-${RETENTION_DAYS} days" +%Y%m%d 2>/dev/null || date -v-${RETENTION_DAYS}d +%Y%m%d)

  aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX:-backups}/" \
    ${S3_ENDPOINT_ARG} \
    | while read -r line; do
      FILE=$(echo "$line" | awk '{print $4}')
      FILE_DATE=$(echo "$FILE" | grep -o '[0-9]\{8\}' | head -1)
      if [ -n "$FILE_DATE" ] && [ "$FILE_DATE" -lt "$CUTOFF_DATE" ] 2>/dev/null; then
        echo "[$(date)] Deleting old backup: ${FILE}"
        aws s3 rm "s3://${S3_BUCKET}/${S3_PREFIX:-backups}/${FILE}" \
          ${S3_ENDPOINT_ARG}
      fi
    done

  echo "[$(date)] Cleanup complete."
else
  echo "[$(date)] WARNING: S3_BUCKET not set, backup saved locally only."
fi

# Remove local temp file
rm -f "${BACKUP_FILE}"
echo "[$(date)] Backup finished successfully."
