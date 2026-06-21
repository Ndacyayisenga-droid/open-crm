# Implementation Steps: Audit Log View

## Overview

Read-only admin page listing every audit log entry (INSERT / UPDATE / DELETE). Backend: paginated `GET /api/audit-logs` with optional `entityType` and `user` filters, plus `GET /api/audit-logs/entity-types` for the filter dropdown. Frontend: server page (IT-ADMIN check) + paginated client table with two filter dropdowns, behind a new admin sub-menu nav item.

## Important deviation from `design.md`

The design's pseudocode (`auditLogDataService.findByEntityType(entityType, pageable)`, etc.) does not match the actual `AuditLogDataService` API in `com.open-elements:spring-services:0.10.0`. The data service exposes **unpaginated** filter methods that return `List<AuditLogDto>`. Using them would require in-memory pagination over the full table — unacceptable for an audit log that may grow without bound.

To honour the design's intent (pagination + filtering at the database layer), this implementation introduces a **project-local repository** (`CrmAuditLogRepository`) that queries `AuditLogEntity` directly with paginated Spring Data methods. This is wiring code, not new business logic, and uses only the public types already exposed by the dependency. The DTO shape and `findAll(Pageable)` for the unfiltered case still come from the data service / `AuditLogDto.fromEntity`.

The endpoint contract, query parameters, response payload, and filter semantics in `design.md` are honoured exactly.

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| IT-ADMIN can access the audit log page | Frontend | 4, 8 |
| Non-IT-ADMIN is shown the forbidden page | Frontend | 4, 8 |
| Unauthenticated request to API is rejected | Backend | 1, 6 |
| Non-IT-ADMIN request to API is rejected | Backend | 1, 6 |
| Audit log entry visible for IT-ADMIN (sidebar) | Frontend | 5, 8 |
| Audit log entry hidden for non-IT-ADMIN (sidebar) | Frontend | 5, 8 |
| Audit log entries are displayed in a table (sorted DESC) | Frontend | 4, 8 |
| All fields are displayed correctly | Frontend | 4, 8 |
| System user entries are displayed | Frontend | 4, 8 |
| Default page size is 20 | Frontend | 4, 8 |
| Page size is persisted in localStorage | Frontend | 4, 8 |
| Page size options are available | Frontend | 4, 8 |
| Changing page size resets to first page | Frontend | 4, 8 |
| Pagination controls shown when multiple pages exist | Frontend | 4, 8 |
| Pagination controls hidden for single page | Frontend | 4, 8 |
| Navigate to next page | Frontend | 4, 8 |
| Previous button disabled on first page | Frontend | 4, 8 |
| Next button disabled on last page | Frontend | 4, 8 |
| Entity type dropdown is populated from API | Frontend | 4, 8 |
| Filtering by entity type | Frontend | 4, 8 |
| Clearing entity type filter | Frontend | 4, 8 |
| User dropdown is populated from user list | Frontend | 4, 8 |
| System is not in the user dropdown | Frontend | 4, 8 |
| Filtering by user | Frontend | 4, 8 |
| Clearing user filter | Frontend | 4, 8 |
| Both filters applied simultaneously | Frontend | 4, 8 |
| Clearing one filter keeps the other active | Frontend | 4, 8 |
| No audit log entries exist (empty state) | Frontend | 4, 8 |
| Filters produce no results (empty state) | Frontend | 4, 8 |
| Skeleton rows shown while loading | Frontend | 4, 8 |
| GET /api/audit-logs without filters | Backend | 1, 6 |
| GET /api/audit-logs with entityType filter | Backend | 1, 6 |
| GET /api/audit-logs with user filter | Backend | 1, 6 |
| GET /api/audit-logs with both filters | Backend | 1, 6 |
| GET /api/audit-logs/entity-types | Backend | 1, 6 |
| GET /api/audit-logs/entity-types with empty table | Backend | 1, 6 |

---

## Step 1 — Backend: AuditLogController + custom repository

**Changes**

