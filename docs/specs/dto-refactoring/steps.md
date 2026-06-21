# Implementation Steps: DTO Refactoring

## Step 1: Rename Company DTOs and add requiredMode

- [x] Rename `CompanyCreateRequest` → `CompanyCreateDto`, add `requiredMode = REQUIRED` on `name`
- [x] Rename `CompanyUpdateRequest` → `CompanyUpdateDto`, add `requiredMode = REQUIRED` on `name`
- [x] Rename `CompanyResponse` → `CompanyDto`, add `requiredMode = REQUIRED` on `id`, `name`, `deleted`, `createdAt`, `updatedAt`
- [x] Update `CompanyService` references
- [x] Update `CompanyController` references

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] No references to old class names remain in the company package

**Related behaviors:** Project compiles after rename

---

## Step 2: Rename Contact DTOs and add requiredMode

- [x] Rename `ContactCreateRequest` → `ContactCreateDto`, add `requiredMode = REQUIRED` on `firstName`, `lastName`, `language`
- [x] Rename `ContactUpdateRequest` → `ContactUpdateDto`, add `requiredMode = REQUIRED` on `firstName`, `lastName`, `language`
- [x] Rename `ContactResponse` → `ContactDto`, add `requiredMode = REQUIRED` on `id`, `firstName`, `lastName`, `syncedToBrevo`, `doubleOptIn`, `language`, `createdAt`, `updatedAt`
- [x] Update `ContactService` references
- [x] Update `ContactController` references

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds

**Related behaviors:** Project compiles after rename

---

## Step 3: Rename Comment and Health DTOs and add requiredMode

- [x] Rename `CommentCreateRequest` → `CommentCreateDto`, add `requiredMode = REQUIRED` on `text`, `author`
- [x] Rename `CommentUpdateRequest` → `CommentUpdateDto`, add `requiredMode = REQUIRED` on `text`, `author`
- [x] Rename `CommentResponse` → `CommentDto`, add `requiredMode = REQUIRED` on `id`, `text`, `author`, `createdAt`, `updatedAt`
- [x] Rename `HealthResponse` → `HealthDto`, add `requiredMode = REQUIRED` on `status`
- [x] Update `CommentService`, `CommentController`, `HealthController` references

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds

**Related behaviors:** Project compiles after rename

---

## Step 4: Update tests and verify

- [x] Update `CompanyControllerTest` references to new DTO names (no direct references — tests use JSON/MockMvc)
- [x] Update `ContactControllerTest` references (no direct references)
- [x] Update `CommentControllerTest` references (no direct references)
- [x] Update `HealthControllerTest` references (no direct references)
- [x] Add 7 OpenAPI schema tests verifying `required` properties for all DTOs
- [x] `./mvnw clean verify` — all 70 tests pass

**Acceptance criteria:**
- [x] All 70 tests pass
- [x] OpenAPI spec at `/v3/api-docs` shows correct required fields for all schemas
- [x] No references to old DTO names anywhere in the codebase

**Related behaviors:** All existing tests pass after rename, Required properties are marked in company create schema, Required properties are marked in contact create schema, Required properties are marked in comment create schema, Required properties are marked in company response schema, Required properties are marked in contact response schema, Required properties are marked in comment response schema, Health DTO schema is correct, JSON field names are unchanged

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Project compiles after rename | Backend | Steps 1-3 |
| All existing tests pass after rename | Backend | Step 4 |
| Required properties marked in company create schema | Backend | Step 4 |
| Required properties marked in contact create schema | Backend | Step 4 |
| Required properties marked in comment create schema | Backend | Step 4 |
| Required properties marked in company response schema | Backend | Step 4 |
| Required properties marked in contact response schema | Backend | Step 4 |
| Required properties marked in comment response schema | Backend | Step 4 |
| Health DTO schema is correct | Backend | Step 4 |
| JSON field names are unchanged | Backend | Step 4 |
