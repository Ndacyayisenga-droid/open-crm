# Implementation Steps: Webhook Frontend View

## Step 1: TypeScript types and API client functions

- [x] Add `WebhookDto`, `WebhookCreateDto`, `WebhookUpdateDto` interfaces to `src/lib/types.ts`
- [x] Add `getWebhooks()`, `createWebhook()`, `updateWebhook()`, `deleteWebhook()`, `pingWebhook()` functions to `src/lib/api.ts`

**Acceptance criteria:**
- [x] Frontend builds successfully (`pnpm build`)
- [x] Types match backend DTO (id, url, active, lastStatus, lastCalledAt, createdAt, updatedAt)

**Related behaviors:** (foundation for all UI behaviors)

---

## Step 2: i18n translations

- [x] Add `webhooks` namespace to `src/lib/i18n/de.ts` with all labels (title, columns, status labels, create dialog, delete dialog, actions, pagination, empty state)
- [x] Add `webhooks` namespace to `src/lib/i18n/en.ts` with English translations
- [x] Add `webhooks` to `nav` in both translation files
- [x] Update `Translations` type in `src/lib/i18n/index.ts` if needed

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] All translation keys compile without type errors

**Related behaviors:** Page displays in German, Page displays in English

---

## Step 3: Sidebar navigation

- [x] Add Webhooks nav item to `src/components/sidebar.tsx` below Admin with Lucide `Webhook` icon and href `/webhooks`

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Webhooks appears in sidebar below Admin

**Related behaviors:** Webhook page is accessible via sidebar, Webhooks sidebar item uses Webhook icon

---

## Step 4: Webhook list page and component

- [x] Create `src/app/(app)/webhooks/page.tsx` rendering `<WebhookList />`
- [x] Create `src/components/webhook-list.tsx` with:
  - Table with columns: URL, Active, Last Status, Last Called, Actions
  - Last Status display logic (OK, Timeout, Connection Error, Bad Call (code), —)
  - Pagination with page size selector (localStorage `pageSize.webhooks`)
  - Loading state with skeletons
  - Empty state with create button
  - Active/Inactive toggle action (calls `updateWebhook`)
  - PING button (calls `pingWebhook`, no confirmation, no feedback)
  - Delete button opening `DeleteConfirmDialog`
  - Create webhook button in header opening a Dialog with URL input
  - All action buttons with Tooltips
  - Responsive: Last Status and Last Called hidden on mobile (`hidden md:table-cell`)

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Page renders at `/webhooks` with table, actions, and create dialog
- [x] All actions work against the backend API

**Related behaviors:** Table shows all webhook columns, Table shows webhook data correctly, Last Status displays OK/Timeout/Connection Error/Bad Call/dash, Table is paginated, Page size is persisted to localStorage, Loading state shows skeletons, Empty state with create button, Create dialog opens from header button, Create webhook with valid URL, Create webhook with empty URL shows validation, Create dialog can be cancelled, Create dialog shows error on API failure, Toggle active to inactive, Toggle inactive to active, PING fires on click without confirmation, PING works for inactive webhooks, PING result visible after manual refresh, Delete shows confirmation dialog, Delete removes webhook after confirmation, Delete can be cancelled, Delete shows error on API failure, Status columns hidden on mobile

---

## Step 5: Frontend component tests

- [x] Create `src/components/__tests__/webhook-list.test.tsx` with tests for:
  - Table renders with correct columns
  - Webhook data displayed correctly (URL, active indicator, status labels, timestamp)
  - Last Status mapping: 200→"OK", -1→"Timeout", 0→"Connection Error", 404→"Bad Call (404)", null→"—"
  - Empty state shown when no webhooks
  - Loading skeletons shown during fetch
  - Create dialog opens on button click
  - Create dialog submits URL and closes on success
  - Create dialog shows validation error on empty URL
  - Create dialog shows API error
  - Create dialog cancellable
  - Toggle active calls updateWebhook with toggled flag
  - PING button calls pingWebhook immediately
  - PING works for inactive webhooks
  - Delete button opens confirmation dialog
  - Delete confirmation calls deleteWebhook and refreshes
  - Delete cancellable
  - Delete shows API error
  - Pagination renders and changes page

**Acceptance criteria:**
- [x] All frontend tests pass (`pnpm test`)
- [x] Every behavioral scenario from behaviors.md has a corresponding test

**Related behaviors:** All 33 behavioral scenarios

---

## Step 6: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — update webhook feature to mention frontend management page
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add webhooks page and webhook-list component

**Acceptance criteria:**
- [x] Documentation reflects the new webhook frontend
- [x] All tests pass

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Webhook page is accessible via sidebar | Frontend | Steps 3, 5 |
| Webhooks sidebar item uses Webhook icon | Frontend | Steps 3, 5 |
| Table shows all webhook columns | Frontend | Steps 4, 5 |
| Table shows webhook data correctly | Frontend | Steps 4, 5 |
| Last Status displays OK for 2xx | Frontend | Steps 4, 5 |
| Last Status displays Timeout for -1 | Frontend | Steps 4, 5 |
| Last Status displays Connection Error for 0 | Frontend | Steps 4, 5 |
| Last Status displays Bad Call with code for 4xx/5xx | Frontend | Steps 4, 5 |
| Last Status displays dash for never called | Frontend | Steps 4, 5 |
| Table is paginated | Frontend | Steps 4, 5 |
| Page size is persisted to localStorage | Frontend | Steps 4, 5 |
| Loading state shows skeletons | Frontend | Steps 4, 5 |
| Empty state with create button | Frontend | Steps 4, 5 |
| Create dialog opens from header button | Frontend | Steps 4, 5 |
| Create webhook with valid URL | Frontend | Steps 4, 5 |
| Create webhook with empty URL shows validation | Frontend | Steps 4, 5 |
| Create dialog can be cancelled | Frontend | Steps 4, 5 |
| Create dialog shows error on API failure | Frontend | Steps 4, 5 |
| Toggle active to inactive | Frontend | Steps 4, 5 |
| Toggle inactive to active | Frontend | Steps 4, 5 |
| PING fires on click without confirmation | Frontend | Steps 4, 5 |
| PING works for inactive webhooks | Frontend | Steps 4, 5 |
| PING result visible after manual refresh | Frontend | Steps 4, 5 |
| Delete shows confirmation dialog | Frontend | Steps 4, 5 |
| Delete removes webhook after confirmation | Frontend | Steps 4, 5 |
| Delete can be cancelled | Frontend | Steps 4, 5 |
| Delete shows error on API failure | Frontend | Steps 4, 5 |
| Page displays in German | Frontend | Steps 2, 5 |
| Page displays in English | Frontend | Steps 2, 5 |
| Status columns hidden on mobile | Frontend | Steps 4, 5 |
