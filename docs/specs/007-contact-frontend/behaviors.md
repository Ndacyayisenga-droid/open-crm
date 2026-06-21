# Behaviors: Contact Frontend

## Contact List

### Displays contacts on page load

- **Given** contacts exist in the system
- **When** the user navigates to `/contacts`
- **Then** a table shows contacts with columns: First name, Last name, Company
- **And** results are paginated (default 20 per page, sorted by last name ascending)

### Shows empty state when no contacts exist

- **Given** no contacts exist in the system
- **When** the user navigates to `/contacts`
- **Then** a message "Keine Kontakte vorhanden" is displayed
- **And** a button to create a new contact is shown

### Shows empty state for filtered results with no matches

- **Given** contacts exist but none match the active filters
- **When** the user applies filters that match no contacts
- **Then** the same empty state message "Keine Kontakte vorhanden" is displayed

### Filters by first name

- **Given** contacts exist with various first names
- **When** the user enters a partial first name in the first name filter
- **Then** only contacts whose first name contains the entered text (case-insensitive) are shown

### Filters by last name

- **Given** contacts exist with various last names
- **When** the user enters a partial last name in the last name filter
- **Then** only contacts whose last name contains the entered text (case-insensitive) are shown

### Filters by email

- **Given** contacts exist with various email addresses
- **When** the user enters a partial email in the email filter
- **Then** only contacts whose email contains the entered text (case-insensitive) are shown

### Filters by company

- **Given** contacts exist linked to different companies
- **When** the user selects a company from the company filter dropdown
- **Then** only contacts associated with the selected company are shown

### Filters by language

- **Given** contacts exist with language DE and EN
- **When** the user selects "DE" from the language filter
- **Then** only contacts with language DE are shown

### Sorts contacts

- **Given** contacts exist
- **When** the user selects a sort option (e.g., "Nachname (Z-A)")
- **Then** the contact list is re-sorted accordingly

### Paginates results

- **Given** more than 20 contacts exist
- **When** the user views the contact list
- **Then** only the first page (20 contacts) is shown with pagination controls
- **And** clicking "Next" loads the next page

### Shows company name in list

- **Given** a contact is associated with a company
- **When** the contact list is displayed
- **Then** the company name is shown in the Company column

### Shows empty company column for unassociated contacts

- **Given** a contact has no company association
- **When** the contact list is displayed
- **Then** the Company column for that contact is empty

### Navigates to detail on row click

- **Given** the contact list is displayed
- **When** the user clicks on a contact row
- **Then** the user is navigated to `/contacts/{id}`

## Contact Detail

### Displays all contact fields

- **Given** a contact exists with all fields populated
- **When** the user navigates to `/contacts/{id}`
- **Then** all fields are displayed: first name, last name, email, position, gender, phone number, LinkedIn URL, language, company name

### Displays Brevo fields as read-only checkboxes

- **Given** a contact exists with `syncedToBrevo: true` and `doubleOptIn: false`
- **When** the user views the contact detail
- **Then** "Synced to Brevo" is shown as a checked, disabled checkbox
- **And** "Double Opt-In" is shown as an unchecked, disabled checkbox

### Shows archived badge for soft-deleted company

- **Given** a contact is associated with a company that has been soft-deleted
- **When** the user views the contact detail
- **Then** the company name is displayed with a visible "Archived" indicator

### Handles missing optional fields gracefully

- **Given** a contact exists with only required fields (firstName, lastName, language)
- **When** the user views the contact detail
- **Then** optional fields (email, position, gender, phone, LinkedIn, company) are either hidden or show a dash

### Shows comments placeholder

- **Given** the user views a contact detail page
- **When** the page loads
- **Then** a "Comments" section is shown with a disabled "Add comment" button

### Navigates to edit page

- **Given** the user views a contact detail
- **When** the user clicks the "Edit" button
- **Then** the user is navigated to `/contacts/{id}/edit`

### Shows 404 for non-existent contact

- **Given** no contact exists with a given ID
- **When** the user navigates to `/contacts/{id}`
- **Then** a 404 page is shown

