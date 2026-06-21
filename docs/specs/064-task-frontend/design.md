# Design: Task Frontend Views

## GitHub Issue

—

## Summary

The Task backend (Spec 062) provides CRUD REST endpoints for tasks. This spec builds the frontend UI: a task list page with table, a detail view, and create/edit forms. Tasks appear as a new entry in the sidebar navigation. The views follow the established patterns from Company and Contact views.

## Goals

- Task list page with filterable, paginated table
- Task detail view showing all fields with edit/delete actions
- Task create and edit forms with company/contact selector
- Sidebar navigation entry for Tasks
- Full i18n support (DE/EN)

## Non-goals

- Visual overdue highlighting (future spec)
- Print view and CSV export for tasks (future spec)
- Nested task views under company/contact detail pages (future spec)
- Direct status change from list or detail view (only via edit form)

## Technical Approach

### Route Structure

Following the existing Company/Contact pattern:

| Route | Page Component | Description |
|-------|----------------|-------------|
| `/tasks` | `TaskList` | Paginated task table |
| `/tasks/new` | `TaskForm` | Create new task |
| `/tasks/[id]` | `TaskDetail` | Task detail view |
| `/tasks/[id]/edit` | `TaskForm` (with task prop) | Edit existing task |

**Files:**
- `frontend/src/app/tasks/page.tsx`
- `frontend/src/app/tasks/new/page.tsx`
- `frontend/src/app/tasks/[id]/page.tsx`
- `frontend/src/app/tasks/[id]/edit/page.tsx`

### Sidebar Navigation

Add a "Tasks" entry to the sidebar nav items in `sidebar.tsx`:
- Icon: `CheckSquare` from lucide-react
- Position: after Contacts, before Tags
- Label: i18n key `nav.tasks`

### Task List (`task-list.tsx`)

**Table columns:**

| Column | Content | Notes |
|--------|---------|-------|
| Firma/Kontakt | Company name or contact name | Link to entity detail |
| Beschreibung | Action text, truncated | Truncated after ~80 characters with ellipsis |
| Status | Status badge | OPEN, IN_PROGRESS, DONE |
| Enddatum | Due date | Formatted as locale date |

**Filters:**
- Status filter: Select dropdown with options All, Open, In Progress, Done
- Tag filter: Tag Combobox (same as company/contact lists)

**Sorting:** Fixed sort by `dueDate` ascending (earliest first) — no user-changeable sorting.

**Pagination:** Page size selector with localStorage persistence (`pageSize.tasks`), following Spec 059 pattern.

**Row click:** Navigates to task detail view.

**Actions per row:** Edit button (Pencil icon), navigates to edit form.

**Header actions:** "New Task" button navigating to `/tasks/new`.

### Task Detail (`task-detail.tsx`)

**Layout** follows the Company/Contact detail pattern:

- **Header:** Task action as title (h1), with Edit and Delete buttons
- **Fields card:** 2-column grid with DetailField components:
  - Status (displayed as badge)
  - Due date (formatted)
  - Company or Contact (as clickable link to entity detail, with Building2 or User icon)
- **Tags:** Tag chips below the fields card (if tags assigned)
- **Description:** Full action text below the fields (using `whitespace-pre-line` for line breaks)
- **Delete:** Uses existing `DeleteConfirmDialog` component

**Rationale for showing action text both as title and in body:** The title is truncated for long texts. The body section shows the full text with preserved line breaks.

### Task Form (`task-form.tsx`)

**Layout:** Card with `mx-auto max-w-2xl`, same as Company/Contact forms.

**Fields:**

1. **Entity type selector** — Radio/Toggle: "Firma" / "Kontakt"
   - On change: clears the selected entity, shows the matching dropdown
2. **Entity dropdown** — Select with search
   - Shows companies when "Firma" is selected (using `getCompaniesForSelect()`)
   - Shows contacts when "Kontakt" is selected (using a new `getContactsForSelect()`)
   - Required field
3. **Action** — Textarea (required, multi-line)
4. **Due date** — Date picker (required)
5. **Status** — Select dropdown: Open, In Progress, Done (default: Open on create)
6. **Tags** — Tag Combobox (same component as other forms)

**Edit mode differences:**
- Entity type and entity dropdown are **disabled/readonly** (owner cannot be changed, per Spec 062)
- All other fields are editable

**Submit/Cancel buttons** at the bottom, same pattern as other forms.

### API Functions (`api.ts`)

Add to `frontend/src/lib/api.ts`:

