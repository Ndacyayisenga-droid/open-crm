# Behaviors: Show Total Record Count in Company and Contact List Tables

## Company List

### Record count is shown with pagination

- **Given** 42 companies match the current filters
- **And** the list is paginated across 3 pages
- **When** the user views page 1
- **Then** the pagination area shows "42 Firmen · Seite 1 von 3" (DE) or "42 Companies · Page 1 of 3" (EN)

### Singular form for one company

- **Given** 1 company matches the current filters
- **When** the user views the list
- **Then** the pagination area shows "1 Firma · Seite 1 von 1" (DE) or "1 Company · Page 1 of 1" (EN)

### Count reflects active name filter

- **Given** 42 companies exist total
- **And** the user filters by name "Acme"
- **And** 3 companies match "Acme"
- **When** the filtered list renders
- **Then** the count shows "3 Firmen" (not 42)

### Count reflects active Brevo filter

- **Given** 42 companies exist, 10 from Brevo
- **And** the user selects "From Brevo" filter
- **When** the filtered list renders
- **Then** the count shows "10 Firmen"

### No count shown in empty state

- **Given** no companies match the current filters
- **When** the empty state is displayed
- **Then** no record count or pagination is shown

## Contact List

### Record count is shown with pagination

- **Given** 115 contacts match the current filters
- **And** the list is paginated across 6 pages
- **When** the user views page 1
- **Then** the pagination area shows "115 Kontakte · Seite 1 von 6" (DE) or "115 Contacts · Page 1 of 6" (EN)

### Singular form for one contact

- **Given** 1 contact matches the current filters
- **When** the user views the list
- **Then** the pagination area shows "1 Kontakt · Seite 1 von 1" (DE) or "1 Contact · Page 1 of 1" (EN)

### Count reflects active filters

- **Given** 115 contacts exist total
- **And** the user filters by lastName "Schmidt"
- **And** 5 contacts match
- **When** the filtered list renders
- **Then** the count shows "5 Kontakte"

### No count shown in empty state

- **Given** no contacts match the current filters
- **When** the empty state is displayed
- **Then** no record count or pagination is shown

## Count Updates

### Count updates when filter changes

- **Given** the company list shows "42 Firmen · Seite 1 von 3"
- **When** the user types "Open" in the name filter
- **And** 2 companies match
- **Then** the count updates to "2 Firmen · Seite 1 von 1"

### Count updates when navigating pages

- **Given** 42 companies match, showing page 1
- **When** the user navigates to page 2
- **Then** the count still shows "42 Firmen" (total doesn't change, only the page number changes)
