# Implementation Steps: Admin Sub-Menu in Sidebar

## Step 1: i18n translations

- [x] Add `nav.serverStatus`, `nav.bearerToken`, `nav.brevo` to `de.ts` and `en.ts`
- [x] Keep `nav.admin`, `nav.apiKeys`, `nav.webhooks` unchanged

**Acceptance criteria:**
- [x] Frontend builds successfully

**Related behaviors:** (foundation for all navigation behaviors)

---

## Step 2: Split admin page into three sub-pages

- [x] Create `src/app/(app)/admin/status/page.tsx` — renders `HealthStatus` component with heading
- [x] Create `src/app/(app)/admin/token/page.tsx` — renders `BearerTokenCard` (extracted from admin page) with heading
- [x] Create `src/app/(app)/admin/brevo/page.tsx` — renders `BrevoSync` component with heading
- [x] Replace `src/app/(app)/admin/page.tsx` with a redirect to `/admin/status` using `redirect()` from `next/navigation`
- [x] Extract `BearerTokenCard` into its own component file if currently inline in admin page

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] `/admin/status`, `/admin/token`, `/admin/brevo` render correctly
- [x] `/admin` redirects to `/admin/status`

**Related behaviors:** Server Status page shows health check, Bearer Token page shows token card, Bearer Token page without session, Brevo page shows settings and import, Old /admin route redirects

---

## Step 3: Refactor sidebar with collapsible admin sub-menu

- [x] Modify `sidebar.tsx`: replace `bottomItems` with collapsible admin group
- [x] Add `ChevronDown` import from lucide-react
- [x] Admin sub-items: Server Status (`/admin/status`, `Activity`), Bearer Token (`/admin/token`, `KeyRound`), Brevo Integration (`/admin/brevo`, `RefreshCw`), API Keys (`/api-keys`, `KeyRound`), Webhooks (`/webhooks`, `Webhook`)
- [x] Desktop: clicking "Admin" parent toggles sub-menu (does not navigate); chevron rotates
- [x] Derive open state from pathname: open if `pathname.startsWith("/admin") || pathname.startsWith("/api-keys") || pathname.startsWith("/webhooks")`
- [x] Sub-items indented with `pl-10`
- [x] Active sub-item highlighted green; parent highlighted (brighter text) when any sub-item active
- [x] Mobile (Sheet): render all 5 admin sub-items as flat top-level entries (no parent grouping), with `onNavigate` for sheet closing

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Desktop sidebar shows collapsible admin group
- [x] Mobile sidebar shows flat admin items

**Related behaviors:** Admin menu is collapsed by default on non-admin pages, Admin menu expands on click, Admin menu collapses on click, Sub-item navigates to page, Active sub-item is highlighted, Admin menu is open on admin pages after refresh, Admin menu is open on API Keys page, Admin menu is open on Webhooks page, Admin menu closes when navigating away, Clicking Admin parent does not navigate, Mobile shows flat admin items, Mobile item navigates and closes sheet, Mobile active state

---

## Step 4: Frontend tests

- [x] Update `src/components/__tests__/sidebar.test.tsx` (if test framework resolves the pre-existing module error) or create a focused test
- [x] Test: Admin parent item visible in desktop sidebar
- [x] Test: sub-items visible after expand
- [x] Test: sub-item count is 5
- [x] Test: Server Status links to `/admin/status`
- [x] Test: API Keys links to `/api-keys`
- [x] Test: Webhooks links to `/webhooks`

**Acceptance criteria:**
- [x] Frontend tests pass (`pnpm test`)

**Related behaviors:** All desktop and mobile sidebar behaviors, all admin page split behaviors

---

## Step 5: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — update admin page description
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add admin sub-routes

**Acceptance criteria:**
- [x] Documentation reflects the new structure
- [x] All tests pass

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Admin menu is collapsed by default on non-admin pages | Frontend | Steps 3, 4 |
| Admin menu expands on click | Frontend | Steps 3, 4 |
| Admin menu collapses on click | Frontend | Steps 3, 4 |
| Sub-item navigates to page | Frontend | Steps 3, 4 |
| Active sub-item is highlighted | Frontend | Steps 3, 4 |
| Admin menu is open on admin pages after refresh | Frontend | Steps 3, 4 |
| Admin menu is open on API Keys page | Frontend | Steps 3, 4 |
| Admin menu is open on Webhooks page | Frontend | Steps 3, 4 |
| Admin menu closes when navigating away | Frontend | Steps 3, 4 |
| Clicking Admin parent does not navigate | Frontend | Steps 3, 4 |
| Mobile shows flat admin items | Frontend | Steps 3, 4 |
| Mobile item navigates and closes sheet | Frontend | Steps 3, 4 |
| Mobile active state | Frontend | Steps 3, 4 |
| Server Status page shows health check | Frontend | Steps 2, 4 |
| Server Status page when backend is down | Frontend | Step 2 |
| Bearer Token page shows token card | Frontend | Steps 2, 4 |
| Bearer Token page without session | Frontend | Step 2 |
| Brevo page shows settings and import | Frontend | Steps 2, 4 |
| Old /admin route redirects | Frontend | Steps 2, 4 |
| Unknown admin sub-route | Frontend | Step 2 |