- [x] Create package `com.openelements.crm.auditlog` under `backend/src/main/java/`
- [x] Create `CrmAuditLogRepository` extending `JpaRepository<AuditLogEntity, UUID>` with methods:
    - [x] `Page<AuditLogEntity> findByEntityType(String entityType, Pageable pageable)`
    - [x] `Page<AuditLogEntity> findByUserName(String userName, Pageable pageable)`
    - [x] `Page<AuditLogEntity> findByEntityTypeAndUserName(String entityType, String userName, Pageable pageable)`
    - [x] `@Query("SELECT DISTINCT a.entityType FROM AuditLogEntity a ORDER BY a.entityType") List<String> findDistinctEntityTypes()`
- [x] Create `AuditLogController` with class-level `@RestController`, `@RequestMapping("/api/audit-logs")`, `@Tag(name = "Audit Log")`, `@SecurityRequirement(name = "oidc")`, `@RequiresItAdmin`
- [x] `GET /api/audit-logs` returning `Page<AuditLogDto>`:
    - [x] Uses `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)` to pin default ordering to `createdAt DESC`
    - [x] Optional `@RequestParam` `entityType` and `user`
    - [x] Routes to repository method per filter combination, then maps via `page.map(AuditLogDto::fromEntity)`
- [x] `GET /api/audit-logs/entity-types` returning `List<String>` from `findDistinctEntityTypes()`
- [x] OpenAPI `@Operation` + `@Parameter` annotations for query params

**Acceptance criteria**

- [x] `cd backend && ./mvnw compile` succeeds
- [x] Controller is wired into Spring context (no startup failures)
- [x] Repository queries compile against `AuditLogEntity`

**Related behaviors:** GET /api/audit-logs without filters; GET /api/audit-logs with entityType filter; GET /api/audit-logs with user filter; GET /api/audit-logs with both filters; GET /api/audit-logs/entity-types; GET /api/audit-logs/entity-types with empty table; Unauthenticated request to API is rejected; Non-IT-ADMIN request to API is rejected

---

## Step 2 — Frontend: API functions + types

**Changes**

- [x] Add `AuditAction` and `AuditLogDto` types to `frontend/src/lib/types.ts`:
    - [x] `export type AuditAction = "INSERT" | "UPDATE" | "DELETE";`
    - [x] `AuditLogDto = { id, entityType, entityId, action: AuditAction, user, createdAt }` (all readonly strings except `action`)
- [x] Add `AuditLogListParams` (`page?`, `size?`, `entityType?`, `user?`) and `getAuditLogs()` to `frontend/src/lib/api.ts`, returning `Page<AuditLogDto>`
- [x] Add `getAuditLogEntityTypes(): Promise<string[]>` to `frontend/src/lib/api.ts`
- [x] Both functions use `apiFetch` with `cache: "no-store"`, throw on non-OK responses (mirror `getUsers` pattern)

**Acceptance criteria**

- [x] `pnpm --filter frontend exec tsc --noEmit` passes

---

## Step 3 — Frontend: i18n keys

**Changes**

- [x] Add `nav.auditLogs` to `frontend/src/lib/i18n/de.ts` (`"Audit Log"`) and `en.ts` (`"Audit Log"`)
- [x] Add new `auditLog` namespace to both with: `title`, `empty`, `loadError`, `filter.{entityType, entityTypeAll, user, userAll}`, `columns.{entityType, entityId, action, user, createdAt}`, `pagination.{perPage, previous, next, totalOne, totalOther}`
- [x] Use the existing `users` namespace as a structural template

**Acceptance criteria**

- [x] `pnpm --filter frontend exec tsc --noEmit` passes (translation typing across en/de matches)

---

## Step 4 — Frontend: server page + audit-logs-client component

**Changes**

