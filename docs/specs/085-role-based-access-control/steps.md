# Implementation Steps: Role-Based Access Control

Ordered, atomic implementation plan for spec `085-role-based-access-control`. Each step ends with build + test as acceptance criteria.

## Step 1 — Backend: Enable method security (`SecurityConfig`)

**Changes:**
- [x] Add `backend/src/main/java/com/openelements/crm/security/SecurityConfig.java` — `@Configuration` + `@EnableMethodSecurity` so `@PreAuthorize` is processed.

**Acceptance:**
- [x] `./mvnw -pl backend -q compile` succeeds.
- [x] Existing `FullSpringServiceConfig` continues to map the `roles` claim to `ROLE_<role>` authorities (no change needed — verified v0.5.0 of `spring-services`).

**Related behaviors:** _Backend — Role mapping_ (all scenarios rely on this).

## Step 2 — Backend: Guard CRM delete endpoints with `ADMIN`

**Changes:**
- [x] Annotate `CompanyController.delete` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `CompanyController.deleteLogo` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `ContactController.delete` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `ContactController.deletePhoto` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `TaskController.delete` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `TagController.delete` with `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Annotate `CommentController.delete` with `@PreAuthorize("hasRole('ADMIN')")`.

**Acceptance:**
- [x] `./mvnw -pl backend -q compile` succeeds.

**Related behaviors:** `User-None cannot delete`, `User-Admin can delete`, `User-ItAdmin cannot delete (CRM entities)`, `User-Both can delete`, `Delete with query parameter is still role-checked`.

## Step 3 — Backend: Guard admin controllers with `IT-ADMIN`

**Changes:**
- [x] Class-level `@PreAuthorize("hasRole('IT-ADMIN')")` on `ApiKeyController`.
- [x] Class-level `@PreAuthorize("hasRole('IT-ADMIN')")` on `WebhookController`.
- [x] Class-level `@PreAuthorize("hasRole('IT-ADMIN')")` on `BrevoSyncController`.

**Acceptance:**
- [x] `./mvnw -pl backend -q compile` succeeds.

**Related behaviors:** `User-None cannot access admin endpoints`, `User-Admin cannot access admin endpoints`, `User-ItAdmin can access admin endpoints`, `User-Both can access admin endpoints`.

## Step 4 — Backend: Integration tests for role enforcement

**Changes:**
- [x] Add `SecurityRoleIntegrationTest.java` — `@SpringBootTest` with `MockMvc` and `SecurityMockMvcRequestPostProcessors.jwt()`.
- [x] Tests for every affected endpoint × every role combination (None / ADMIN / IT-ADMIN / Both) asserting 2xx/204 vs 403.
- [x] Tests that verify unprotected endpoints (e.g. `GET /api/companies`) return 2xx for users with no roles.
- [x] Tests for `GET /api/health` → 200 even unauthenticated (permit-all in base SecurityConfig).
- [x] Tests verifying `DELETE /api/companies/{id}?deleteContacts=true` is rejected with 403 for User-None.
- [x] Unit-level test for the JWT → authorities mapping (via a MockMvc request that hits an ADMIN-only endpoint with `roles: ["ADMIN"]`, `["IT-ADMIN"]`, `["ADMIN","IT-ADMIN"]`, `[]`, and no claim).

**Acceptance:**
- [x] `./mvnw -pl backend -q test` passes.

**Related behaviors:** _Backend — Role mapping_ (all), _Backend — Delete endpoints_ (all), _Backend — Admin-area endpoints_ (all), _Backend — Unaffected endpoints_ (all).

## Step 5 — Frontend: i18n keys for errors + roleRequired tooltips

**Changes:**
- [x] Add `errors.forbidden.title/description/backToHome/deleteNoPermission` to `en.ts` + `de.ts`.
- [x] Add `errors.roleRequired.admin/itAdmin` to `en.ts` + `de.ts`.

**Acceptance:**
- [x] Types compile (`pnpm tsc --noEmit`).

**Related behaviors:** Supports frontend tooltip + 403 dialog scenarios.

## Step 6 — Frontend: Role helper module

**Changes:**
- [x] Add `frontend/src/lib/roles.ts` exporting `hasRole`, `ROLE_ADMIN`, `ROLE_IT_ADMIN`.

**Acceptance:**
- [x] Types compile.

**Related behaviors:** Supports all frontend role-based scenarios.

## Step 7 — Frontend: `ForbiddenPage` component

**Changes:**
- [x] Add `frontend/src/components/forbidden-page.tsx` — branded 403 page with heading, description, back-to-home link.

**Acceptance:**
- [x] Component renders in isolation.

**Related behaviors:** `Direct navigation without IT-ADMIN shows the 403 page`, `User-Admin also sees the 403 page on admin routes`.

## Step 8 — Frontend: `ForbiddenError` in API wrapper

**Changes:**
- [x] Export `ForbiddenError` class from `frontend/src/lib/api.ts`.
- [x] Make all `delete*` functions throw `ForbiddenError` on HTTP 403 (`deleteCompany`, `deleteCompanyLogo`, `deleteContact`, `deleteContactPhoto`, `deleteTask`, `deleteTag`, `deleteComment`).

**Acceptance:**
- [x] Types compile, existing callers still work (generic error paths are unchanged).

**Related behaviors:** Underpins `403 on delete surfaces a permission-specific message`.

## Step 9 — Frontend: Sidebar hides admin group without `IT-ADMIN`

**Changes:**
- [x] Use `useSession` + `hasRole(session, ROLE_IT_ADMIN)` in `sidebar.tsx`.
- [x] When the user lacks `IT-ADMIN`, render no admin parent button and no admin sub-items on desktop AND mobile.

**Acceptance:**
- [x] Component tests (Step 14) cover the four role combinations.

**Related behaviors:** `User-None sees no admin items (desktop)`, `User-Admin sees no admin items`, `User-ItAdmin sees all admin items`, `User-Both sees all admin items`, `Mobile sidebar respects the same rule`.

## Step 10 — Frontend: Gate admin routes with `ForbiddenPage`

**Changes:**
- [x] Convert `/admin/status`, `/admin/token`, `/admin/brevo`, `/api-keys`, `/webhooks` to perform `auth()` server-side check (server component wrapping existing client component).
- [x] If `IT-ADMIN` missing, render `ForbiddenPage` instead of the admin page body.

**Acceptance:**
- [x] Playwright-style integration not required; component-level test for `ForbiddenPage`.

**Related behaviors:** `Direct navigation without IT-ADMIN shows the 403 page`, `User-Admin also sees the 403 page on admin routes`, `User-ItAdmin can open admin routes`.

## Step 11 — Frontend: Disable delete buttons without `ADMIN` + tooltip

**Changes:**
- [x] In `company-detail.tsx`, `company-list.tsx`, `contact-detail.tsx`, `contact-list.tsx`, `task-detail.tsx`, `tag-list.tsx`, `company-comments.tsx`, `contact-comments.tsx`, `task-comments.tsx`: wrap every delete trigger with a `Tooltip` and set `disabled={!hasRole(session, ROLE_ADMIN)}` + tooltip text = `t.errors.roleRequired.admin` when lacking role. When the user has `ADMIN`, the existing tooltip text (if any) is shown instead.

**Acceptance:**
- [x] Component tests for at least `company-detail.tsx`, `contact-detail.tsx`, `task-detail.tsx`, `tag-list.tsx`, `company-comments.tsx`.

**Related behaviors:** `Without ADMIN the delete button is visible but disabled`, `With ADMIN the delete button is enabled`, `Clicking a disabled delete button does not open the dialog`.

## Step 12 — Frontend: 403 error surface in delete dialogs

**Changes:**
- [x] In `company-detail.tsx`, extend `handleDeleteAll` and `handleDeleteCompanyOnly` to `catch (e)` — if `e instanceof ForbiddenError`, keep the dialog open and set an error message via a new `error` prop on `CompanyDeleteDialog`.
- [x] Extend `CompanyDeleteDialog` to accept optional `error` + `errorTitle` props and render them like `DeleteConfirmDialog` does.
- [x] In other delete flows (contact-detail, tag-list, contact-list, company-list, company-comments, contact-comments, task-comments, task-detail), map `ForbiddenError` to `errors.forbidden.deleteNoPermission` via `setDeleteError(...)`. Non-403 errors keep their existing generic error messages.

**Acceptance:**
- [x] Frontend tests cover 403 path in `DeleteConfirmDialog` users and `CompanyDeleteDialog`.

**Related behaviors:** `403 on delete surfaces a permission-specific message`, `403 on company "delete with contacts" surfaces the same permission error`, `Non-403 errors still show the generic error`.

## Step 13 — Frontend tests: sidebar + delete buttons + forbidden + 403 dialog

**Changes:**
- [x] Extend `__tests__/sidebar.test.tsx` with tests for the four role combinations (hide/show admin group).
- [x] Add `__tests__/forbidden-page.test.tsx` — renders heading + description + back link.
- [x] Add `__tests__/role-delete-buttons.test.tsx` — verifies disabled state + tooltip on company/contact/task/tag/comment delete buttons without `ADMIN`, enabled with `ADMIN`.
- [x] Add `__tests__/delete-dialog-403.test.tsx` — verifies mapped `errors.forbidden.deleteNoPermission` message when `ForbiddenError` thrown; non-403 errors keep the existing generic text.

**Acceptance:**
- [x] `pnpm test` passes.

**Related behaviors:** All frontend scenarios.

## Step 14 — Update project documentation and INDEX

**Changes:**
- [x] Append a short "Role-based access control" section to `.claude/conventions/project-specific/project-features.md`.
- [x] Change spec 085 status in `specs/INDEX.md` from `open` → `done`.

**Acceptance:**
- [x] Files updated.

**Related behaviors:** Meta.

## Step 15 — Run final build + tests

**Acceptance:**
- [x] `cd backend && ./mvnw -q test` passes.
- [x] `cd frontend && pnpm test run` passes.
- [x] `cd frontend && pnpm tsc --noEmit` passes.

## Behavior coverage

| Scenario | Layer | Step |
|---|---|---|
| JWT with roles claim "ADMIN" grants ROLE_ADMIN authority | Backend | 1, 4 |
| JWT with roles claim "IT-ADMIN" grants ROLE_IT-ADMIN authority | Backend | 1, 4 |
| JWT with multiple roles grants multiple authorities | Backend | 1, 4 |
| JWT without a roles claim grants no role authorities | Backend | 1, 4 |
| JWT with empty roles claim grants no role authorities | Backend | 1, 4 |
| Unknown role values are mapped through unchanged | Backend | 1, 4 |
| User-None cannot delete | Backend | 2, 4 |
| User-Admin can delete | Backend | 2, 4 |
| User-ItAdmin cannot delete (CRM entities) | Backend | 2, 4 |
| User-Both can delete | Backend | 2, 4 |
| Delete with query parameter is still role-checked | Backend | 2, 4 |
| User-None cannot access admin endpoints | Backend | 3, 4 |
| User-Admin cannot access admin endpoints | Backend | 3, 4 |
| User-ItAdmin can access admin endpoints | Backend | 3, 4 |
| User-Both can access admin endpoints | Backend | 3, 4 |
| Read/create/update of CRM entities remains open to authenticated users | Backend | 4 |
| Health endpoint remains public | Backend | 4 |
| API key authentication continues to work without role checks | Backend | 4 |
| User-None sees no admin items (desktop) | Frontend | 9, 13 |
| User-Admin sees no admin items | Frontend | 9, 13 |
| User-ItAdmin sees all admin items | Frontend | 9, 13 |
| User-Both sees all admin items | Frontend | 9, 13 |
| Mobile sidebar respects the same rule | Frontend | 9, 13 |
| Direct navigation without IT-ADMIN shows the 403 page | Frontend | 7, 10, 13 |
| User-Admin also sees the 403 page on admin routes | Frontend | 7, 10, 13 |
| User-ItAdmin can open admin routes | Frontend | 10 |
| Without ADMIN the delete button is visible but disabled | Frontend | 11, 13 |
| With ADMIN the delete button is enabled | Frontend | 11, 13 |
| Clicking a disabled delete button does not open the dialog | Frontend | 11, 13 |
| 403 on delete surfaces a permission-specific message | Frontend | 8, 12, 13 |
| 403 on company "delete with contacts" surfaces the same permission error | Frontend | 8, 12, 13 |
| Non-403 errors still show the generic error | Frontend | 12, 13 |
| Role revocation during an active session | Both | 8, 12, 13 + 2/4 |
| Role addition during an active session | Frontend | 9, 11, 13 |