```
getTasks(params: TaskListParams): Promise<Page<TaskDto>>
getTask(id: string): Promise<TaskDto>
createTask(data: TaskCreateDto): Promise<TaskDto>
updateTask(id: string, data: TaskUpdateDto): Promise<TaskDto>
deleteTask(id: string): Promise<void>
getContactsForSelect(): Promise<ContactDto[]>
```

**`TaskListParams`:**
- `page`, `size` — pagination
- `status` — optional TaskStatus filter
- `tagIds` — optional tag filter

**`getContactsForSelect()`** loads all contacts (non-paginated, sorted by lastName) for the entity dropdown in the form. Follows the existing `getCompaniesForSelect()` pattern.

### TypeScript Types (`types.ts`)

```typescript
interface TaskDto {
  readonly id: string;
  readonly action: string;
  readonly dueDate: string;
  readonly status: TaskStatus;
  readonly companyId: string | null;
  readonly companyName: string | null;
  readonly contactId: string | null;
  readonly contactName: string | null;
  readonly tagIds: readonly string[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

type TaskStatus = "OPEN" | "IN_PROGRESS" | "DONE";

interface TaskCreateDto {
  readonly action: string;
  readonly dueDate: string;
  readonly status?: TaskStatus;
  readonly companyId?: string | null;
  readonly contactId?: string | null;
  readonly tagIds?: readonly string[];
}

interface TaskUpdateDto {
  readonly action: string;
  readonly dueDate: string;
  readonly status: TaskStatus;
  readonly tagIds?: readonly string[] | null;
}
```

### i18n

New translation keys under `tasks`:

| Key | EN | DE |
|-----|----|----|
| `nav.tasks` | "Tasks" | "Aufgaben" |
| `tasks.title` | "Tasks" | "Aufgaben" |
| `tasks.new` | "New Task" | "Neue Aufgabe" |
| `tasks.edit` | "Edit Task" | "Aufgabe bearbeiten" |
| `tasks.columns.entity` | "Company / Contact" | "Firma / Kontakt" |
| `tasks.columns.action` | "Description" | "Beschreibung" |
| `tasks.columns.status` | "Status" | "Status" |
| `tasks.columns.dueDate` | "Due Date" | "Enddatum" |
| `tasks.fields.action` | "Description" | "Beschreibung" |
| `tasks.fields.dueDate` | "Due Date" | "Enddatum" |
| `tasks.fields.status` | "Status" | "Status" |
| `tasks.fields.entityType` | "Assign to" | "Zuordnen zu" |
| `tasks.fields.company` | "Company" | "Firma" |
| `tasks.fields.contact` | "Contact" | "Kontakt" |
| `tasks.status.OPEN` | "Open" | "Offen" |
| `tasks.status.IN_PROGRESS` | "In Progress" | "In Bearbeitung" |
| `tasks.status.DONE` | "Done" | "Erledigt" |
| `tasks.filter.status` | "Status" | "Status" |
| `tasks.filter.allStatuses` | "All" | "Alle" |
| `tasks.deleteDialog.title` | "Delete Task" | "Aufgabe löschen" |
| `tasks.deleteDialog.description` | "Do you really want to delete this task? This action cannot be undone." | "Möchten Sie diese Aufgabe wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden." |
| `tasks.deleteDialog.confirm` | "Delete" | "Löschen" |
| `tasks.deleteDialog.cancel` | "Cancel" | "Abbrechen" |
| `tasks.pagination.*` | (same pattern as companies/contacts) | |

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/sidebar.tsx` | Add Tasks nav item |
| `frontend/src/components/task-list.tsx` | New: task list component |
| `frontend/src/components/task-detail.tsx` | New: task detail component |
| `frontend/src/components/task-form.tsx` | New: task create/edit form |
| `frontend/src/app/tasks/page.tsx` | New: list route |
| `frontend/src/app/tasks/new/page.tsx` | New: create route |
| `frontend/src/app/tasks/[id]/page.tsx` | New: detail route |
| `frontend/src/app/tasks/[id]/edit/page.tsx` | New: edit route |
| `frontend/src/lib/api.ts` | Add task API functions + getContactsForSelect |
| `frontend/src/lib/types.ts` | Add TaskDto, TaskCreateDto, TaskUpdateDto, TaskStatus |
| `frontend/src/lib/i18n/en.ts` | Add task translations |
| `frontend/src/lib/i18n/de.ts` | Add task translations |

## Security Considerations

- All API calls use `authFetch` with JWT token (same as existing patterns)
- No new authorization model — any authenticated user can CRUD any task
- Action text displayed with `whitespace-pre-line` — no HTML rendering, no XSS risk
