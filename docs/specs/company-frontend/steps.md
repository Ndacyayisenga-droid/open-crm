# Implementation Steps: Company Frontend

## Step 1: Install shadcn/ui Components and API Client

- [x] Install shadcn/ui components: Button, Input, Table, Dialog, AlertDialog, Label, Separator, Sheet, Skeleton, Badge
- [x] Create `src/lib/api.ts` — typed API client with functions: `getCompanies`, `getCompany`, `createCompany`, `updateCompany`, `deleteCompany`, `restoreCompany`
- [x] Define TypeScript interfaces for `CompanyDto`, `CompanyCreateDto`, `Page<T>` in `src/lib/types.ts`
- [x] Extend `src/lib/constants.ts` with all company-related i18n strings (list, detail, form, delete, empty states, errors)

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] All shadcn/ui components are available
- [x] API client compiles with correct types

**Related behaviors:** None directly — foundation for all UI steps

---

## Step 2: Sidebar Navigation and Layout Restructure

- [x] Create `src/components/sidebar.tsx` — vertical sidebar with "Server-Health" and "Firmen" entries, oe-dark background, oe-white text, active state highlighting. Uses Sheet for mobile overlay.
- [x] Modify `src/app/layout.tsx` — wrap children with sidebar layout, responsive (sidebar on desktop, hamburger + Sheet on mobile)
- [x] Move health page: create `src/app/health/page.tsx` with the current health check logic from `src/app/page.tsx`
- [x] Change `src/app/page.tsx` to redirect to `/companies` using `redirect()` from `next/navigation`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Sidebar visible on desktop, hamburger on mobile
- [x] `/` redirects to `/companies`
- [x] `/health` shows the health status page
- [x] Brand colors applied (oe-dark sidebar, Montserrat headings)

**Related behaviors:** Sidebar shows navigation entries, Sidebar navigates to pages, Sidebar collapses to hamburger on mobile, Root page redirects to companies

---

## Step 3: Company List Page

- [x] Create `src/app/companies/page.tsx` — page wrapper that renders the company list component
- [x] Create `src/components/company-list.tsx` — client component with:
  - Table (Name, Website columns) with clickable rows
  - Filter bar (name, city, country inputs)
  - Sort controls
  - Pagination controls (prev/next, page info)
  - "Neue Firma" button (links to `/companies/new`)
  - "Archivierte Firmen anzeigen" toggle
  - Delete button per row (opens confirmation dialog)
  - Empty state: "Keine Firmen vorhanden. Erstellen Sie die erste Firma."
  - Loading skeleton while fetching
- [x] Create `src/components/delete-confirm-dialog.tsx` — reusable AlertDialog with confirm/cancel, supports error display for 409

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] List fetches and displays companies from `/api/companies`
- [x] Filters update the list on change
- [x] Pagination navigates between pages
- [x] Empty state shown when no companies exist
- [x] Skeleton shown while loading

**Related behaviors:** List displays companies with name and website, List shows empty state when no companies exist, List shows pagination controls, List navigates pages via pagination, List filters by name, List filters by city, List filters by country, List sorts companies, List excludes soft-deleted companies by default, List shows "Neue Firma" button, Clicking a company row navigates to detail, List shows loading state while fetching

---

## Step 4: Archived Companies View and Restore

- [x] Add "Archivierte Firmen anzeigen" toggle to company list (calls API with `?includeDeleted=true`)
- [x] Archived companies shown with muted/grayed styling
- [x] Replace delete button with "Wiederherstellen" button for archived entries
- [x] Implement restore via POST `/api/companies/{id}/restore`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Toggle shows/hides archived companies
- [x] Archived companies are visually distinct
- [x] Restore button calls API and refreshes list

**Related behaviors:** List shows archived companies when toggled, List shows restore button for archived companies, Restore button restores company

---

## Step 5: Company Detail Page

- [x] Create `src/app/companies/[id]/page.tsx` — server component fetching company data
- [x] Create `src/components/company-detail.tsx` — displays all fields (Name, E-Mail, Website, Straße, Hausnummer, PLZ, Stadt, Land) in a structured card layout
  - "Bearbeiten" button → `/companies/{id}/edit`
  - "Löschen" button → opens delete confirmation dialog
  - Comments placeholder section: "Kommentare" heading, "Keine Kommentare vorhanden", disabled "Kommentar hinzufügen" button
- [x] Handle 404 — show "not found" message when company doesn't exist
- [x] Loading skeleton while fetching

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] All company fields displayed
- [x] Edit and delete buttons present
- [x] Comment placeholder section visible
- [x] 404 handling works

**Related behaviors:** Detail page shows all company fields, Detail page shows edit button, Detail page shows delete button, Detail page shows comment placeholder, Detail page shows 404 for non-existent company, Detail shows loading state while fetching

