# Behaviors: Create Task from Detail View

## Company Detail View

### Create Task button visible

- **Given** the user is on a company detail view
- **When** the page is displayed
- **Then** a "Create Task" button with a CheckSquare icon is shown in the header actions

### Button navigates to pre-filled form

- **Given** the user is on the detail view of company "Acme Corp" (id: abc-123)
- **When** the user clicks "Create Task"
- **Then** the browser navigates to `/tasks/new?companyId=abc-123`

## Contact Detail View

### Create Task button visible

- **Given** the user is on a contact detail view
- **When** the page is displayed
- **Then** a "Create Task" button with a CheckSquare icon is shown in the header actions

### Button navigates to pre-filled form

- **Given** the user is on the detail view of contact "Dr. Max Müller" (id: def-456)
- **When** the user clicks "Create Task"
- **Then** the browser navigates to `/tasks/new?contactId=def-456`

## Company List Table

### Create Task action button in table row

- **Given** the company list is displayed
- **When** the user looks at a table row
- **Then** a CheckSquare action button is visible alongside Edit and Comment buttons

### Action button navigates to pre-filled form

- **Given** the company list is displayed
- **When** the user clicks the CheckSquare button on a company row
- **Then** the browser navigates to `/tasks/new?companyId=<companyId>`

## Contact List Table

### Create Task action button in table row

- **Given** the contact list is displayed
- **When** the user looks at a table row
- **Then** a CheckSquare action button is visible alongside Edit and Comment buttons

### Action button navigates to pre-filled form

- **Given** the contact list is displayed
- **When** the user clicks the CheckSquare button on a contact row
- **Then** the browser navigates to `/tasks/new?contactId=<contactId>`

## Task Form with companyId Parameter

### Entity pre-selected for company

- **Given** the user navigates to `/tasks/new?companyId=abc-123`
- **When** the form loads
- **Then** the entity type toggle is set to "Company" and disabled
- **Then** the company dropdown shows the matching company and is disabled

### Only task fields are editable

- **Given** the form loaded with `companyId` parameter
- **When** the form is displayed
- **Then** action, due date, status, and tags are editable
- **Then** entity type toggle and entity dropdown are not editable

### Submit creates task for pre-selected company

- **Given** the form is pre-filled with companyId
- **When** the user fills in action, due date and submits
- **Then** the task is created with the pre-selected company
- **Then** the user is navigated to the task detail view

## Task Form with contactId Parameter

### Entity pre-selected for contact

- **Given** the user navigates to `/tasks/new?contactId=def-456`
- **When** the form loads
- **Then** the entity type toggle is set to "Contact" and disabled
- **Then** the contact dropdown shows the matching contact and is disabled

### Submit creates task for pre-selected contact

- **Given** the form is pre-filled with contactId
- **When** the user fills in action, due date and submits
- **Then** the task is created with the pre-selected contact
- **Then** the user is navigated to the task detail view

## Task Form without Parameters

### Form works normally without URL params

- **Given** the user navigates to `/tasks/new` (no query parameters)
- **When** the form loads
- **Then** the entity type toggle and entity dropdown are editable (default behavior from Spec 064)

## Edge Cases

### Invalid companyId parameter

- **Given** the user navigates to `/tasks/new?companyId=invalid-uuid`
- **When** the form loads
- **Then** the company is not found in the dropdown
- **Then** the user can still select a different entity manually (form falls back to unlocked state)

### Invalid contactId parameter

- **Given** the user navigates to `/tasks/new?contactId=invalid-uuid`
- **When** the form loads
- **Then** the contact is not found in the dropdown
- **Then** the user can still select a different entity manually (form falls back to unlocked state)

### Both parameters provided

- **Given** the user navigates to `/tasks/new?companyId=abc&contactId=def`
- **When** the form loads
- **Then** companyId takes precedence (first parameter wins)
- **Then** the entity type is set to "Company"
