# Behaviors: spring-services 0.16.0 Upgrade

This upgrade has a small intentional behavior surface (new audit `name` field, 20 MB upload ceiling, refactored transcoding) on top of a structural cleanup that must preserve Spec 101/102 contracts. Scenarios below cover the visible behavior, the regression contract for image uploads, and the schema migration.

## Schema migration

### V33 runs against an existing audit_log table

- **Given** a database with existing rows in `audit_log` and no `entity_name` column
- **When** Flyway applies migration `V33__add_audit_log_entity_name.sql`
- **Then** the column `entity_name VARCHAR(255) NOT NULL` exists
- **And** every pre-existing row has `entity_name = 'UNKNOWN'`
- **And** the migration runs in well under a second for the CRM's expected v1 volume (low thousands of rows)

### V33 is idempotent across restarts

- **Given** V33 has already been applied
- **When** the application restarts
- **Then** Flyway records the migration as already applied and does not run it again
- **And** application startup proceeds without schema-validation errors

### Hibernate validates after V33

- **Given** V33 has been applied and the application starts with `spring.jpa.hibernate.ddl-auto=validate`
- **When** the JPA EntityManagerFactory initializes
- **Then** validation of the `AuditLogEntity` mapping against the `audit_log` table succeeds — the `entity_name` column matches the entity's `nullable = false` declaration

## Audit log — new `name` field

### `name` is populated from `@NameSupplier` for CompanyDto

- **Given** the application is running on spring-services 0.16
- **And** a `Company` named `"Acme GmbH"` exists
- **When** an `OnObjectUpdate` event is published for that company's DTO
- **Then** a new `audit_log` row is created with `entity_name = "Acme GmbH"`
- **And** `/api/audit-logs` (paged) includes that row with `"name": "Acme GmbH"` in the JSON response

### `name` is populated from `@NameSupplier` for ContactDto

- **Given** a `Contact` with `firstName = "Jane"`, `lastName = "Doe"`
- **When** an `OnObjectCreate` event is published for that contact's DTO
- **Then** the resulting `audit_log` row has `entity_name = "Jane Doe"`
- **And** the value does *not* include `title` even if the contact has one

### `name` for events from DTOs without `@NameSupplier` falls back to `"UNKNOWN"`

- **Given** a `TagDto` or `CommentDto` event is published
- **When** the audit-log listener handles the event
- **Then** the resulting `audit_log` row has `entity_name = "UNKNOWN"`
- **And** no error or warning is logged

### `name` for direct `createEntry` callers must be supplied explicitly

- **Given** test code calling `auditLogDataService.createEntry(...)` directly
- **When** the call is made with the new 5-arg signature
- **Then** the row is persisted with the supplied `name`
- **And** passing `null` for `name` raises `NullPointerException` synchronously (per library guard)

### `/api/audit-logs` JSON includes `name` between `entityId` and `action`

- **Given** at least one row exists in `audit_log`
- **When** an IT-Admin calls `GET /api/audit-logs`
- **Then** each entry in the response includes a `name` field
- **And** the field is non-null for all rows

### Historical rows still serialize after the migration

- **Given** rows that existed before V33 (now backfilled to `entity_name = "UNKNOWN"`)
- **When** they are returned via `/api/audit-logs`
- **Then** their `name` field is the literal string `"UNKNOWN"`

## Image upload — 20 MB ceiling

### Multipart accepts a 20 MB image

- **Given** Spring multipart limits are set to 20 MB in `application.yml`
- **When** a client uploads a JPEG of exactly 20 MB to `POST /api/contacts/{id}/photo`
- **Then** the upload is accepted by the multipart layer
- **And** `ImageData.MAX_IMAGE_SIZE` validation passes

### Multipart rejects above 20 MB

- **Given** the same configuration
- **When** a client uploads a 21 MB file
- **Then** Spring rejects the request before it reaches `ContactService`
- **And** the response is `413 Payload Too Large`

### `ImageData` rejects above 20 MB at the JVM boundary

- **Given** `ContactService.uploadPhoto` is called directly with a 21 MB byte array
- **When** the call reaches `ImageData.of(bytes, contentType)`
- **Then** a `ResponseStatusException` with status `400 BAD_REQUEST` is raised
- **And** the message contains `"20 MB"` (not `"2 MB"`)

### Swagger description reflects the new limit

- **Given** the OpenAPI spec is generated
- **When** the `POST /api/contacts/{id}/photo` operation is inspected
- **Then** the `file` parameter description mentions `"max 20 MB"`

## Image upload — format support

### JPEG upload bypasses re-encoding

- **Given** a 1 MB JPEG file
- **When** `ContactService.uploadPhoto` is invoked with `contentType = "image/jpeg"`
- **Then** `ImageData.asJpeg()` is *not* called (the JPEG branch stores the original bytes unchanged)
- **And** the stored bytes equal the input bytes

### PNG upload is transcoded via the library

- **Given** a PNG file with an alpha channel
- **When** `ContactService.uploadPhoto` is invoked with `contentType = "image/png"`
- **Then** the library's `ImageData.asJpeg()` is invoked
- **And** the stored content type is `"image/jpeg"`
- **And** the stored bytes are a valid JPEG with no alpha channel (flattened onto white per library contract)

### WebP upload is transcoded via the library

