# Implementation Steps: Read-Only API Key Authentication

## Step 1: Database migration and entity

- [x] Create `V22__add_api_keys.sql` with `api_keys` table (id UUID PK, name VARCHAR(255) NOT NULL, key_hash VARCHAR(64) NOT NULL UNIQUE, key_prefix VARCHAR(20) NOT NULL, created_by VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL)
- [x] Create `ApiKeyEntity.java` in `com.openelements.crm.apikey` with JPA annotations, UUID PK, `@CreationTimestamp`
- [x] Create `ApiKeyRepository.java` with `findByKeyHash(String)` method

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Migration runs without errors

**Related behaviors:** Key hash is stored not raw key, Different raw keys produce different hashes

---

## Step 2: DTOs and service

- [x] Create `ApiKeyDto.java` record (id, name, keyPrefix, createdBy, createdAt) with `fromEntity()`
- [x] Create `ApiKeyCreateDto.java` record with `@NotBlank @Size(max=255) name`
- [x] Create `ApiKeyCreatedDto.java` record (id, name, keyPrefix, key, createdBy, createdAt) — includes raw key
- [x] Create `ApiKeyService.java` with:
  - `create(ApiKeyCreateDto)` — generates raw key (`crm_` + 48 SecureRandom alphanumeric), computes SHA-256 hash, extracts prefix, resolves creator name via `UserService`, persists entity, returns `ApiKeyCreatedDto` with raw key
  - `list(Pageable)` — returns `Page<ApiKeyDto>`
  - `delete(UUID)` — throws 404 if not found
  - `authenticate(String rawKey)` — hashes key, looks up in DB, returns `Optional<ApiKeyEntity>`

**Acceptance criteria:**
- [x] Project builds successfully

**Related behaviors:** Create API key with valid name, Raw key is unique per creation, Key hash is stored not raw key, Different raw keys produce different hashes

---

## Step 3: Controller

- [x] Create `ApiKeyController.java` at `/api/api-keys` with:
  - `POST /api/api-keys` (201 Created) — returns `ApiKeyCreatedDto`
  - `GET /api/api-keys` (200 OK, paginated) — returns `Page<ApiKeyDto>`
  - `DELETE /api/api-keys/{id}` (204 No Content, 404)
- [x] OpenAPI annotations on all endpoints with `@SecurityRequirement(name = "oidc")`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Endpoints visible in Swagger UI

**Related behaviors:** Create API key with valid name, Create API key without name, Create API key with null name, Create API key with name exceeding max length, List API keys with pagination, List API keys returns no raw key, List API keys when none exist, Delete existing API key, Delete non-existent API key

---

## Step 4: API key authentication filter

- [x] Create `ApiKeyAuthenticationFilter.java` extending `OncePerRequestFilter`:
  - Check for `X-API-Key` header; if absent, pass through to next filter
  - If present: hash the key value with SHA-256, call `ApiKeyService.authenticate()`
  - If not found: return 401 Unauthorized
  - If found but method is not GET/HEAD/OPTIONS: return 403 Forbidden with "API keys only grant read-only access"
  - If found and method is GET/HEAD/OPTIONS: set `Authentication` in SecurityContext, continue chain
- [x] Modify `SecurityConfig.java`: register `ApiKeyAuthenticationFilter` before `BearerTokenAuthenticationFilter`
- [x] Modify `OpenApiConfig.java`: add `apiKey` security scheme (type: APIKEY, in: HEADER, name: X-API-Key)

**Acceptance criteria:**
- [x] Project builds successfully
- [x] All existing tests still pass

**Related behaviors:** Valid API key on GET endpoint, Valid API key on GET with path parameter, Valid API key on GET with query parameters, Valid API key on health endpoint, Valid API key on POST endpoint (403), Valid API key on PUT endpoint (403), Valid API key on DELETE endpoint (403), Invalid API key (401), Empty API key header (401), No API key header falls through to JWT, Both API key and JWT header present, Create API key without authentication (401), Delete API key via API key authentication (403), Deleted key stops working immediately, List API keys via API key authentication

