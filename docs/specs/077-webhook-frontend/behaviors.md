# Behaviors: Webhook Frontend View

## Navigation

### Webhook page is accessible via sidebar

- **Given** the user is authenticated
- **When** they click "Webhooks" in the sidebar
- **Then** they are navigated to `/webhooks`
- **And** the sidebar item shows as active

### Webhooks sidebar item uses Webhook icon

- **Given** the sidebar is visible
- **When** the user looks at the navigation
- **Then** the Webhooks entry shows the Lucide `Webhook` icon
- **And** it appears below the Admin entry

## List View — Table

### Table shows all webhook columns

- **Given** webhooks exist in the database
- **When** the user visits `/webhooks`
- **Then** the table shows columns: URL, Active, Last Status, Last Called, Actions

### Table shows webhook data correctly

- **Given** a webhook exists with URL `https://example.com/hook`, active `true`, lastStatus `200`, lastCalledAt `2026-04-04T15:00:00Z`
- **When** the user visits `/webhooks`
- **Then** the row shows the URL, an active indicator, "OK" as status, and a formatted timestamp

### Last Status displays OK for 2xx

- **Given** a webhook has `lastStatus: 200`
- **When** the table renders
- **Then** the status column shows "OK"

### Last Status displays Timeout for -1

- **Given** a webhook has `lastStatus: -1`
- **When** the table renders
- **Then** the status column shows "Timeout"

### Last Status displays Connection Error for 0

- **Given** a webhook has `lastStatus: 0`
- **When** the table renders
- **Then** the status column shows "Connection Error"

### Last Status displays Bad Call with code for 4xx/5xx

- **Given** a webhook has `lastStatus: 404`
- **When** the table renders
- **Then** the status column shows "Bad Call (404)"

### Last Status displays dash for never called

- **Given** a webhook has `lastStatus: null`
- **When** the table renders
- **Then** the status column shows "—"

### Table is paginated

- **Given** 25 webhooks exist
- **When** the user visits `/webhooks` with default page size 20
- **Then** 20 webhooks are shown with pagination controls

### Page size is persisted to localStorage

- **Given** the user changes page size to 50
- **When** they reload the page
- **Then** the page size is still 50

### Loading state shows skeletons

- **Given** the webhook data is being fetched
- **When** the table is loading
- **Then** skeleton placeholders are shown instead of table rows

### Empty state with create button

- **Given** no webhooks exist
- **When** the user visits `/webhooks`
- **Then** an empty state message is shown with a button to create the first webhook

## Create Webhook

### Create dialog opens from header button

- **Given** the user is on `/webhooks`
- **When** they click the "New Webhook" button
- **Then** a dialog opens with a URL input field

### Create webhook with valid URL

- **Given** the create dialog is open
- **When** the user enters `https://example.com/hook` and clicks Create
- **Then** the webhook is created via `POST /api/webhooks`
- **And** the dialog closes
- **And** the table refreshes and shows the new webhook

### Create webhook with empty URL shows validation

- **Given** the create dialog is open
- **When** the user clicks Create without entering a URL
- **Then** the form shows a validation error

### Create dialog can be cancelled

- **Given** the create dialog is open
- **When** the user clicks Cancel
- **Then** the dialog closes without creating a webhook

### Create dialog shows error on API failure

- **Given** the create dialog is open
- **When** the API returns an error
- **Then** the error is displayed in the dialog

## Toggle Active/Inactive

### Toggle active to inactive

- **Given** a webhook with `active: true` exists in the table
- **When** the user clicks the toggle/deactivate action
- **Then** `PUT /api/webhooks/{id}` is called with `active: false`
- **And** the table row updates to show inactive status

### Toggle inactive to active

- **Given** a webhook with `active: false` exists in the table
- **When** the user clicks the toggle/activate action
- **Then** `PUT /api/webhooks/{id}` is called with `active: true`
- **And** the table row updates to show active status

## PING

### PING fires on click without confirmation

- **Given** a webhook exists in the table
- **When** the user clicks the PING button
- **Then** `POST /api/webhooks/{id}/ping` is called immediately
- **And** no confirmation dialog is shown

### PING works for inactive webhooks

- **Given** an inactive webhook exists in the table
- **When** the user clicks the PING button
- **Then** the PING is sent (no restriction on inactive webhooks)

### PING result visible after manual refresh

- **Given** a PING has been sent to a webhook
- **When** the user reloads the page
- **Then** the webhook's Last Status and Last Called columns reflect the PING result

## Delete Webhook

### Delete shows confirmation dialog

- **Given** a webhook exists in the table
- **When** the user clicks the Delete button
- **Then** a confirmation dialog appears asking to confirm deletion

### Delete removes webhook after confirmation

- **Given** the delete confirmation dialog is open
- **When** the user clicks Confirm
- **Then** `DELETE /api/webhooks/{id}` is called
- **And** the dialog closes
- **And** the table refreshes without the deleted webhook

### Delete can be cancelled

- **Given** the delete confirmation dialog is open
- **When** the user clicks Cancel
- **Then** the dialog closes without deleting the webhook

### Delete shows error on API failure

- **Given** the delete confirmation dialog is open
- **When** the API returns an error
- **Then** the error is displayed in the dialog

## i18n

### Page displays in German

- **Given** the language is set to German
- **When** the user visits `/webhooks`
- **Then** all labels, buttons, and messages are in German

### Page displays in English

- **Given** the language is set to English
- **When** the user visits `/webhooks`
- **Then** all labels, buttons, and messages are in English

## Responsive

### Status columns hidden on mobile

- **Given** the user is on a mobile viewport
- **When** viewing the webhook table
- **Then** the Last Status and Last Called columns are hidden
- **And** URL, Active, and Actions columns remain visible
