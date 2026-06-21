# Behaviors: Task Frontend Views

## Sidebar Navigation

### Tasks nav item visible

- **Given** a user is logged in
- **When** the sidebar is displayed
- **Then** a "Tasks" / "Aufgaben" nav item is shown with a CheckSquare icon
- **Then** it is positioned after Contacts and before Tags

### Tasks nav item navigates to list

- **Given** the sidebar is displayed
- **When** the user clicks the Tasks nav item
- **Then** the browser navigates to `/tasks`

## Task List

### List displays tasks sorted by due date

- **Given** tasks exist with different due dates
- **When** the user opens the task list
- **Then** tasks are displayed in a table sorted by due date ascending (earliest first)

### List shows correct columns

- **Given** tasks exist
- **When** the task list is displayed
- **Then** the table shows columns: Company/Contact, Description, Status, Due Date

### Description is truncated in list

- **Given** a task has a long action text (more than 80 characters)
- **When** the task list is displayed
- **Then** the description column shows truncated text with ellipsis

### Company name shown for company tasks

- **Given** a task is assigned to a company "Acme Corp"
- **When** the task list is displayed
- **Then** the Company/Contact column shows "Acme Corp"

### Contact name shown for contact tasks

- **Given** a task is assigned to a contact "Dr. Max Müller"
- **When** the task list is displayed
- **Then** the Company/Contact column shows "Dr. Max Müller"

### Row click navigates to detail

- **Given** the task list is displayed
- **When** the user clicks a table row
- **Then** the browser navigates to the task detail view

### Edit button in actions column

- **Given** the task list is displayed
- **When** the user clicks the edit (Pencil) icon on a row
- **Then** the browser navigates to the task edit form

### New Task button

- **Given** the task list is displayed
- **When** the user clicks "New Task" / "Neue Aufgabe"
- **Then** the browser navigates to `/tasks/new`

### Empty list

- **Given** no tasks exist
- **When** the user opens the task list
- **Then** an empty table is shown

## List Filters

### Filter by status

- **Given** tasks exist with status OPEN, IN_PROGRESS, and DONE
- **When** the user selects "Open" from the status filter
- **Then** only tasks with status OPEN are shown

### Filter all statuses

- **Given** the status filter is set to "Open"
- **When** the user selects "All" from the status filter
- **Then** all tasks are shown regardless of status

### Filter by tags

- **Given** tasks exist with different tags
- **When** the user selects a tag in the tag filter
- **Then** only tasks with that tag are shown

### Combined status and tag filter

- **Given** tasks exist with various statuses and tags
- **When** the user selects status "Open" and a specific tag
- **Then** only tasks matching both filters are shown

### Filter change resets to page 1

- **Given** the user is on page 2 of the task list
- **When** the user changes the status filter
- **Then** the page resets to page 1

## List Pagination

### Page size selector

- **Given** the task list is displayed
- **When** the user selects page size 50
- **Then** up to 50 tasks are shown per page
- **Then** the page size is persisted in localStorage as `pageSize.tasks`

### Record count display

- **Given** 42 tasks exist matching the current filters
- **When** the task list is displayed with page size 20
- **Then** the pagination shows "42 Aufgaben · Seite 1 von 3" (or English equivalent)

## Task Detail

### Detail shows all fields

- **Given** a task exists with action, dueDate, status OPEN, assigned to a company, with tags
- **When** the user opens the task detail view
- **Then** the action text is shown as page title
- **Then** the status, due date, company (as link), and tags are displayed

### Company link navigates to company detail

- **Given** a task is assigned to a company
- **When** the user clicks the company name in the detail view
- **Then** the browser navigates to the company detail page

### Contact link navigates to contact detail

- **Given** a task is assigned to a contact
- **When** the user clicks the contact name in the detail view
- **Then** the browser navigates to the contact detail page

### Full action text with line breaks

- **Given** a task has a multi-line action text
- **When** the detail view is displayed
- **Then** the full text is shown with line breaks preserved

### Edit button navigates to edit form

- **Given** the task detail view is displayed
- **When** the user clicks "Edit" / "Bearbeiten"
- **Then** the browser navigates to the task edit form

### Delete with confirmation

- **Given** the task detail view is displayed
- **When** the user clicks "Delete" / "Löschen"
- **Then** a confirmation dialog appears

### Delete confirmed

- **Given** the delete confirmation dialog is open
- **When** the user clicks "Delete" / "Löschen"
- **Then** the task is deleted
- **Then** the user is navigated back to the task list

### Delete cancelled

- **Given** the delete confirmation dialog is open
- **When** the user clicks "Cancel" / "Abbrechen"
- **Then** the dialog closes and the task remains

### Non-existent task shows 404

- **Given** the task ID does not exist
- **When** the user navigates to `/tasks/{id}`
- **Then** a 404 page is shown

## Task Create Form

### Entity type toggle

- **Given** the create form is displayed
- **When** the user selects "Firma" / "Company"
- **Then** a company dropdown appears
- **When** the user switches to "Kontakt" / "Contact"
- **Then** the company dropdown is replaced by a contact dropdown

### Create task for company

- **Given** the user selects "Firma", picks a company, fills in action, due date, and leaves status as OPEN
- **When** the user submits the form
- **Then** the task is created
- **Then** the user is navigated to the task detail view

### Create task for contact

- **Given** the user selects "Kontakt", picks a contact, fills in action and due date
- **When** the user submits the form
- **Then** the task is created with the selected contact

### Create task with tags

- **Given** the user fills in all required fields and selects tags
- **When** the user submits the form
- **Then** the task is created with the selected tags

### Create task with explicit status

- **Given** the user sets status to "In Progress" / "In Bearbeitung"
- **When** the user submits the form
- **Then** the task is created with status IN_PROGRESS

### Submit without entity fails

- **Given** the user fills in action and due date but does not select a company or contact
- **When** the user tries to submit
- **Then** validation prevents submission

### Submit without action fails

- **Given** the user leaves the action field empty
- **When** the user tries to submit
- **Then** validation prevents submission

### Submit without due date fails

- **Given** the user leaves the due date empty
- **When** the user tries to submit
- **Then** validation prevents submission

### Cancel navigates back

- **Given** the create form is displayed
- **When** the user clicks "Cancel" / "Abbrechen"
- **Then** the user is navigated back to the task list

## Task Edit Form

### Form pre-filled with existing data

- **Given** a task exists with action "Call client", dueDate "2026-04-15", status OPEN, assigned to company "Acme"
- **When** the user opens the edit form
- **Then** all fields are pre-filled with the existing values

### Entity assignment is readonly

- **Given** the edit form is displayed for a task assigned to a company
- **When** the form is rendered
- **Then** the entity type toggle and entity dropdown are disabled/readonly
- **Then** the assigned company is shown but not changeable

### Edit task fields

- **Given** the edit form is displayed
- **When** the user changes the action, due date, and status and submits
- **Then** the task is updated
- **Then** the user is navigated to the task detail view

### Edit task tags

- **Given** a task has tags assigned
- **When** the user adds or removes tags in the edit form and submits
- **Then** the tags are updated

## i18n

### German translations

- **Given** the language is set to German
- **When** the user views any task page
- **Then** all labels, buttons, and messages are in German

### English translations

- **Given** the language is set to English
- **When** the user views any task page
- **Then** all labels, buttons, and messages are in English
