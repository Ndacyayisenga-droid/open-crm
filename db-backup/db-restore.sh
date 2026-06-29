#!/bin/sh
set -e

# Database restore script — downloads a backup from S3 and restores it
#
# Usage:
#   ./db-restore.sh                          # lists available backups
#   ./db-restore.sh opencrm_20260401.sql.gz  # restores a specific backup

if [ -z "${S3_BUCKET}" ] || [ -z "${S3_ENDPOINT}" ]; then
  echo "ERROR: S3_BUCKET and S3_ENDPOINT must be set."
  exit 1
fi

S3_BASE="s3://${S3_BUCKET}/${S3_PREFIX:-backups}"

# If no argument, list available backups
if [ -z "$1" ]; then
  echo "Available backups in ${S3_BASE}:"
  echo ""
  aws s3 ls "${S3_BASE}/" \
    --endpoint-url "${S3_ENDPOINT}" \
    | sort -r \
    | head -20
  echo ""
  echo "Usage: $0 <filename>"
  echo "Example: $0 opencrm_20260401_020000.sql.gz"
  exit 0
fi

BACKUP_NAME="$1"
RESTORE_FILE="/tmp/${BACKUP_NAME}"

echo "[$(date)] Downloading ${S3_BASE}/${BACKUP_NAME}..."
aws s3 cp "${S3_BASE}/${BACKUP_NAME}" "${RESTORE_FILE}" \
  --endpoint-url "${S3_ENDPOINT}" \
  --no-progress

echo "[$(date)] Restoring database ${DB_NAME}..."
echo ""
echo "WARNING: This will DROP and recreate the database ${DB_NAME}!"
echo "Press Ctrl+C within 5 seconds to abort..."
sleep 5

# Drop and recreate database
PGPASSWORD="${DB_PASSWORD}" psql \
  -h "${DB_HOST:-db}" \
  -p "${DB_INTERNAL_PORT:-5432}" \
  -U "${DB_USER}" \
  -d postgres \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" \
  -c "DROP DATABASE IF EXISTS \"${DB_NAME}\";" \
  -c "CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USER}\";"

# Restore from dump
gunzip -c "${RESTORE_FILE}" | PGPASSWORD="${DB_PASSWORD}" psql \
  -h "${DB_HOST:-db}" \
  -p "${DB_INTERNAL_PORT:-5432}" \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  --quiet

rm -f "${RESTORE_FILE}"
echo "[$(date)] Restore complete. Restart the backend to apply Flyway migrations if needed."
