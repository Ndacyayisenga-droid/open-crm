# Implementation Steps: spring-services 0.16.0 Upgrade

> API verified against the actual `spring-services-0.16.0.jar`. Reconciliations vs. design.md:
> - `ImageData` accessor is `data()`, **not** `bytes()`.
> - `ImageData.of(...)` throws `IllegalArgumentException` ("File too large (max 20 MB)") and `ImageType.fromContentType` throws `IllegalArgumentException` for bad types — **not** `ResponseStatusException(400)`. So the explicit size + content-type guards stay in `ContactService.uploadPhoto` (behaviors require 400 at the service boundary); only the transcoding delegates to `ImageData.asJpeg()`.
> - **Design omission:** `recordCommentAudit` in `ContactService` + `CompanyService` build `AuditLogEntity` directly and must now set `name` (NOT NULL).
> - Only `UpdatesControllerTest`, `UpdatesServiceTest`, `AuditLogControllerTest` call `createEntry` (4-arg → 5-arg). `CommentAuditEmissionTest` uses REST, no signature change.

## Step 1: V33 migration

- [ ] `V33__add_audit_log_entity_name.sql`: add nullable `entity_name`, backfill `'UNKNOWN'`, set NOT NULL

## Step 2: Bump spring-services 0.15 → 0.16

- [ ] `backend/pom.xml` version bump
- [ ] Set `name` in both `recordCommentAudit` (owner entity name, fallback `"UNKNOWN"`)
- [ ] Update 4-arg `createEntry` callers in the three test files to 5-arg

## Step 3: Multipart 20 MB + Swagger/message text

- [ ] `application.yml`: `max-file-size`/`max-request-size` → `20MB`
- [ ] `ContactService.uploadPhoto` size-guard message → "Photo exceeds 20 MB"
- [ ] `ContactController` Swagger param → "max 20 MB"

## Step 4: Image-format cleanup

- [ ] Delete `ContactPhotoTranscoder.java`, CRM `HeicSupportCheck.java`, `ContactPhotoTranscoderTest.java`
- [ ] Add `CrmHeicSupportCheck` wrapping lib `HeicSupportCheck.verifyHeicSupport()`
- [ ] `ContactService.uploadPhoto`: delegate PNG/WebP/HEIC transcode to `ImageData.of(data,type).asJpeg().data()`; keep explicit JPEG-passthrough, HEIC 415 short-circuit, and default 400
- [ ] Update `ContactPhotoHeicWebpIntegrationTest`: `HeicSupportCheck` → `CrmHeicSupportCheck`; oversize tests → 21 MB + "20 MB"; reconcile malformed-webp message with library

## Step 5: @NameSupplier

- [ ] `CompanyDto.displayName()` `@NameSupplier` → name
- [ ] `ContactDto.displayName()` `@NameSupplier` → `(firstName + " " + lastName).trim()`

## Step 6: Tests for new behavior

- [ ] Service-level test: `uploadPhoto` with 21 MB array → 400 "20 MB"
- [ ] Audit name test: company/contact event → `entity_name` = display name; assert `/api/audit-logs` exposes `name`
- [ ] `AuditLogControllerTest`: assert response includes non-null `name`

## Step 7: Build + reviews + follow-up issue + PR

- [ ] `./mvnw clean verify` green
- [ ] File follow-up issue for the nextjs-app-layer audit-log "Name" column
- [ ] spec-review + quality-review clean; push; PR closing #33
