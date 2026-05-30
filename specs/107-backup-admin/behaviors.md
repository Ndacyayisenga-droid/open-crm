# Behaviors: Backup Admin Dialog

## Authorization

### IT-ADMIN reaches the backup page

- **Given** a signed-in user whose roles include `IT-ADMIN`
- **When** they navigate to `/admin/backup`
- **Then** the page renders the four sections (Status, Trigger, List, Download) and does not return `403`

### Non-IT-ADMIN is forbidden at the frontend route

- **Given** a signed-in user whose roles do *not* include `IT-ADMIN`
- **When** they navigate to `/admin/backup`
- **Then** the response is the `ForbiddenPage` component and no `BackupPageClient` bundle is loaded
- **And** no request to `/api/admin/backup/*` is issued

### Non-IT-ADMIN is forbidden at every backend endpoint

- **Given** a signed-in user without the `IT-ADMIN` role and a valid auth cookie
- **When** they call any of `GET /api/admin/backup/status`, `POST /api/admin/backup/trigger`, `GET /api/admin/backup/backups`, `GET /api/admin/backup/backups/{id}/download`
- **Then** the backend responds with HTTP `403` and no `DbBackupClient` method is invoked

### Sidebar entry is hidden for non-IT-ADMIN

- **Given** a signed-in user without the `IT-ADMIN` role
- **When** the app layout renders
- **Then** the admin sub-menu (and therefore the new "Backup" entry) is not shown

### Sidebar entry is shown for IT-ADMIN

- **Given** a signed-in user with the `IT-ADMIN` role
- **When** the app layout renders
- **Then** the admin sub-menu contains a "Backup" entry linking to `/admin/backup`
- **And** the entry is marked `active` when the current path starts with `/admin/backup`

## Configuration

### Backend boots when `DB_BACKUP_API_TOKEN` is unset

- **Given** an environment where `DB_BACKUP_API_TOKEN` is not set (resolves to empty string in `application.yml`)
- **When** the Spring Boot application starts
- **Then** the context comes up successfully
- **And** `DbBackupClient` is in the context as a bean
- **And** no `DbBackupException` is thrown at boot

### Backend boots when `DB_BACKUP_BASE_URL` is unset

- **Given** an environment where `DB_BACKUP_BASE_URL` is not set
- **When** the Spring Boot application starts
- **Then** the context comes up successfully with the default base URL `http://localhost:8081`

### Custom env values flow into `MeilisearchProperties`-style binding

- **Given** `DB_BACKUP_BASE_URL=https://backup.example.com` and `DB_BACKUP_API_TOKEN=t-123`
- **When** the `DbBackupProperties` bean is resolved
- **Then** `baseUrl` is `https://backup.example.com` and `apiToken` is `t-123`

## Service status (GET /api/admin/backup/status)

### Not configured

- **Given** an IT-ADMIN session and a backend where `openelements.db-backup.api-token` is blank
- **When** the frontend calls `GET /api/admin/backup/status`
- **Then** the backend returns HTTP `200` with body `{ configured: false, healthy: false, info: null }`
- **And** the controller does *not* invoke `DbBackupClient.isHealthy()` or `getInfo()`
- **And** the Status card renders "Backup-Service ist nicht konfiguriert."

### Configured, healthy, info available (happy path)

- **Given** an IT-ADMIN session, a configured token, and a reachable backup service
- **When** the frontend calls `GET /api/admin/backup/status`
- **Then** the backend invokes `isHealthy()` (returns `true`) and `getInfo()` (returns a `BackupServiceInfo`)
- **And** the response is HTTP `200` with `{ configured: true, healthy: true, info: { ... } }`
- **And** the Status card renders a green dot, the version, `pg_dump` version, retention, interval, and the relative age of the last successful backup

### Configured, healthy, but `getInfo()` fails

- **Given** an IT-ADMIN session, a configured token, `isHealthy()` returning `true`, and `getInfo()` throwing `DbBackupException`
- **When** the frontend calls `GET /api/admin/backup/status`
- **Then** the response is HTTP `200` with `{ configured: true, healthy: true, info: null }`
- **And** the Status card renders a green dot, "Verbunden.", and "Service-Info konnte nicht geladen werden."

### Configured but unreachable

