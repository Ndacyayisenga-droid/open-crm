# Behaviors: Remove Company Archive, Introduce Hard Delete

## Company Hard Delete — Keep Contacts

### Delete company without contacts

- **Given** a company with no associated contacts, comments, tasks, or tags
- **When** the user deletes the company and chooses "Delete company only"
- **Then** the company is permanently removed from the database
- **And** the API returns 204 No Content

### Delete company with contacts — keep contacts

- **Given** a company with 3 associated contacts
- **When** the user deletes the company and chooses "Delete company only"
- **Then** the company is permanently removed
- **And** all 3 contacts still exist with their `companyId` set to `null`
- **And** the contacts' own tasks and comments are unaffected

### Delete company with comments — comments cascade

- **Given** a company with 2 comments
- **When** the user deletes the company (either option)
- **Then** the company is permanently removed
- **And** both company comments are permanently removed

### Delete company with tasks — tasks cascade

- **Given** a company with 3 tasks (various statuses)
- **When** the user deletes the company (either option)
- **Then** the company is permanently removed
- **And** all 3 company tasks are permanently removed
- **And** any tag associations on those tasks are removed

### Delete company with tags — tag associations removed

- **Given** a company associated with 2 tags
- **When** the user deletes the company (either option)
- **Then** the company is permanently removed
- **And** the company-tag join entries are removed
- **And** the tags themselves still exist

### Delete company with logo — logo removed

- **Given** a company with an uploaded logo
- **When** the user deletes the company (either option)
- **Then** the company and its logo data are permanently removed

## Company Hard Delete — Delete Contacts Too

### Delete company and all contacts

- **Given** a company with 2 associated contacts
- **When** the user deletes the company and chooses "Delete company and all contacts"
- **Then** the company is permanently removed
- **And** both contacts are permanently removed

### Contact cascade — contact tasks and comments deleted

- **Given** a company with 1 contact that has 2 tasks and 3 comments
- **When** the user deletes the company and chooses "Delete company and all contacts"
- **Then** the company, the contact, the contact's 2 tasks, and the contact's 3 comments are all permanently removed

### Contact cascade — contact tags unlinked

- **Given** a company with 1 contact that has tag associations
- **When** the user deletes the company and chooses "Delete company and all contacts"
- **Then** the contact-tag join entries are removed
- **And** the tags themselves still exist

## Delete Dialog UI

### Dialog always shown

- **Given** any company (with or without contacts)
- **When** the user clicks the delete button (in list or detail view)
- **Then** a confirmation dialog is shown with two options: "Delete company and all contacts" and "Delete company only"
- **And** a Cancel button

### Dialog from company list

- **Given** the company list view
- **When** the user clicks the delete action button on a table row
- **Then** the company delete dialog is shown

### Dialog from company detail

- **Given** the company detail view
- **When** the user clicks the delete button
- **Then** the company delete dialog is shown

### Successful deletion navigates back to list

- **Given** the company detail view
- **When** the user confirms deletion in the dialog
- **Then** the company is deleted
- **And** the user is navigated to the company list

### Successful deletion refreshes list

- **Given** the company list view
- **When** the user confirms deletion in the dialog
- **Then** the company is deleted
- **And** the list is refreshed without the deleted company

## Brevo Companies

### Brevo company can be deleted

- **Given** a company synced from Brevo (has a `brevoCompanyId`)
- **When** the user deletes the company
- **Then** the company is permanently removed (no blocking)

### Brevo company recreated on next sync

- **Given** a Brevo company was deleted from the CRM
- **When** the next Brevo sync runs
- **Then** the company is recreated as a new entry with a new internal ID

## Archive Feature Removal

### No archive toggle in company list

- **Given** the company list view
- **When** the page loads
- **Then** there is no "Show Archived" / "Hide Archived" toggle button

### No restore button

- **Given** any company view
- **When** the page loads
- **Then** there is no restore button anywhere

### No includeDeleted API parameter

- **Given** the company list API endpoint
- **When** a request is made with `includeDeleted=true`
- **Then** the parameter is ignored (no filtering by deleted status exists)

### No archived badge on contact detail

- **Given** a contact whose company was previously archived
- **When** the contact detail view loads
- **Then** no "Archived" badge is shown for the company

## API

### Delete with deleteContacts=false (default)

- **Given** a company exists with ID `{id}`
- **When** `DELETE /api/companies/{id}` is called without the `deleteContacts` parameter
- **Then** the company is hard-deleted
- **And** associated contacts have their `companyId` set to null
- **And** the response is 204 No Content

### Delete with deleteContacts=true

- **Given** a company exists with ID `{id}` and has associated contacts
- **When** `DELETE /api/companies/{id}?deleteContacts=true` is called
- **Then** the company and all associated contacts (with their tasks and comments) are hard-deleted
- **And** the response is 204 No Content

### Delete nonexistent company

- **Given** no company exists with ID `{id}`
- **When** `DELETE /api/companies/{id}` is called
- **Then** the response is 404 Not Found

## Contact Delete Dialog Fix

### Contact delete dialog mentions tasks

- **Given** a contact with associated tasks and comments
- **When** the user clicks delete on the contact
- **Then** the confirmation dialog mentions that both comments and tasks will be deleted

## Error Cases

### Delete company — server error

- **Given** a company exists but a database error occurs during deletion
- **When** the user confirms deletion
- **Then** an error message is shown
- **And** no partial deletion has occurred (transaction rollback)
