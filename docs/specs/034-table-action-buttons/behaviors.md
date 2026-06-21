# Behaviors: Add Comment and Edit Actions to Company and Contact List Tables

## Company List — Edit Button

### Edit button navigates to edit page

- **Given** the company list shows company "Acme" with id "abc-123"
- **When** the user clicks the edit button on the "Acme" row
- **Then** the user is navigated to `/companies/abc-123/edit`
- **And** the row click (navigate to detail) is not triggered

### Edit button is shown for archived companies

- **Given** company "OldCorp" is archived (soft-deleted)
- **When** the company list shows "OldCorp" with archive toggle enabled
- **Then** the edit button is visible on the "OldCorp" row

## Company List — Comment Button

### Comment button opens modal dialog

- **Given** the company list shows company "Acme"
- **When** the user clicks the comment button on the "Acme" row
- **Then** the comment modal dialog opens
- **And** the row click (navigate to detail) is not triggered

### Comment is created via modal

- **Given** the comment modal is open for company "Acme"
- **When** the user types "Follow up next week" and clicks send
- **Then** the comment is created for company "Acme"
- **And** the modal closes

### Comment modal shows error on failure

- **Given** the comment modal is open for company "Acme"
- **When** the user submits a comment and the API call fails
- **Then** an error message is shown in the modal
- **And** the modal remains open

### Comment button is shown for archived companies

- **Given** company "OldCorp" is archived
- **When** the company list shows "OldCorp"
- **Then** the comment button is visible on the "OldCorp" row

## Company List — Button Order

### Buttons are in correct order for active companies

- **Given** the company list shows an active company
- **When** the actions column renders
- **Then** the buttons appear left to right: Edit, Comment, Delete

### Buttons are in correct order for archived companies

- **Given** the company list shows an archived company
- **When** the actions column renders
- **Then** the buttons appear left to right: Edit, Comment, Restore

## Contact List — Edit Button

### Edit button navigates to edit page

- **Given** the contact list shows contact "Anna Schmidt" with id "def-456"
- **When** the user clicks the edit button on the "Anna Schmidt" row
- **Then** the user is navigated to `/contacts/def-456/edit`
- **And** the row click (navigate to detail) is not triggered

## Contact List — Comment Button

### Comment button opens modal dialog

- **Given** the contact list shows contact "Anna Schmidt"
- **When** the user clicks the comment button on the "Anna Schmidt" row
- **Then** the comment modal dialog opens

### Comment is created via modal for contact

- **Given** the comment modal is open for contact "Anna Schmidt"
- **When** the user types "Called about project" and clicks send
- **Then** the comment is created for contact "Anna Schmidt"
- **And** the modal closes

## Contact List — Button Order

### Buttons are in correct order for contacts

- **Given** the contact list shows a contact
- **When** the actions column renders
- **Then** the buttons appear left to right: Edit, Comment, Delete

## Comment Count

### Comment count is not immediately updated

- **Given** company "Acme" shows "3" in the comments column
- **When** the user adds a comment via the modal
- **Then** the comments column still shows "3"
- **And** after refreshing/navigating back, it shows "4"

## Click Propagation

### Action buttons do not trigger row navigation

- **Given** the company list shows company "Acme"
- **When** the user clicks any action button (edit, comment, delete)
- **Then** the user is NOT navigated to the company detail view
- **And** only the button's specific action is executed

### Row click still navigates to detail

- **Given** the company list shows company "Acme"
- **When** the user clicks on the row (not on an action button)
- **Then** the user is navigated to the company detail view