- **Given** an IT-ADMIN session, a configured token, and `isHealthy()` returning `false`
- **When** the frontend calls `GET /api/admin/backup/status`
- **Then** the response is HTTP `200` with `{ configured: true, healthy: false, info: null }`
- **And** `getInfo()` is not invoked
- **And** the Status card renders a red dot and "Backup-Service nicht verfügbar."

### Configured but auth fails upstream (401 collapses to "unavailable")

- **Given** an IT-ADMIN session, a configured *wrong* token, and `isHealthy()` returning `false` because the service rejects the token
- **When** the frontend calls `GET /api/admin/backup/status`
- **Then** the UI shows "Backup-Service nicht verfügbar." (same as a network outage)
- **And** the backend log records the underlying 401 / `DbBackupException` for ops diagnosis

## Trigger backup (POST /api/admin/backup/trigger)

### Trigger when no backup is running

- **Given** an IT-ADMIN session and `client.triggerBackup()` returning `BackupTriggerResult(job, alreadyRunning=false)` with `job.jobId="j1"`
- **When** the frontend POSTs `/api/admin/backup/trigger`
- **Then** the response is HTTP `200` with body `{ jobId: "j1", alreadyRunning: false }`
- **And** the UI renders "Backup ausgelöst — Job-ID j1"

### Trigger when a backup is already running

- **Given** an IT-ADMIN session and `client.triggerBackup()` returning `BackupTriggerResult(job, alreadyRunning=true)` with `job.jobId="j1"`
- **When** the frontend POSTs `/api/admin/backup/trigger`
- **Then** the response is HTTP `200` with body `{ jobId: "j1", alreadyRunning: true }`
- **And** the UI renders "Backup läuft bereits — Job-ID j1"

### Trigger when service is unreachable

- **Given** an IT-ADMIN session and `client.triggerBackup()` throwing `DbBackupException`
- **When** the frontend POSTs `/api/admin/backup/trigger`
- **Then** the response is HTTP `503` with body `{ "error": "Backup-Service nicht verfügbar" }`
- **And** the trigger card displays the generic error message
- **And** the button remains enabled for a retry

### Trigger when token is blank

- **Given** an IT-ADMIN session and a backend where `openelements.db-backup.api-token` is blank
- **When** the frontend POSTs `/api/admin/backup/trigger`
- **Then** the controller does not pre-check; it invokes the client, which throws `DbBackupException`
- **And** the response is HTTP `503` with `{ "error": "Backup-Service nicht verfügbar" }`

## List backups (GET /api/admin/backup/backups)

### Happy path with non-empty list

- **Given** an IT-ADMIN session and `client.listBackups()` returning a list of two `BackupMetadata` records, newest first
- **When** the frontend GETs `/api/admin/backup/backups`
- **Then** the response is HTTP `200` with two `BackupItemDto` entries in the same order
- **And** every field of `BackupMetadata` (`id`, `createdAt`, `sizeBytes`, `sha256`, `pgVersion`, `durationMs`, `triggeredBy`) is mapped one-to-one
- **And** the table renders the rows in the order returned

### Empty list

- **Given** an IT-ADMIN session and `client.listBackups()` returning an empty list
- **When** the frontend GETs `/api/admin/backup/backups`
- **Then** the response is HTTP `200` with body `[]`
- **And** the table area shows "Noch keine Backups vorhanden."

### Service unreachable

- **Given** an IT-ADMIN session and `client.listBackups()` throwing `DbBackupException`
- **When** the frontend GETs `/api/admin/backup/backups`
- **Then** the response is HTTP `503` with body `{ "error": "Backup-Service nicht verfügbar" }`
- **And** the table area shows the generic error message
- **And** the Status and Trigger cards on the same page render independently and are unaffected

### SHA-256 is copyable from the row

- **Given** the backup list contains an entry with `sha256 = "abc...def"`
- **When** the admin clicks the SHA-256 chip
- **Then** the value `abc...def` is placed on the clipboard
- **And** a transient confirmation "Hash kopiert." appears

## Download backup (GET /api/admin/backup/backups/{id}/download)

### Happy path: known backup, filename includes timestamp and id

- **Given** an IT-ADMIN session, `client.listBackups()` returning a backup with `id="abc"` and `createdAt=2026-05-30T08:15:00Z`, and `client.downloadBackup("abc")` returning a `BackupDownload` with `sizeBytes=12345` and a non-empty stream
- **When** the admin's browser GETs `/api/admin/backup/backups/abc/download`
- **Then** the response is HTTP `200` with headers
  - `Content-Type: application/gzip`
  - `Content-Length: 12345`
  - `Content-Disposition: attachment; filename="backup-2026-05-30T08-15-00Z-abc.sql.gz"`
