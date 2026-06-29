FROM alpine:3.21

RUN apk add --no-cache \
    postgresql17-client \
    aws-cli \
    bash \
    tzdata

COPY db-backup.sh /usr/local/bin/db-backup
COPY db-restore.sh /usr/local/bin/db-restore
RUN chmod +x /usr/local/bin/db-backup /usr/local/bin/db-restore

# Default: run backup on schedule via crond
CMD echo "${BACKUP_CRON:-0 2 * * *} /usr/local/bin/db-backup >> /var/log/backup.log 2>&1" > /etc/crontabs/root \
    && echo "[$(date)] Backup scheduler started. Schedule: ${BACKUP_CRON:-0 2 * * *}" \
    && crond -f -l 2