---

## Step 5: Frontend types, API client, i18n, sidebar

- [x] Add `ApiKeyDto`, `ApiKeyCreateDto`, `ApiKeyCreatedDto` to `src/lib/types.ts`
- [x] Add `getApiKeys()`, `createApiKey()`, `deleteApiKey()` to `src/lib/api.ts`
- [x] Add `apiKeys` namespace to `de.ts` and `en.ts` (title, columns, create dialog, key dialog, delete dialog, actions, pagination, empty state, warning)
- [x] Add `nav.apiKeys` to both translation files
- [x] Add API Keys nav item to `sidebar.tsx` with `KeyRound` icon in bottomItems

**Acceptance criteria:**
- [x] Frontend builds successfully (`pnpm build`)

**Related behaviors:** Sidebar navigation

---

## Step 6: Frontend API key list page

- [x] Create `src/app/(app)/api-keys/page.tsx` rendering `<ApiKeyList />`
- [x] Create `src/components/api-key-list.tsx` with:
  - Table: Name, Key (prefix), Created By, Created, Actions (delete)
  - "New API Key" button → create dialog (name input)
  - On create success → "Key Created" dialog showing raw key with copy button + warning
  - Delete button → DeleteConfirmDialog
  - Pagination with page size selector
  - Empty state with icon and create CTA
  - Loading skeletons

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Page renders at `/api-keys`

**Related behaviors:** Display API keys in table, Empty state, Create dialog opens and submits, Key created dialog with copy, Key created dialog warning, Key created dialog close refreshes list, Create dialog validation, Delete with confirmation, Cancel delete

---

## Step 7: Backend tests — service and controller

- [x] Create `ApiKeyServiceTest.java` (`@SpringBootTest`, `@ActiveProfiles("test")`):
  - Create with valid name returns DTO with key starting `crm_`, 52 chars, correct prefix
  - Create without name/blank name throws 400
  - Create with name >255 chars throws 400
  - Two creates with same name produce different keys
  - List with pagination
  - List returns no raw key field
  - List when empty
  - Delete existing key
  - Delete non-existent key throws 404
  - Authenticate with valid key returns entity
  - Authenticate with invalid key returns empty
- [x] Create `ApiKeyControllerTest.java` (`@SpringBootTest`, `@AutoConfigureMockMvc`):
  - POST with valid name returns 201 with all fields
  - POST with blank/null name returns 400
  - POST without auth returns 401
  - GET returns paginated list without raw key
  - GET default sort is createdAt desc
  - DELETE returns 204
  - DELETE non-existent returns 404

**Acceptance criteria:**
- [x] All backend tests pass (`mvn test`)

**Related behaviors:** Create API key with valid name, Create API key without name, Create API key with null name, Create API key with name exceeding max length, Create API key without authentication, Raw key is unique per creation, List API keys with pagination, List API keys returns no raw key, List API keys when none exist, List API keys default sort order, Delete existing API key, Delete non-existent API key, Key hash is stored not raw key, Different raw keys produce different hashes

---

## Step 8: Backend tests — authentication filter

- [x] Create `ApiKeyAuthFilterTest.java` (`@SpringBootTest`, `@AutoConfigureMockMvc`):
  - Valid API key on GET /api/companies → 200
  - Valid API key on GET /api/companies/{id} → 200
  - Valid API key on GET /api/contacts?page=0&size=5 → 200
  - Valid API key on POST /api/companies → 403
  - Valid API key on PUT /api/companies/{id} → 403
  - Valid API key on DELETE /api/companies/{id} → 403
  - Invalid API key → 401
  - Empty X-API-Key header → 401
  - No X-API-Key header, no JWT → 401 (standard JWT failure)
  - Both X-API-Key and JWT present → API key takes precedence (200)
  - Valid API key on GET /api/api-keys → 200
  - Valid API key on DELETE /api/api-keys/{id} → 403
  - Deleted key → 401

**Acceptance criteria:**
- [x] All backend tests pass

