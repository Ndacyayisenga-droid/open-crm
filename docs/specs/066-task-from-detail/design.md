# Design: Create Task from Detail View

## GitHub Issue

—

## Summary

Users should be able to create tasks directly from company and contact detail views, as well as from list table action buttons. The button navigates to the task create form (`/tasks/new`) with the entity pre-selected via URL parameters. The entity type toggle and dropdown are locked so the user only needs to fill in the task-specific fields (action, due date, status, tags).

## Goals

- Add "Create Task" button to company and contact detail views
- Add "Create Task" action button to company and contact list tables
- Pre-select and lock the entity in the task create form via URL parameters

## Non-goals

- Backend changes — the task create API already accepts companyId/contactId
- Showing existing tasks in company/contact detail views (future spec with nested endpoints)
- Changing the task create form layout

## Technical Approach

### URL Parameter Convention

The task create form reads optional URL search parameters:

- `/tasks/new?companyId=<uuid>` — pre-selects entity type "Company" and the specific company
- `/tasks/new?contactId=<uuid>` — pre-selects entity type "Contact" and the specific contact

When a parameter is present:
- The entity type toggle is set and **disabled**
- The entity dropdown is set to the specified entity and **disabled**
- The user can still fill in action, due date, status, and tags normally

When no parameter is present: the form behaves as before (Spec 064).

### Detail View Buttons

**Company detail** (`company-detail.tsx`):
- Add a "Create Task" button in the header action buttons area (alongside Edit, Delete)
- Icon: `CheckSquare` from lucide-react (matches the sidebar Tasks icon)
- On click: `router.push(\`/tasks/new?companyId=\${company.id}\`)`

**Contact detail** (`contact-detail.tsx`):
- Same pattern as company detail
- On click: `router.push(\`/tasks/new?contactId=\${contact.id}\`)`

### List Table Action Buttons

**Company list** (`company-list.tsx`):
- Add a `CheckSquare` icon button in the actions column (alongside Edit, Comment)
- On click: `router.push(\`/tasks/new?companyId=\${company.id}\`)`

**Contact list** (`contact-list.tsx`):
- Same pattern
- On click: `router.push(\`/tasks/new?contactId=\${contact.id}\`)`

### Task Form Changes (`task-form.tsx`)

- Read `companyId` and `contactId` from `useSearchParams()`
- If a parameter is present:
  - Set entity type state accordingly
  - Set entity ID state to the provided value
  - Disable the entity type toggle and entity dropdown
- The form submission uses the pre-filled entity ID as usual — no special handling needed
- After successful creation: navigate to the task detail view (existing Spec 064 behavior)

### i18n

| Key | EN | DE |
|-----|----|----|
| `companies.detail.createTask` | "Create Task" | "Aufgabe erstellen" |
| `contacts.detail.createTask` | "Create Task" | "Aufgabe erstellen" |

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/company-detail.tsx` | Add "Create Task" button |
| `frontend/src/components/contact-detail.tsx` | Add "Create Task" button |
| `frontend/src/components/company-list.tsx` | Add CheckSquare action button |
| `frontend/src/components/contact-list.tsx` | Add CheckSquare action button |
| `frontend/src/components/task-form.tsx` | Read URL params, pre-select and lock entity |
| `frontend/src/lib/i18n/en.ts` | Add createTask translation |
| `frontend/src/lib/i18n/de.ts` | Add createTask translation |