---

## Step 6: Company Create Page

- [x] Create `src/app/companies/new/page.tsx` — page wrapper
- [x] Create `src/components/company-form.tsx` — reusable form component for create and edit:
  - Fields: Name (required), E-Mail, Website, Straße, Hausnummer, PLZ, Stadt, Land
  - Client-side validation (name required)
  - "Speichern" and "Abbrechen" buttons
  - Error display for API failures
  - Uses shadcn/ui Input, Label, Button
- [x] On create success: redirect to `/companies/{id}` (detail page)
- [x] "Abbrechen" navigates to `/companies`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Form renders all fields
- [x] Name validation prevents submission when empty
- [x] Successful create redirects to detail
- [x] Cancel navigates to list
- [x] API error shown inline

**Related behaviors:** Create page shows form with all fields, Create form validates name is required, Create form submits and redirects to detail, Create form cancel navigates back, Create form shows error on API failure

---

## Step 7: Company Edit Page

- [x] Create `src/app/companies/[id]/edit/page.tsx` — fetches company data, renders form pre-filled
- [x] Reuse `company-form.tsx` in edit mode (pre-populated fields, PUT instead of POST)
- [x] On edit success: redirect to `/companies/{id}` (detail page)
- [x] "Abbrechen" navigates to `/companies/{id}`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Form pre-filled with existing data
- [x] Name validation works
- [x] Successful update redirects to detail
- [x] Cancel navigates to detail

**Related behaviors:** Edit page shows form pre-filled with existing data, Edit form submits and redirects to detail, Edit form validates name is required, Edit form cancel navigates back to detail

---

## Step 8: Delete Flows (List and Detail)

- [x] Wire delete button in list to open `delete-confirm-dialog` with company name
- [x] Wire delete button on detail page to open same dialog
- [x] On confirm: call DELETE API, handle success (refresh list / redirect to list)
- [x] On cancel: close dialog, no action
- [x] On 409 error: show error dialog "Die Firma kann nicht gelöscht werden, da noch Kontakte zugeordnet sind."

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Confirmation dialog shows company name
- [x] Successful delete removes company from list / redirects from detail
- [x] Cancel closes dialog without action
- [x] 409 error shows error message

**Related behaviors:** Delete from list shows confirmation dialog, Delete confirmation soft-deletes and refreshes list, Delete cancel closes dialog without action, Delete from detail page shows confirmation and redirects, Delete fails with 409 shows error dialog

---

## Step 9: Responsive Design Polish

- [x] Verify all pages work on mobile viewport
- [x] Table horizontal scroll or card layout on small screens
- [x] Form fields stack properly on mobile
- [x] Sidebar hamburger menu works correctly

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] All functionality accessible on mobile viewport
- [x] No horizontal overflow or broken layouts

**Related behaviors:** Layout is responsive on mobile

---

## Step 10: Frontend Tests — Navigation and Layout

- [x] Test: sidebar renders with "Server-Health" and "Firmen" entries
- [x] Test: sidebar hamburger menu on mobile viewport
- [x] Test: root page redirects to `/companies`

**Acceptance criteria:**
- [x] `pnpm test` passes

**Related behaviors:** Sidebar shows navigation entries, Sidebar collapses to hamburger on mobile, Root page redirects to companies

---

## Step 11: Frontend Tests — Company List

- [x] Test: company list renders companies with name and website
- [x] Test: empty state shows message when no companies
- [x] Test: pagination controls visible with > 20 items
- [x] Test: filter inputs update displayed results
- [x] Test: sort changes order
- [x] Test: soft-deleted excluded by default
- [x] Test: archived toggle shows soft-deleted companies
- [x] Test: restore button shown for archived companies
- [x] Test: clicking row navigates to detail
- [x] Test: "Neue Firma" button links to `/companies/new`
- [x] Test: loading skeleton shown while fetching

**Acceptance criteria:**
- [x] `pnpm test` passes

**Related behaviors:** List displays companies with name and website, List shows empty state when no companies exist, List shows pagination controls, List navigates pages via pagination, List filters by name, List filters by city, List filters by country, List sorts companies, List excludes soft-deleted companies by default, List shows archived companies when toggled, List shows restore button for archived companies, Clicking a company row navigates to detail, List shows "Neue Firma" button, List shows loading state while fetching

---

## Step 12: Frontend Tests — Company Detail

- [x] Test: detail page renders all company fields
- [x] Test: edit button links to edit page
- [x] Test: delete button present
- [x] Test: comment placeholder section shown
- [x] Test: 404 message for non-existent company
- [x] Test: loading skeleton shown while fetching

**Acceptance criteria:**
- [x] `pnpm test` passes