- **And** the response body contains exactly the bytes from the `BackupDownload` stream
- **And** the `BackupDownload` is closed after the response is fully written (try-with-resources)

### Download falls back when metadata cannot be resolved

- **Given** an IT-ADMIN session, `client.listBackups()` throwing `DbBackupException`, and `client.downloadBackup("abc")` returning a valid `BackupDownload`
- **When** the admin's browser GETs `/api/admin/backup/backups/abc/download`
- **Then** the response is HTTP `200` with `Content-Disposition: attachment; filename="backup-abc.sql.gz"`
- **And** the body still streams the backup correctly

### Backup not found

- **Given** an IT-ADMIN session and `client.downloadBackup("missing")` throwing `DbBackupException` (or returning `Optional.empty` if that is the client contract)
- **When** the admin's browser GETs `/api/admin/backup/backups/missing/download`
- **Then** the response is HTTP `503` (or `404` if `Optional.empty`) with body `{ "error": "Backup-Service nicht verfügbar" }`
- **And** no headers other than the standard error response are set

### Stream interrupted mid-transfer

- **Given** an IT-ADMIN session, headers already written (`200 OK`, `Content-Length`, `Content-Disposition`), and the upstream stream throws `IOException` after some bytes were forwarded
- **When** the exception propagates
- **Then** the backend closes the response stream
- **And** the `BackupDownload` is closed via try-with-resources
- **And** the browser sees a truncated file (length less than `Content-Length`)
- **And** the admin can detect the truncation by running `sha256sum` against the value shown in the list

### Service down before first byte

- **Given** an IT-ADMIN session and `client.downloadBackup("abc")` throwing `DbBackupException` *before* the controller starts writing the response
- **When** the admin's browser GETs `/api/admin/backup/backups/abc/download`
- **Then** the response is HTTP `503` with body `{ "error": "Backup-Service nicht verfügbar" }` and no `Content-Disposition` header

### Concurrent downloads

- **Given** two IT-ADMIN sessions, each starting a download of a different backup at the same time
- **When** both `GET /api/admin/backup/backups/<id>/download` requests are in flight
- **Then** each receives its own independent `BackupDownload` from the client
- **And** the two streams do not interfere
- **And** each filename reflects its own backup's `createdAt`/`id`

## Frontend behaviors

### Manual refresh re-fetches every card

- **Given** the page has rendered all four sections successfully
- **When** the admin reloads the browser tab
- **Then** the Status, Trigger result, and List cards refetch their data independently
- **And** the previously shown trigger result (if any) is cleared (one-shot after trigger)

### Partial render: one card fails, others succeed

- **Given** an IT-ADMIN session where `GET /status` returns `200` and `GET /backups` returns `503`
- **When** the page loads
- **Then** the Status card renders normally
- **And** the Trigger card renders normally (button enabled, no error)
- **And** the List card shows the generic "Backup-Service nicht verfügbar." in its area
- **And** the page does not crash or display a top-level error

### Trigger result is transient

- **Given** the admin clicks "Backup auslösen" and the UI shows "Backup ausgelöst — Job-ID j1"
- **When** the admin reloads the page
- **Then** the trigger result message is gone (no persistence across reloads)
- **And** the new Job will appear in the List card on the next reload once the service has it indexed

### "Not configured" hides nothing

- **Given** an IT-ADMIN session and a backend where the token is blank
- **When** the page loads
- **Then** the Status card shows "Backup-Service ist nicht konfiguriert."
- **And** the Trigger, List, and Download UI elements remain visible
- **And** clicking the Trigger button results in the generic "Backup-Service nicht verfügbar." message
- **And** the List card shows the generic "Backup-Service nicht verfügbar." message

## OpenAPI / contract

### Endpoints appear in OpenAPI under the "Backup Admin" tag

- **Given** the backend is running with the new controller
- **When** Swagger UI loads `/api/swagger-ui` (or the project's documented path)
- **Then** a tag "Backup Admin" lists exactly four operations: `GET /status`, `POST /trigger`, `GET /backups`, `GET /backups/{id}/download`
- **And** each operation declares the `oidc` security requirement
- **And** each non-download operation declares HTTP `403` (forbidden) and HTTP `503` (service unavailable) responses in addition to its happy-path response