**Related behaviors:** Valid API key on GET endpoint, Valid API key on GET with path parameter, Valid API key on GET with query parameters, Valid API key on POST endpoint, Valid API key on PUT endpoint, Valid API key on DELETE endpoint, Invalid API key, Empty API key header, No API key header falls through to JWT, Both API key and JWT header present, List API keys via API key authentication, Delete API key via API key authentication, Deleted key stops working immediately

---

## Step 9: Frontend tests

- [x] Create `src/components/__tests__/api-key-list.test.tsx`:
  - Table renders with correct columns
  - Key column shows prefix
  - Empty state shows create CTA
  - Create dialog opens on button click
  - Create submits name and shows key dialog
  - Key dialog shows raw key with copy button and warning
  - Key dialog close triggers list refresh
  - Create validation on empty name
  - Delete opens confirmation dialog
  - Delete confirms and removes from table
  - Cancel delete keeps key

**Acceptance criteria:**
- [x] All frontend tests pass (`pnpm test`)

**Related behaviors:** Display API keys in table, Empty state, Sidebar navigation, Create dialog opens and submits, Key created dialog with copy, Key created dialog warning, Key created dialog close refreshes list, Create dialog validation, Delete with confirmation, Cancel delete

---

## Step 10: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — add API key auth feature
- [x] Update `.claude/conventions/project-specific/project-tech.md` — add SHA-256 hashing, SecureRandom, OncePerRequestFilter
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add apikey package, api-keys page
- [x] Update `.claude/conventions/project-specific/project-architecture.md` — add API key auth flow, update ER diagram

**Acceptance criteria:**
- [x] Documentation reflects the new feature
- [x] All tests pass

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create API key with valid name | Backend | Steps 7, 3 |
| Create API key without name | Backend | Steps 7, 3 |
| Create API key with null name | Backend | Steps 7, 3 |
| Create API key with name exceeding max length | Backend | Steps 7, 3 |
| Create API key without authentication | Backend | Steps 7, 8 |
| Raw key is unique per creation | Backend | Step 7 |
| List API keys with pagination | Backend | Step 7 |
| List API keys returns no raw key | Backend | Step 7 |
| List API keys when none exist | Backend | Step 7 |
| List API keys default sort order | Backend | Step 7 |
| List API keys via API key authentication | Backend | Step 8 |
| Delete existing API key | Backend | Step 7 |
| Delete non-existent API key | Backend | Step 7 |
| Delete API key via API key authentication | Backend | Step 8 |
| Deleted key stops working immediately | Backend | Step 8 |
| Valid API key on GET endpoint | Backend | Step 8 |
| Valid API key on GET with path parameter | Backend | Step 8 |
| Valid API key on GET with query parameters | Backend | Step 8 |
| Valid API key on health endpoint | Backend | Step 8 |
| Valid API key on POST endpoint (403) | Backend | Step 8 |
| Valid API key on PUT endpoint (403) | Backend | Step 8 |
| Valid API key on DELETE endpoint (403) | Backend | Step 8 |
| Invalid API key (401) | Backend | Step 8 |
| Empty API key header (401) | Backend | Step 8 |
| No API key header falls through to JWT | Backend | Step 8 |
| Both API key and JWT header present | Backend | Step 8 |
| Key hash is stored not raw key | Backend | Step 7 |
| Different raw keys produce different hashes | Backend | Step 7 |
| Display API keys in table | Frontend | Steps 6, 9 |
| Empty state | Frontend | Steps 6, 9 |
| Sidebar navigation | Frontend | Steps 5, 9 |
| Create dialog opens and submits | Frontend | Steps 6, 9 |
| Key created dialog with copy | Frontend | Steps 6, 9 |
| Key created dialog warning | Frontend | Steps 6, 9 |
| Key created dialog close refreshes list | Frontend | Steps 6, 9 |
| Create dialog validation | Frontend | Steps 6, 9 |
| Delete with confirmation | Frontend | Steps 6, 9 |
| Cancel delete | Frontend | Steps 6, 9 |
