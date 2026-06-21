# Behaviors: CSV Export

## CSV Button

### CSV button visible in company list

- **Given** the company list view is displayed
- **When** the page renders
- **Then** a CSV export button is visible next to the print button in the header toolbar

### CSV button visible in contact list

- **Given** the contact list view is displayed
- **When** the page renders
- **Then** a CSV export button is visible next to the print button in the header toolbar

### CSV button disabled when no results

- **Given** the company list view is displayed
- **And** the current filter returns 0 results
- **When** the page renders
- **Then** the CSV export button is disabled

### CSV button disabled when no contact results

- **Given** the contact list view is displayed
- **And** the current filter returns 0 results
- **When** the page renders
- **Then** the CSV export button is disabled

## Column Selection Dialog

### Dialog opens on CSV button click

- **Given** the company list has results
- **When** the user clicks the CSV export button
- **Then** a modal dialog opens with the title "CSV Export"
- **And** checkboxes for all company columns are displayed

### All checkboxes checked by default

- **Given** the CSV export dialog is opened for companies
- **When** the dialog renders
- **Then** all column checkboxes are checked

### User can uncheck columns

- **Given** the CSV export dialog is open
- **When** the user unchecks the "email" checkbox
- **Then** the "email" checkbox is unchecked
- **And** all other checkboxes remain checked

### Select All checks all columns

- **Given** the CSV export dialog is open
- **And** some checkboxes are unchecked
- **When** the user clicks "Select All"
- **Then** all column checkboxes are checked

### Deselect All unchecks all columns

- **Given** the CSV export dialog is open
- **And** all checkboxes are checked
- **When** the user clicks "Deselect All"
- **Then** all column checkboxes are unchecked

### Cancel closes dialog without download

- **Given** the CSV export dialog is open
- **When** the user clicks "Cancel"
- **Then** the dialog closes
- **And** no CSV download is triggered

### Column labels translated in dialog

- **Given** the UI language is set to German
- **When** the CSV export dialog is opened for companies
- **Then** the column labels are shown in German (e.g., "Name", "E-Mail", "Webseite", "Straße")

### Column labels translated in dialog (EN)

- **Given** the UI language is set to English
- **When** the CSV export dialog is opened for contacts
- **Then** the column labels are shown in English (e.g., "First Name", "Last Name", "Email", "Position")

## CSV Download — Company

### Full company export with all columns

- **Given** companies exist in the system
- **And** no filters are active
- **When** the user opens the CSV dialog, keeps all columns checked, and clicks "Download"
- **Then** a file `companies.csv` is downloaded
- **And** the first row contains all technical column headers
- **And** subsequent rows contain the data for all companies

### Filtered company export

- **Given** companies exist with various names
- **And** the name filter is set to "Acme"
- **When** the user exports CSV with all columns
- **Then** the downloaded CSV contains only companies matching the "Acme" filter

### Company export with selected columns only

- **Given** companies exist in the system
- **When** the user opens the CSV dialog, selects only "name" and "email", and clicks "Download"
- **Then** the CSV header row contains only `name,email`
- **And** each data row contains only those two fields

### Company export respects archive filter

- **Given** archived (soft-deleted) companies exist
- **And** the "include archived" toggle is active
- **When** the user exports CSV
- **Then** archived companies are included in the CSV

### Company export excludes archived by default

- **Given** archived (soft-deleted) companies exist
- **And** the "include archived" toggle is not active
- **When** the user exports CSV
- **Then** archived companies are not included in the CSV

### Company export respects Brevo filter

- **Given** companies exist with and without Brevo origin
- **And** the Brevo filter is set to "From Brevo"
- **When** the user exports CSV
- **Then** only companies from Brevo are included in the CSV

## CSV Download — Contact

### Full contact export with all columns

- **Given** contacts exist in the system
- **And** no filters are active
- **When** the user opens the CSV dialog, keeps all columns checked, and clicks "Download"
- **Then** a file `contacts.csv` is downloaded
- **And** the first row contains all technical column headers
- **And** subsequent rows contain the data for all contacts

### Filtered contact export by search

- **Given** contacts exist with various names and emails
- **And** the search filter is set to "Schmidt"
- **When** the user exports CSV
- **Then** the downloaded CSV contains only contacts matching the search

### Contact export respects company filter

- **Given** contacts exist associated with different companies
- **And** the company filter is set to a specific company
- **When** the user exports CSV
- **Then** only contacts of that company are included

### Contact export respects language filter

- **Given** contacts exist with different languages
- **And** the language filter is set to "DE"
- **When** the user exports CSV
- **Then** only contacts with language "DE" are included

## CSV Format

### CSV uses comma separator

- **Given** a company with name "Acme Corp" and email "info@acme.com"
- **When** the CSV is exported with columns "name" and "email"
- **Then** the data row reads `Acme Corp,info@acme.com`

### CSV has UTF-8 BOM

- **Given** companies exist
- **When** the CSV is exported
- **Then** the file starts with the UTF-8 BOM byte sequence (EF BB BF)

### CSV header uses technical field names

- **Given** the UI language is set to German
- **When** the CSV is exported for companies with columns name and email
- **Then** the header row reads `name,email` (not "Name,E-Mail")

### Values with commas are quoted

- **Given** a company with name "Schmidt, Müller & Partner GmbH"
- **When** the CSV is exported
- **Then** the name field is quoted: `"Schmidt, Müller & Partner GmbH"`

### Null values are empty

- **Given** a company with email set to null
- **When** the CSV is exported with the email column
- **Then** the email field is an empty string in the CSV row

### Empty result returns header only

- **Given** the backend receives an export request with filters that match no records
- **When** the CSV is generated
- **Then** the response contains only the header row with the selected column names
- **And** there are no data rows

## Backend Endpoint

### Export endpoint returns correct content type

- **Given** companies exist
- **When** `GET /api/companies/export?columns=NAME` is called
- **Then** the response header `Content-Type` is `text/csv; charset=UTF-8`

### Export endpoint returns download disposition

- **Given** companies exist
- **When** `GET /api/companies/export?columns=NAME` is called
- **Then** the response header `Content-Disposition` is `attachment; filename="companies.csv"`

### Contact export endpoint returns correct filename

- **Given** contacts exist
- **When** `GET /api/contacts/export?columns=FIRST_NAME` is called
- **Then** the response header `Content-Disposition` is `attachment; filename="contacts.csv"`
