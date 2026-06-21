# Behaviors: Count Columns for Company and Contact Tables

## Company List Table — Contact Count Column

### Company with contacts shows count

- **Given** a company with 3 associated contacts
- **When** the user views the company list
- **Then** the "Kontakte" / "Contacts" column shows "3" for that company

### Company with no contacts shows zero

- **Given** a company with no associated contacts
- **When** the user views the company list
- **Then** the "Kontakte" / "Contacts" column shows "0"

### Archived company shows contact count

- **Given** an archived company with 2 associated contacts and "include archived" is enabled
- **When** the user views the company list
- **Then** the "Kontakte" / "Contacts" column shows "2" for the archived company

## Company List Table — Comment Count Column

### Company with comments shows count

- **Given** a company with 5 comments
- **When** the user views the company list
- **Then** the "Kommentare" / "Comments" column shows "5"

### Company with no comments shows zero

- **Given** a company with no comments
- **When** the user views the company list
- **Then** the "Kommentare" / "Comments" column shows "0"

## Company List Table — Column Order

### Columns appear in correct order

- **Given** the company list table
- **When** it renders
- **Then** the column order is: Name, Website, Kontakte/Contacts, Kommentare/Comments, Aktionen/Actions

## Contact List Table — Comment Count Column

### Contact with comments shows count

- **Given** a contact with 4 comments
- **When** the user views the contact list
- **Then** the "Kommentare" / "Comments" column shows "4"

### Contact with no comments shows zero

- **Given** a contact with no comments
- **When** the user views the contact list
- **Then** the "Kommentare" / "Comments" column shows "0"

## Contact List Table — Column Order

### Columns appear in correct order

- **Given** the contact list table
- **When** it renders
- **Then** the column order is: Vorname/First Name, Nachname/Last Name, Firma/Company, Kommentare/Comments, Aktionen/Actions

## Company Detail — Contact Count in Show Employees Link

### Show employees link displays contact count

- **Given** a company with 7 contacts
- **When** the user views the company detail page
- **Then** the "Alle Mitarbeiter" / "show employees" link includes "(7)"

### Show employees link displays zero count

- **Given** a company with no contacts
- **When** the user views the company detail page
- **Then** the "Alle Mitarbeiter" / "show employees" link includes "(0)"

## Company Detail — Comment Count in Heading

### Comments heading shows count

- **Given** a company with 3 comments
- **When** the user views the company detail page
- **Then** the comments section heading reads "Kommentare (3)" / "Comments (3)"

### Comments heading shows zero count

- **Given** a company with no comments
- **When** the user views the company detail page
- **Then** the comments section heading reads "Kommentare (0)" / "Comments (0)"

## Contact Detail — Comment Count in Heading

### Comments heading shows count

- **Given** a contact with 2 comments
- **When** the user views the contact detail page
- **Then** the comments section heading reads "Kommentare (2)" / "Comments (2)"

### Comments heading shows zero count

- **Given** a contact with no comments
- **When** the user views the contact detail page
- **Then** the comments section heading reads "Kommentare (0)" / "Comments (0)"

## Count Updates

### Adding a comment updates company comment count

- **Given** a company with 3 comments displayed in the detail view
- **When** the user adds a new comment
- **Then** the comments heading updates to show "(4)"

### Deleting a contact updates company contact count

- **Given** a company with 5 contacts
- **When** a contact associated with this company is deleted
- **Then** the next time the company list or detail is loaded, the contact count shows "4"

## Backend — CompanyDto Counts

### CompanyDto includes contact and comment counts

- **Given** a company with 2 contacts and 4 comments
- **When** the API returns the company via `GET /api/companies/{id}`
- **Then** the response includes `contactCount: 2` and `commentCount: 4`

### New company has zero counts

- **Given** a newly created company
- **When** the API returns the company via `POST /api/companies`
- **Then** the response includes `contactCount: 0` and `commentCount: 0`

### Company list includes counts per entry

- **Given** multiple companies with varying counts
- **When** the API returns companies via `GET /api/companies`
- **Then** each entry in the page includes its own `contactCount` and `commentCount`

## Backend — ContactDto Counts

### ContactDto includes comment count

- **Given** a contact with 3 comments
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `commentCount: 3`

### New contact has zero comment count

- **Given** a newly created contact
- **When** the API returns the contact via `POST /api/contacts`
- **Then** the response includes `commentCount: 0`

## i18n

### German labels for count columns

- **Given** the UI is set to German
- **When** the company list renders
- **Then** the new columns are labeled "Kontakte" and "Kommentare"

### English labels for count columns

- **Given** the UI is set to English
- **When** the company list renders
- **Then** the new columns are labeled "Contacts" and "Comments"

### German label for contact comment column

- **Given** the UI is set to German
- **When** the contact list renders
- **Then** the new column is labeled "Kommentare"

### English label for contact comment column

- **Given** the UI is set to English
- **When** the contact list renders
- **Then** the new column is labeled "Comments"

## Non-sortable Columns

### Count columns have no sort behavior

- **Given** the company or contact list table
- **When** the user views the count columns
- **Then** there is no sort indicator or click-to-sort behavior on the count column headers
