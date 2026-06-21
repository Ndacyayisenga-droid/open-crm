# Steps: Updates View

## Step 1 — Emit audit-log entries for comment CRUD (backend)

**Changes**

- [x] `CompanyService` — inject `AuditLogDataService` + `UserService`; after add/update/delete comment, emit `AuditLogDataService.createEntry("CompanyComment", companyId, INSERT|UPDATE|DELETE, currentUser)`.
- [x] `ContactService` — analogous: entityType `"ContactComment"`, `entityId = contactId`.

**Acceptance criteria**

- [x] Backend compiles.
- [x] Comment CRUD via `/api/companies/{id}/comments` and `/api/contacts/{id}/comments` writes an audit-log row with the correct entityType / entityId / action / user.

**Related behaviors**: Adding a comment to a company emits an audit entry; Updating a company comment emits an audit entry; Deleting a company comment emits an audit entry; Same invariants for contact comments.

## Step 2 — Updates DTO, enum, service (backend)

**Changes**

- [x] New package `com.openelements.crm.updates`.
- [x] `UpdateType` enum with 12 values per design.
- [x] `UpdateEntryDto(id, type, entityId, entityName, user, createdAt)` record.
- [x] `UpdatesService` with `load(int size)` returning `List<UpdateEntryDto>`:
  - iterative fetch from `AuditLogDataService.findAll(Pageable)`
  - filter to relevant entityTypes
  - dedupe consecutive UPDATEs (same entityType + entityId + user.id, action == UPDATE) — newest wins
  - resolve `entityName` via `CompanyRepository` / `ContactRepository`; null for `*_DELETED` company/contact and unresolved entities
  - null `entityId` for `COMPANY_DELETED` and `CONTACT_DELETED`

**Acceptance criteria**

- [x] Backend compiles.

## Step 3 — UpdatesController (backend)

**Changes**

- [x] `UpdatesController` at `/api/updates`, `@SecurityRequirement("oidc")`, **no** role restriction.
- [x] `GET /api/updates?size=&page=` returning `Page<UpdateEntryDto>` with `totalPages=1`, `totalElements = content.size()`.
- [x] `size` validated against `{20, 50, 100, 200}`; default `20`; other values → `400`.
- [x] `page > 0` returns empty content (no error).

**Acceptance criteria**

- [x] Backend compiles.

**Related behaviors**: Unauthenticated request is rejected; Any authenticated user can read updates; IT-ADMIN user can also read updates; Default size is 20; Size 50/100/200 accepted; Disallowed size rejected; Page parameter ignored beyond page 0.

## Step 4 — Backend tests

**Changes**