- [x] Create `frontend/src/app/(app)/admin/audit-logs/page.tsx` (server component): calls `auth()`, returns `<ForbiddenPage />` when `ROLE_IT_ADMIN` is missing, otherwise renders `<AuditLogsClient />`
- [x] Create `frontend/src/app/(app)/admin/audit-logs/audit-logs-client.tsx` with:
    - [x] Exported constants: `PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200]`, `DEFAULT_PAGE_SIZE = 20`, `PAGE_SIZE_STORAGE_KEY = "pageSize.auditLogs"`
    - [x] State: `data`, `loading`, `error`, `page`, `pageSize` (hydrated from localStorage), `entityType` (string|undefined), `user` (string|undefined), `entityTypes` (string[]), `users` (UserDto[])
    - [x] Effect on mount: load entity types via `getAuditLogEntityTypes()` and users via `getUsers({ size: 200 })` to populate dropdowns
    - [x] Effect on `page`/`pageSize`/`entityType`/`user` change: load `getAuditLogs({ page, size, entityType, user })`
    - [x] Two `<Select>` filters above the table; "All types" / "All users" first option clears the filter and resets `page` to 0
    - [x] Table columns: Type, Entity ID, Action, User, Date (Date formatted via `new Date(createdAt).toLocaleString()`)
    - [x] Loading: 5 `<Skeleton>` rows (`data-testid="audit-logs-loading"`)
    - [x] Empty: `FileText` icon + `t.auditLog.empty` (`data-testid="audit-logs-empty"`)
    - [x] Error: `AlertCircle` + `t.auditLog.loadError` (`data-testid="audit-logs-error"`)
    - [x] Page-size `<Select>` (10/20/50/100/200); on change persists to localStorage and resets `page` to 0
    - [x] Total-count text using `t.auditLog.pagination.totalOne` / `totalOther`
    - [x] Prev/Next buttons hidden when `totalPages <= 1`; disabled at boundaries
    - [x] Imports from `@open-elements/ui`: `Button`, `Select*`, `Skeleton`, `Table*`

**Acceptance criteria**

- [x] `pnpm --filter frontend exec tsc --noEmit` passes

**Related behaviors:** all "Frontend" rows in the coverage table (excluding sidebar)

---

## Step 5 — Frontend: sidebar nav entry

**Changes**

- [x] Edit `frontend/src/app/(app)/layout.tsx`: add a `<NavItem>` for `/admin/audit-logs` inside the existing admin `<CollapsibleGroup>`, after the `Users` item
- [x] Use `FileText` icon from `lucide-react`, label `t.nav.auditLogs`
- [x] Visibility inherited from existing `canSeeAdmin` gate — no extra logic needed

**Acceptance criteria**

- [x] Sidebar shows "Audit Log" item under Admin for IT-ADMIN
- [x] Sidebar admin section is hidden for non-IT-ADMIN (existing behaviour, no regression)

**Related behaviors:** Audit log entry visible for IT-ADMIN; Audit log entry hidden for non-IT-ADMIN

---

## Step 6 — Backend tests

**Changes**

