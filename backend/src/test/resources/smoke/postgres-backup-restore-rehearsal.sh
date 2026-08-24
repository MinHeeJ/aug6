#!/usr/bin/env bash
set -euo pipefail

# Daily PostgreSQL backup/restore rehearsal smoke script.
# Run from a host or CI container with pg_dump/pg_restore and database env vars available.
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
RESTORE_REHEARSAL_DAYS="${RESTORE_REHEARSAL_DAYS:-7}"
BACKUP_DIR="${BACKUP_DIR:-./backups/postgres}"
DATABASE_URL="${DATABASE_URL:?DATABASE_URL is required}"

mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive="$BACKUP_DIR/common-foundation-$stamp.dump"

pg_dump --format=custom --no-owner --file="$archive" "$DATABASE_URL"
find "$BACKUP_DIR" -name 'common-foundation-*.dump' -type f -mtime +"$BACKUP_RETENTION_DAYS" -delete

# Restore rehearsal validates recent backups without mutating the source database.
if [ "${RESTORE_DATABASE_URL:-}" != "" ]; then
  pg_restore --clean --if-exists --no-owner --dbname="$RESTORE_DATABASE_URL" "$archive"
fi

echo "backup=$archive retention_days=$BACKUP_RETENTION_DAYS restore_rehearsal_days=$RESTORE_REHEARSAL_DAYS"