- **Given** a WebP file
- **When** `ContactService.uploadPhoto` is invoked with `contentType = "image/webp"`
- **Then** the library's `ImageData.asJpeg()` is invoked
- **And** the stored bytes are a valid JPEG
- **And** the EXIF orientation of the source is applied (image appears upright)

### HEIC upload is transcoded when libheif is available

- **Given** `CrmHeicSupportCheck.isHeicAvailable()` returns `true`
- **And** a HEIC file is uploaded with `contentType = "image/heic"`
- **When** `ContactService.uploadPhoto` is invoked
- **Then** the library's `ImageData.asJpeg()` decodes the HEIC
- **And** the stored content type is `"image/jpeg"`

### HEIC upload returns 415 when libheif is unavailable (Spec 102 regression)

- **Given** `CrmHeicSupportCheck.isHeicAvailable()` returns `false`
- **When** a client uploads any file with `contentType = "image/heic"` or `"image/heif"`
- **Then** the response is `415 UNSUPPORTED_MEDIA_TYPE`
- **And** the reason contains `"HEIC support is not available"`
- **And** the library's `asJpeg()` is *not* called (the check short-circuits before it)

### Rejected content types

- **Given** any non-supported content type (e.g., `"image/gif"`, `"application/pdf"`)
- **When** `ContactService.uploadPhoto` is invoked with that content type
- **Then** a `ResponseStatusException` with status `400 BAD_REQUEST` is raised
- **And** the message indicates allowed formats

### Null or empty content type

- **Given** a client omits the `Content-Type` header on the file part
- **When** the upload reaches `ContactService`
- **Then** the behavior matches the rejected-content-types scenario (400, not NPE)

## `CrmHeicSupportCheck` bean

### Probes at startup exactly once

- **Given** the Spring context is initializing
- **When** `CrmHeicSupportCheck.verify()` (`@PostConstruct`) runs
- **Then** the library's `HeicSupportCheck.verifyHeicSupport()` is called exactly once
- **And** the result is cached in a volatile field
- **And** subsequent reads via `isHeicAvailable()` do not re-probe

### Logs a warning when HEIC support is unavailable

- **Given** the probe returns `false` at startup
- **When** the application context comes up
- **Then** a `WARN`-level log entry is emitted explaining that HEIC uploads will be rejected with 415
- **And** the entry mentions `libheif1` and `libheif-plugin-libde265` as the missing prerequisites

## Cleanup of local image-format code

### `ContactPhotoTranscoder` is gone

- **Given** the source tree after the merge
- **When** `find backend/src/main -name "ContactPhotoTranscoder*"` is run
- **Then** no files are returned

### The standalone CRM `HeicSupportCheck` is gone

- **Given** the source tree after the merge
- **When** `find backend/src/main/java/com/openelements/crm/contact -name "HeicSupportCheck.java"` is run
- **Then** no file is returned (replaced by `CrmHeicSupportCheck`)

### Library `HeicSupportCheck` is used by the wrapper

- **Given** `CrmHeicSupportCheck.java`
- **When** the file is inspected
- **Then** it imports `com.openelements.spring.base.data.image.util.HeicSupportCheck` and calls its `verifyHeicSupport()` static helper

### HEIC/WebP Maven dependencies stay

- **Given** `backend/pom.xml`
- **When** dependencies are listed
- **Then** `com.github.gotson.nightmonkeys:imageio-heif` and `com.twelvemonkeys.imageio:imageio-webp` are still declared with their existing versions

### Dockerfile native-library install stays

- **Given** `backend/Dockerfile`
- **When** the file is inspected
- **Then** the `apt-get install -y libheif1 libheif-plugin-libde265` step is present
- **And** the JVM flag `--enable-native-access=ALL-UNNAMED` is still passed

### Existing Spec 101/102 integration tests pass

- **Given** `ContactPhotoHeicWebpIntegrationTest` and any PNG-specific upload tests from Spec 101
- **When** the tests are run after the upgrade
- **Then** they all pass
- **And** the only source-level changes inside those tests are: size-message strings (`"2 MB"` → `"20 MB"`) and any import adjustments for the moved transcoding type

### `ContactPhotoTranscoderTest` is deleted

- **Given** the test tree after the merge
- **When** `find backend/src/test -name "ContactPhotoTranscoderTest*"` is run
- **Then** no files are returned

## Test code that constructs audit-log values directly

### Direct `createEntry` callers pass an explicit name

- **Given** `UpdatesServiceTest.java` and `CommentAuditEmissionTest.java`
- **When** the test file is inspected
- **Then** every `auditLogDataService.createEntry(...)` call uses the 5-arg form
- **And** each call passes a stable, non-null, non-blank string for the new `name` argument

### Tests asserting `AuditLogDto` content know about `name`

- **Given** a test that fetches an `AuditLogDto` via the service or controller
- **When** the test asserts on the DTO's fields
- **Then** assertions referencing the record's positional order are updated for the new component position
- **And** new assertions optionally cover the `name` field where the test sets up a known value

## Application configuration

### Multipart limits in `application.yml` are 20 MB

- **Given** `backend/src/main/resources/application.yml`
- **When** the file is inspected
- **Then** `spring.servlet.multipart.max-file-size` is `20MB`
- **And** `spring.servlet.multipart.max-request-size` is `20MB`

### No environment-variable contract changes

- **Given** `docker-compose.yml` and any deployment manifests
- **When** they are compared before vs after this spec
- **Then** no environment-variable name or value is changed by this work