## Contact Create

### Creates a contact with all fields

- **Given** the user is on the create contact page `/contacts/new`
- **When** the user fills in all fields including company and gender and submits
- **Then** the contact is created via `POST /api/contacts`
- **And** the user is navigated to the new contact's detail page

### Creates a contact with only required fields

- **Given** the user is on the create contact page
- **When** the user fills in only firstName, lastName, and language and submits
- **Then** the contact is created successfully with null values for optional fields

### Validates required fields

- **Given** the user is on the create contact page
- **When** the user submits without filling in firstName or lastName
- **Then** a validation error is shown for the missing fields
- **And** the form is not submitted

### Validates language is selected

- **Given** the user is on the create contact page
- **When** the user fills in name but does not select a language
- **Then** a validation error is shown for the language field

### Company dropdown shows only active companies

- **Given** active and soft-deleted companies exist
- **When** the user opens the company dropdown in the create form
- **Then** only active (non-deleted) companies are listed

### Company dropdown allows empty selection

- **Given** the user is on the create contact page
- **When** the user views the company dropdown
- **Then** a "No company" or empty option is available and selected by default

### Shows server-side validation errors

- **Given** the user is on the create contact page
- **When** the user submits data that fails backend validation (e.g., companyId of a deleted company)
- **Then** the error message from the server is displayed below the form

### Cancel navigates to contact list

- **Given** the user is on the create contact page
- **When** the user clicks "Cancel"
- **Then** the user is navigated to `/contacts`

## Contact Edit

### Pre-fills form with existing data

- **Given** a contact exists with all fields populated
- **When** the user navigates to `/contacts/{id}/edit`
- **Then** all form fields are pre-filled with the contact's current values
- **And** the correct company is selected in the dropdown

### Updates contact successfully

- **Given** the user is on the edit page for an existing contact
- **When** the user changes fields and submits
- **Then** the contact is updated via `PUT /api/contacts/{id}`
- **And** the user is navigated to the contact's detail page

### Validates required fields on edit

- **Given** the user is on the edit page
- **When** the user clears the firstName field and submits
- **Then** a validation error is shown and the form is not submitted

### Cancel navigates to detail page

- **Given** the user is on the edit page for contact with ID `{id}`
- **When** the user clicks "Cancel"
- **Then** the user is navigated to `/contacts/{id}`

### Shows 404 for non-existent contact

- **Given** no contact exists with a given ID
- **When** the user navigates to `/contacts/{id}/edit`
- **Then** a 404 page is shown

## Contact Delete

### Shows permanent deletion warning

- **Given** the user is viewing a contact (list or detail)
- **When** the user clicks the delete button
- **Then** a confirmation dialog appears warning that the deletion is permanent and cannot be undone

### Warns about comment loss

- **Given** the user clicks delete on a contact
- **When** the confirmation dialog appears
- **Then** the dialog explicitly states that all associated comments will also be deleted

### Deletes contact on confirmation

- **Given** the delete confirmation dialog is shown
- **When** the user confirms the deletion
- **Then** the contact is deleted via `DELETE /api/contacts/{id}`
- **And** the user is navigated to the contact list (if on detail) or the list refreshes (if on list)

### Cancels deletion

- **Given** the delete confirmation dialog is shown
- **When** the user clicks "Cancel"
- **Then** the dialog closes and the contact is not deleted

## Sidebar Navigation

### Shows contacts entry in sidebar

- **Given** any page in the application
- **When** the sidebar is visible
- **Then** a "Kontakte" / "Contacts" navigation entry is shown below "Firmen" / "Companies"

### Highlights active contacts page

- **Given** the user is on any `/contacts` route
- **When** the sidebar is visible
- **Then** the contacts navigation entry is visually highlighted as active

## Internationalization

### Displays German strings by default

- **Given** the user's language preference is German
- **When** any contact view is displayed
- **Then** all labels, buttons, and messages are shown in German

### Displays English strings when selected

- **Given** the user switches the language to English
- **When** any contact view is displayed
- **Then** all labels, buttons, and messages are shown in English