- [x] Create `backend/src/test/java/com/openelements/crm/auditlog/AuditLogControllerTest.java` using the same `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `asItAdmin(...)` JWT helper pattern as `UserControllerTest`
- [x] Tests covering the API contract:
    - [x] `GET /api/audit-logs` as IT-ADMIN → 200, body has `content[]`, `page.size`, `page.number`, `page.totalElements`, `page.totalPages`
    - [x] `GET /api/audit-logs` (no params) as IT-ADMIN → `page.size == 20`, `page.number == 0`
    - [x] `GET /api/audit-logs?page=0&size=10` as IT-ADMIN → `page.size == 10`
    - [x] `GET /api/audit-logs?entityType=CompanyDto` as IT-ADMIN → 200 (filter accepted, content limited to that type)
    - [x] `GET /api/audit-logs?user=Max%20Mustermann` as IT-ADMIN → 200 (filter accepted)
    - [x] `GET /api/audit-logs?entityType=CompanyDto&user=Max%20Mustermann` as IT-ADMIN → 200
    - [x] `GET /api/audit-logs/entity-types` as IT-ADMIN → 200, JSON array (may be empty)
    - [x] Sort defaults to `createdAt` DESC (verify via repeated entries inserted in test, or check the response default-sort param exposed in error / explicit `sort=createdAt,desc` accepted)
- [x] Add 401/403 coverage to `SecurityRoleIntegrationTest`:
    - [x] `GET /api/audit-logs` unauthenticated → 401
    - [x] `GET /api/audit-logs` as USER role → 403
    - [x] `GET /api/audit-logs` as ADMIN role → 403
    - [x] `GET /api/audit-logs` as IT-ADMIN → 200
- [x] Add the `AuditLogController` class to `PreAuthorizeAnnotationTest` so the `@RequiresItAdmin` annotation is asserted to be present

**Acceptance criteria**

- [x] `cd backend && ./mvnw test` passes (full suite green)

**Related behaviors:** Unauthenticated request to API is rejected; Non-IT-ADMIN request to API is rejected; GET /api/audit-logs without/with filters; GET /api/audit-logs/entity-types (and empty case)

---

## Step 7 — (intentionally empty)

Reserved to keep step numbering aligned with other specs in this repo. Skip.

---

## Step 8 — Frontend tests

**Changes**

- [x] Create `frontend/src/app/(app)/admin/audit-logs/__tests__/audit-logs-client.test.tsx` (Vitest + React Testing Library) following the `users-client.test.tsx` template
- [x] Mock `@/lib/api` (`getAuditLogs`, `getAuditLogEntityTypes`, `getUsers`), `next/navigation`, and `localStorage`
- [x] Tests:
    - [x] Constants: `PAGE_SIZE_OPTIONS`, `DEFAULT_PAGE_SIZE`, `PAGE_SIZE_STORAGE_KEY === "pageSize.auditLogs"`
    - [x] Renders skeleton during loading
    - [x] Renders empty state when `totalElements === 0`
    - [x] Renders error state when fetch rejects
    - [x] Renders rows with all columns (Type, Entity ID, Action, User, Date)
    - [x] System-user row renders (`user: "System"`)
    - [x] Default page size is 20 on first load (no localStorage)
    - [x] Reads page size from `localStorage["pageSize.auditLogs"]` on mount
    - [x] Changing page size requests page 0 (reset)
    - [x] Total-count text rendered (singular + plural)
    - [x] Prev/Next hidden when `totalPages === 1`
    - [x] Prev disabled on first page; Next disabled on last
    - [x] Click Next requests page 1; Click Prev requests page 0
    - [x] On mount, `getAuditLogEntityTypes` is called and the result populates filter options (assert via captured options or rerender)
    - [x] On mount, `getUsers` is called and dropdown excludes `System`
    - [x] Selecting an `entityType` filter calls `getAuditLogs({ entityType, page: 0, size })`
    - [x] Clearing the `entityType` filter calls `getAuditLogs({ page: 0, size })` (no entityType param)
    - [x] Selecting a `user` filter calls `getAuditLogs({ user, page: 0, size })`
    - [x] Clearing the `user` filter while `entityType` is set keeps `entityType` in the request
- [x] Note (mirror spec 089): Radix `<Select>` interactions are flaky in jsdom; where the underlying state can be exercised more directly (e.g. via a programmatic handler exposed for testing or via the page-size constant), prefer that over `fireEvent` on the popover. For filter interaction tests, use `fireEvent.change` on the underlying native control if possible, otherwise drive the `onValueChange` handler directly through a test helper.

**Acceptance criteria**

- [x] `pnpm --filter frontend test src/app/\(app\)/admin/audit-logs/__tests__/audit-logs-client.test.tsx` passes
- [x] No regressions in the rest of the suite (compare against `main` baseline)

**Related behaviors:** all "Frontend" rows in the coverage table (excluding sidebar visibility)

---

## Step 9 — Update project documentation

**Changes**

- [x] Update `.claude/conventions/project-specific/project-features.md` with the new admin audit log view feature (if file exists and is non-empty)
- [x] Update `.claude/conventions/project-specific/project-structure.md` if it lists admin pages
- [x] (Performed by spec-flow Step 6, not here) Update `specs/INDEX.md`: `090` status → `done`

**Acceptance criteria**

- [x] Project docs reference the new page accurately