**Related behaviors:** Detail page shows all company fields, Detail page shows edit button, Detail page shows delete button, Detail page shows comment placeholder, Detail page shows 404 for non-existent company, Detail shows loading state while fetching

---

## Step 13: Frontend Tests — Create and Edit Forms

- [x] Test: create form renders all fields with save/cancel buttons
- [x] Test: create form validates name is required
- [x] Test: create form submits and calls API
- [x] Test: create cancel navigates to list
- [x] Test: create form shows error on API failure
- [x] Test: edit form pre-fills existing data
- [x] Test: edit form submits and calls API
- [x] Test: edit form validates name is required
- [x] Test: edit cancel navigates to detail

**Acceptance criteria:**
- [x] `pnpm test` passes

**Related behaviors:** Create page shows form with all fields, Create form validates name is required, Create form submits and redirects to detail, Create form cancel navigates back, Create form shows error on API failure, Edit page shows form pre-filled with existing data, Edit form submits and redirects to detail, Edit form validates name is required, Edit form cancel navigates back to detail

---

## Step 14: Frontend Tests — Delete and Restore

- [x] Test: delete button opens confirmation dialog with company name
- [x] Test: confirm deletes company and refreshes list
- [x] Test: cancel closes dialog without action
- [x] Test: delete from detail redirects to list
- [x] Test: 409 error shows error dialog
- [x] Test: restore button calls restore API

**Acceptance criteria:**
- [x] `pnpm test` passes
- [x] `pnpm build` succeeds (final verification)

**Related behaviors:** Delete from list shows confirmation dialog, Delete confirmation soft-deletes and refreshes list, Delete cancel closes dialog without action, Delete from detail page shows confirmation and redirects, Delete fails with 409 shows error dialog, Restore button restores company, Sidebar navigates to pages

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Sidebar shows navigation entries | Frontend | Steps 2, 10 |
| Sidebar navigates to pages | Frontend | Steps 2, 14 |
| Sidebar collapses to hamburger on mobile | Frontend | Steps 2, 10 |
| Root page redirects to companies | Frontend | Steps 2, 10 |
| List displays companies with name and website | Frontend | Steps 3, 11 |
| List shows empty state when no companies exist | Frontend | Steps 3, 11 |
| List shows pagination controls | Frontend | Steps 3, 11 |
| List navigates pages via pagination | Frontend | Steps 3, 11 |
| List filters by name | Frontend | Steps 3, 11 |
| List filters by city | Frontend | Steps 3, 11 |
| List filters by country | Frontend | Steps 3, 11 |
| List sorts companies | Frontend | Steps 3, 11 |
| List excludes soft-deleted companies by default | Frontend | Steps 3, 11 |
| List shows archived companies when toggled | Frontend | Steps 4, 11 |
| List shows restore button for archived companies | Frontend | Steps 4, 11 |
| Clicking a company row navigates to detail | Frontend | Steps 3, 11 |
| List shows "Neue Firma" button | Frontend | Steps 3, 11 |
| Detail page shows all company fields | Frontend | Steps 5, 12 |
| Detail page shows edit button | Frontend | Steps 5, 12 |
| Detail page shows delete button | Frontend | Steps 5, 12 |
| Detail page shows comment placeholder | Frontend | Steps 5, 12 |
| Detail page shows 404 for non-existent company | Frontend | Steps 5, 12 |
| Create page shows form with all fields | Frontend | Steps 6, 13 |
| Create form validates name is required | Frontend | Steps 6, 13 |
| Create form submits and redirects to detail | Frontend | Steps 6, 13 |
| Create form cancel navigates back | Frontend | Steps 6, 13 |
| Create form shows error on API failure | Frontend | Steps 6, 13 |
| Edit page shows form pre-filled with existing data | Frontend | Steps 7, 13 |
| Edit form submits and redirects to detail | Frontend | Steps 7, 13 |
| Edit form validates name is required | Frontend | Steps 7, 13 |
| Edit form cancel navigates back to detail | Frontend | Steps 7, 13 |
| Delete from list shows confirmation dialog | Frontend | Steps 8, 14 |
| Delete confirmation soft-deletes and refreshes list | Frontend | Steps 8, 14 |
| Delete cancel closes dialog without action | Frontend | Steps 8, 14 |
| Delete from detail page shows confirmation and redirects | Frontend | Steps 8, 14 |
| Delete fails with 409 shows error dialog | Frontend | Steps 8, 14 |
| Restore button restores company | Frontend | Steps 4, 14 |
| Layout is responsive on mobile | Frontend | Step 9 |
| List shows loading state while fetching | Frontend | Steps 3, 11 |
| Detail shows loading state while fetching | Frontend | Steps 5, 12 |
