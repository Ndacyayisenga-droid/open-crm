# Behaviors: Company Frontend

## Navigation

### Sidebar shows navigation entries

- **Given** the user opens the application
- **When** any page loads
- **Then** a sidebar is visible on the left with entries "Server-Health" and "Firmen"

### Sidebar navigates to pages

- **Given** the sidebar is visible
- **When** the user clicks "Firmen"
- **Then** the user is navigated to `/companies`

### Sidebar collapses to hamburger on mobile

- **Given** the viewport is below the `md` breakpoint
- **When** any page loads
- **Then** the sidebar is hidden and a hamburger menu button is visible
- **And** clicking the hamburger button opens the sidebar as an overlay

### Root page redirects to companies

- **Given** no specific page is requested
- **When** the user navigates to `/`
- **Then** the user is redirected to `/companies`

## Company List

### List displays companies with name and website

- **Given** companies "Open Elements" (website: open-elements.com) and "Acme Corp" (website: acme.com) exist
- **When** the user opens `/companies`
- **Then** a table shows both companies with Name and Website columns

### List shows empty state when no companies exist

- **Given** no companies exist
- **When** the user opens `/companies`
- **Then** the text "Keine Firmen vorhanden. Erstellen Sie die erste Firma." is displayed
- **And** a "Neue Firma" button is shown

### List shows pagination controls

- **Given** more than 20 companies exist
- **When** the user opens `/companies`
- **Then** pagination controls are visible at the bottom
- **And** only 20 companies are shown on the first page

### List navigates pages via pagination

- **Given** 25 companies exist and the user is on page 1
- **When** the user clicks the next page button
- **Then** the remaining 5 companies are shown

### List filters by name

- **Given** companies "Open Elements" and "Acme Corp" exist
- **When** the user types "open" in the name filter
- **Then** only "Open Elements" is shown in the list

### List filters by city

- **Given** companies in "Berlin" and "Munich" exist
- **When** the user enters "Berlin" in the city filter
- **Then** only the Berlin company is shown

### List filters by country

- **Given** companies in "Germany" and "Austria" exist
- **When** the user enters "Germany" in the country filter
- **Then** only the German company is shown

### List sorts companies

- **Given** companies "Zebra Inc" and "Alpha GmbH" exist
- **When** the user changes the sort order
- **Then** the companies are reordered accordingly

### List excludes soft-deleted companies by default

- **Given** 3 companies exist, 1 is soft-deleted
- **When** the user opens `/companies`
- **Then** only 2 companies are shown

### List shows archived companies when toggled

- **Given** 3 companies exist, 1 is soft-deleted
- **When** the user clicks "Archivierte Firmen anzeigen"
- **Then** all 3 companies are shown
- **And** the archived company is visually distinct (muted/grayed out)

### List shows restore button for archived companies

- **Given** the archived view is active and a soft-deleted company is shown
- **When** the user views the list
- **Then** a "Wiederherstellen" button is shown for the archived company instead of the delete button

### Clicking a company row navigates to detail

- **Given** the company list is displayed
- **When** the user clicks on a company row
- **Then** the user is navigated to `/companies/{id}`

### List shows "Neue Firma" button

- **Given** the company list is displayed
- **When** the user views the page
- **Then** a "Neue Firma" button is visible
- **And** clicking it navigates to `/companies/new`

## Company Detail

### Detail page shows all company fields

- **Given** a company with name, email, website, and full address exists
- **When** the user opens `/companies/{id}`
- **Then** all fields (Name, E-Mail, Website, Straße, Hausnummer, PLZ, Stadt, Land) are displayed

### Detail page shows edit button

- **Given** a company detail page is displayed
- **When** the user views the page
- **Then** a "Bearbeiten" button is visible
- **And** clicking it navigates to `/companies/{id}/edit`

### Detail page shows delete button

- **Given** a company detail page is displayed
- **When** the user views the page
- **Then** a "Löschen" button is visible

### Detail page shows comment placeholder

- **Given** a company detail page is displayed
- **When** the user views the page
- **Then** a "Kommentare" section is visible
- **And** the text "Keine Kommentare vorhanden" is shown
- **And** a disabled "Kommentar hinzufügen" button is visible