- [x] `UpdatesServiceTest` (`@SpringBootTest`) covering: filter, dedupe (same user, different users, different entities, CREATE+UPDATE, UPDATE+DELETE, contact dedupe, comment dedupe, dedupe across filtered entries), name resolution including missing entities, comment parent resolution, COMPANY/CONTACT_DELETED with null id/name, sort order, empty audit log, fewer than size, iterative fetch.
- [x] `UpdatesControllerTest` (`@SpringBootTest` + `MockMvc`) covering: 401 unauth, 200 for normal user, 200 for IT-ADMIN, default size, valid sizes, invalid sizes → 400, `page=2` returns empty, sort order.
- [x] `CompanyServiceCommentAuditTest` / `ContactServiceCommentAuditTest` (or extend existing) covering audit emission on comment add/update/delete.
- [x] Filter test for out-of-scope entity types (user, webhook, tag, api-key).
- [x] Task-comment exclusion (task comments don't appear).

**Acceptance criteria**

- [x] All new tests pass.
- [x] `mvn verify` is green.

**Related behaviors**: all backend scenarios from `behaviors.md`.

## Step 5 — Frontend i18n + nav entry

**Changes**

- [x] Add `updates` block + `nav.updates` to `frontend/src/lib/i18n/de.ts` and `en.ts` (or equivalent shared file).
- [x] Sidebar / app layout: add Updates as first nav item before Companies/Contacts/Tags, visible to all authenticated users.

**Acceptance criteria**

- [x] Frontend compiles.
- [x] Sidebar renders Updates entry.

**Related behaviors**: Navigation entry is shown to every authenticated user.

## Step 6 — Frontend page + client

**Changes**

- [x] New route `frontend/src/app/(app)/updates/page.tsx` (server entry).
- [x] `updates-client.tsx` client component:
  - fetch `GET /api/updates?size=N` on mount and on size change
  - page-size combobox (20/50/100/200), persists in `localStorage` under `updates.pageSize`
  - render rows: localized text built from `type` + `entityName`; entity-name portion is a link to `/companies/{id}` or `/contacts/{id}` when `entityId != null`; plain text otherwise
  - actor display name + relative time
  - empty state, loading skeleton, error banner

**Acceptance criteria**

- [x] Frontend compiles.
- [x] Manual smoke check: `/updates` renders.

**Related behaviors**: Page renders empty-state text; Entity name is a link when entityId is non-null; No link for COMPANY_DELETED and CONTACT_DELETED; Comment entries link to the parent entity; Page-size combobox persists choice; Manual reload reflects newly created entries; Page does not auto-refresh; German user / English user sees correct texts.

## Step 7 — Frontend tests

**Changes**

- [x] Vitest tests for `updates-client.tsx` covering: rendering rows for each type, link vs plain-text behaviour, empty state, `localStorage` persistence on size change, language switching (de vs en text), no auto-refresh.

**Acceptance criteria**

- [x] `pnpm vitest` green.

## Step 8 — Documentation + index + PR

**Changes**

- [x] Update `specs/INDEX.md` status to `done`.
- [x] Update `.claude/conventions/project-specific/project-features.md` and friends if applicable.

**Acceptance criteria**

- [x] Reviewed by spec-review and quality-review, no Critical / Improvement findings open.

## Behavior Coverage

| Scenario | Layer | Step |
|---|---|---|
| Unauthenticated request is rejected | Backend | 4 |
| Any authenticated user can read updates | Backend | 4 |
| IT-ADMIN user can also read updates | Backend | 4 |
| Default size is 20 | Backend | 4 |
| Size 50, 100 and 200 are accepted | Backend | 4 |
| Disallowed size is rejected | Backend | 4 |
| Page parameter is ignored beyond page 0 | Backend | 4 |
| Company created | Backend | 4 |
| Company updated | Backend | 4 |
| Company deleted | Backend | 4 |
| Renamed company shows current name in older entries | Backend | 4 |
| Contact created | Backend | 4 |
| Contact updated | Backend | 4 |
| Contact deleted | Backend | 4 |
| Comment added to a company | Backend | 4 |
| Comment on a company is updated | Backend | 4 |
| Comment on a company is deleted | Backend | 4 |
| Comment added to a contact | Backend | 4 |
| Comment on a contact is updated | Backend | 4 |
| Comment on a contact is deleted | Backend | 4 |
| Two consecutive UPDATEs on the same company by the same user are merged | Backend | 4 |
| Consecutive UPDATEs by different users are not merged | Backend | 4 |
| Consecutive UPDATEs on different companies by the same user are not merged | Backend | 4 |
| CREATE followed by UPDATE on the same company by the same user is not merged | Backend | 4 |
| UPDATE followed by DELETE on the same company by the same user is not merged | Backend | 4 |
| Dedupe applies to contacts identically | Backend | 4 |
| Dedupe applies to comment UPDATEs | Backend | 4 |
| Dedupe skips filtered entries between candidates | Backend | 4 |
| Entries are sorted newest first | Backend | 4 |
| Empty audit log returns empty content | Backend | 4 |
| Fewer entries than requested size | Backend | 4 |
| Iterative fetch returns up to size after heavy dedupe | Backend | 4 |
| Entity vanishes between audit emission and read | Backend | 4 |
| Comment parent has been deleted | Backend | 4 |
| Adding a comment to a company emits an audit entry | Backend | 4 |
| Updating a company comment emits an audit entry | Backend | 4 |
| Deleting a company comment emits an audit entry | Backend | 4 |
| Same invariants for contact comments | Backend | 4 |
| Task comments are not emitted as updates | Backend | 4 |
| User-related audit entries are not exposed | Backend | 4 |
| Webhook, API key, tag audit entries are not exposed | Backend | 4 |
| Navigation entry is shown to every authenticated user | Frontend | 7 |
| Page renders the empty-state text when content is empty | Frontend | 7 |
| Entity name in the entry text is a link when entityId is non-null | Frontend | 7 |
| No link for COMPANY_DELETED and CONTACT_DELETED | Frontend | 7 |
| Comment entries link to the parent entity | Frontend | 7 |
| Page-size combobox persists choice in localStorage | Frontend | 7 |
| Manual reload reflects newly created entries | Frontend | 7 |
| Page does not auto-refresh | Frontend | 7 |
| German user sees German texts | Frontend | 7 |
| English user sees English texts | Frontend | 7 |
