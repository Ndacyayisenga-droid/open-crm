# Behaviors: Contact–Company Cross-Navigation

## Contact Detail — Company Link

### Active company is shown as a clickable link

- **Given** a contact associated with a company that is not archived
- **When** the user views the contact detail page
- **Then** the company name is displayed as a clickable link with the label "zur Firma" (DE) / "show company" (EN)

### Clicking the company link navigates to the company detail

- **Given** a contact detail page showing a linked company name
- **When** the user clicks the company link
- **Then** the browser navigates to `/companies/{companyId}`

### Archived company is shown as static text with badge

- **Given** a contact associated with a company that is archived (soft-deleted)
- **When** the user views the contact detail page
- **Then** the company name is displayed as static text (not a link) with an "Archiviert" / "Archived" badge

### Contact without company shows dash

- **Given** a contact that is not associated with any company
- **When** the user views the contact detail page
- **Then** the company field displays "—" with no link

## Company Detail — Show Employees Link

### Show employees link is visible for active companies

- **Given** an active (non-archived) company
- **When** the user views the company detail page
- **Then** a "Alle Mitarbeiter" / "show employees" link with a Users icon is visible in the header actions area

### Clicking show employees navigates to filtered contact list

- **Given** a company detail page with the "show employees" link
- **When** the user clicks the link
- **Then** the browser navigates to `/contacts?companyId={companyId}`
- **And** the contact list displays only contacts associated with that company

### Show employees link is disabled for archived companies

- **Given** an archived (soft-deleted) company
- **When** the user views the company detail page
- **Then** the "show employees" link is visible but visually disabled (grayed out)
- **And** clicking it does not navigate anywhere

### Show employees link is visible even with zero contacts

- **Given** a company with no associated contacts
- **When** the user views the company detail page
- **Then** the "show employees" link is still visible and clickable
- **And** clicking it navigates to the contact list which shows an empty state

## Contact List — URL Parameter Filtering

### Contact list filters by companyId from URL

- **Given** the URL `/contacts?companyId={id}` where `{id}` is a valid company ID
- **When** the contact list page loads
- **Then** only contacts associated with that company are displayed

### Contact list shows all contacts when no companyId in URL

- **Given** the URL `/contacts` without a `companyId` parameter
- **When** the contact list page loads
- **Then** all contacts are displayed (default behavior, unchanged)

### Contact list handles invalid companyId gracefully

- **Given** the URL `/contacts?companyId={invalidId}` where `{invalidId}` does not match any company
- **When** the contact list page loads
- **Then** an empty contact list is displayed (no error)

## Backend — ContactDto

### ContactDto includes companyDeleted field

- **Given** a contact associated with an active company
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `companyDeleted: false`

### ContactDto reflects archived company status

- **Given** a contact associated with an archived company
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `companyDeleted: true`

### ContactDto defaults companyDeleted for contacts without company

- **Given** a contact not associated with any company
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `companyDeleted: false`

## i18n

### Labels switch with language toggle

- **Given** the UI is set to German
- **When** the user views a contact with an active company
- **Then** the company link label reads "zur Firma"
- **And** the company detail "show employees" link reads "Alle Mitarbeiter"

### Labels display correctly in English

- **Given** the UI is set to English
- **When** the user views a contact with an active company
- **Then** the company link label reads "show company"
- **And** the company detail "show employees" link reads "show employees"
