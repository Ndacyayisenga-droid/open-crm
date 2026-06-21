# Behaviors: No Company Filter

## Dropdown Option

### "No company" option visible in dropdown

- **Given** the contact list view is displayed
- **When** the user opens the company filter dropdown
- **Then** the options are in order: "All companies", "No company", followed by the alphabetically sorted company list

### Dropdown label translated (DE)

- **Given** the UI language is set to German
- **When** the user opens the company filter dropdown
- **Then** the option reads "Keine Firma"

### Dropdown label translated (EN)

- **Given** the UI language is set to English
- **When** the user opens the company filter dropdown
- **Then** the option reads "No company"

## Filtering Contacts

### Selecting "No company" shows only unassigned contacts

- **Given** contacts exist with and without company associations
- **When** the user selects "No company" in the company filter
- **Then** only contacts with no company association are shown in the table

### Selecting "No company" with no unassigned contacts shows empty state

- **Given** all contacts are assigned to a company
- **When** the user selects "No company" in the company filter
- **Then** the table shows the empty state (no results)

### Switching from "No company" to "All companies" shows all contacts again

- **Given** the "No company" filter is active
- **When** the user switches the company filter to "All companies"
- **Then** all contacts are shown regardless of company association

### "No company" filter combines with other filters

- **Given** contacts without a company exist with various languages
- **And** the "No company" filter is active
- **When** the user also sets the language filter to "DE"
- **Then** only contacts without a company AND with language "DE" are shown

### "No company" filter combines with search

- **Given** contacts without a company exist with various names
- **And** the "No company" filter is active
- **When** the user types "Schmidt" in the search field
- **Then** only contacts without a company AND matching "Schmidt" are shown

## Backend API

### noCompany=true returns only unassigned contacts

- **Given** contacts exist with and without company associations
- **When** `GET /api/contacts?noCompany=true` is called
- **Then** only contacts with `companyId IS NULL` are returned

### noCompany=false behaves like no filter

- **Given** contacts exist with and without company associations
- **When** `GET /api/contacts?noCompany=false` is called
- **Then** all contacts are returned (same as omitting the parameter)

### noCompany absent behaves like no filter

- **Given** contacts exist with and without company associations
- **When** `GET /api/contacts` is called without `noCompany` parameter
- **Then** all contacts are returned

### Conflict: companyId and noCompany together returns 400

- **Given** the API is available
- **When** `GET /api/contacts?companyId=<uuid>&noCompany=true` is called
- **Then** the response status is 400 Bad Request

## Print View

### Print view respects "No company" filter

- **Given** the "No company" filter is active in the contact list
- **When** the user clicks the print button
- **Then** the print view opens with only unassigned contacts

### Print view filter summary shows "No company"

- **Given** the "No company" filter is active
- **When** the print view is displayed
- **Then** the filter summary line includes the "No company" filter indicator

## CSV Export

### CSV export respects "No company" filter

- **Given** the "No company" filter is active in the contact list
- **When** the user triggers a CSV export
- **Then** the exported CSV contains only contacts without a company association

## Pagination

### Record count reflects "No company" filter

- **Given** 100 contacts exist, 15 without a company
- **When** the "No company" filter is selected
- **Then** the record count shows 15