### Detail page shows 404 for non-existent company

- **Given** no company with the given ID exists
- **When** the user opens `/companies/{id}`
- **Then** a "not found" message is displayed

## Company Create

### Create page shows form with all fields

- **Given** the user navigates to `/companies/new`
- **When** the page loads
- **Then** a form is shown with fields: Name, E-Mail, Website, Straße, Hausnummer, PLZ, Stadt, Land
- **And** a "Speichern" button and an "Abbrechen" button are visible

### Create form validates name is required

- **Given** the user is on the create page
- **When** the user clicks "Speichern" without entering a name
- **Then** a validation error is shown for the name field
- **And** the form is not submitted

### Create form submits and redirects to detail

- **Given** the user is on the create page
- **When** the user enters "New Corp" as name and clicks "Speichern"
- **Then** the company is created via POST `/api/companies`
- **And** the user is redirected to the detail page of the new company

### Create form cancel navigates back

- **Given** the user is on the create page
- **When** the user clicks "Abbrechen"
- **Then** the user is navigated back to the company list

### Create form shows error on API failure

- **Given** the user is on the create page
- **When** the user submits the form and the API returns an error
- **Then** an error message is displayed on the form
- **And** the user stays on the create page

## Company Edit

### Edit page shows form pre-filled with existing data

- **Given** a company "Open Elements" with email "info@oe.com" exists
- **When** the user navigates to `/companies/{id}/edit`
- **Then** the form is pre-filled with the existing company data

### Edit form submits and redirects to detail

- **Given** the user is on the edit page
- **When** the user changes the name to "Updated Corp" and clicks "Speichern"
- **Then** the company is updated via PUT `/api/companies/{id}`
- **And** the user is redirected to the detail page

### Edit form validates name is required

- **Given** the user is on the edit page
- **When** the user clears the name field and clicks "Speichern"
- **Then** a validation error is shown
- **And** the form is not submitted

### Edit form cancel navigates back to detail

- **Given** the user is on the edit page
- **When** the user clicks "Abbrechen"
- **Then** the user is navigated back to the company detail page

## Company Delete

### Delete from list shows confirmation dialog

- **Given** the company list is displayed with "Test Corp"
- **When** the user clicks the delete button for "Test Corp"
- **Then** a confirmation dialog is shown with text "Möchten Sie die Firma 'Test Corp' wirklich löschen?"

### Delete confirmation soft-deletes and refreshes list

- **Given** the delete confirmation dialog is open
- **When** the user confirms the deletion
- **Then** the company is soft-deleted via DELETE `/api/companies/{id}`
- **And** the company disappears from the list

### Delete cancel closes dialog without action

- **Given** the delete confirmation dialog is open
- **When** the user cancels
- **Then** the dialog closes
- **And** the company remains in the list

### Delete from detail page shows confirmation and redirects

- **Given** the user is on a company detail page
- **When** the user clicks "Löschen" and confirms
- **Then** the company is soft-deleted
- **And** the user is redirected to the company list

### Delete fails with 409 shows error dialog

- **Given** the company has associated contacts
- **When** the user confirms deletion
- **Then** an error dialog is shown with text "Die Firma kann nicht gelöscht werden, da noch Kontakte zugeordnet sind."
- **And** the company remains unchanged

## Company Restore

### Restore button restores company

- **Given** the archived view is active and a soft-deleted company is shown
- **When** the user clicks "Wiederherstellen"
- **Then** the company is restored via POST `/api/companies/{id}/restore`
- **And** the company is no longer marked as archived in the list

## Responsive Design

### Layout is responsive on mobile

- **Given** the viewport is a mobile screen size
- **When** the user views the company list
- **Then** the table adapts to the smaller screen (e.g., horizontal scroll or stacked layout)
- **And** all functionality remains accessible

## Loading States

### List shows loading state while fetching

- **Given** the user navigates to `/companies`
- **When** the data is being fetched
- **Then** skeleton placeholders or a loading indicator is shown

### Detail shows loading state while fetching

- **Given** the user navigates to a company detail page
- **When** the data is being fetched
- **Then** skeleton placeholders or a loading indicator is shown
