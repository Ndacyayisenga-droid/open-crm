# Behaviors: Page Serialization Fix

## Backend Warning Removed

### No PageImpl warning in logs

- **Given** the backend is running with `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`
- **When** a paginated endpoint is called (e.g., `GET /api/companies`)
- **Then** no "Serializing PageImpl instances as-is is not supported" warning appears in the logs

## JSON Structure

### Company list returns new page structure

- **Given** companies exist in the system
- **When** `GET /api/companies` is called
- **Then** the response contains `content` (array) and `page` (object with `size`, `number`, `totalElements`, `totalPages`)
- **And** there is no top-level `first`, `last`, `totalElements`, `totalPages`, `number`, or `size` field

### Contact list returns new page structure

- **Given** contacts exist in the system
- **When** `GET /api/contacts` is called
- **Then** the response contains `content` (array) and `page` (object with `size`, `number`, `totalElements`, `totalPages`)

### Comment list returns new page structure

- **Given** comments exist for a company
- **When** `GET /api/companies/{id}/comments` is called
- **Then** the response contains `content` (array) and `page` (object with `size`, `number`, `totalElements`, `totalPages`)

## Frontend Pagination

### Company list pagination still works

- **Given** more than 20 companies exist
- **When** the company list is displayed
- **Then** the first page shows 20 companies
- **And** the pagination shows the correct total count and page numbers
- **And** navigating to page 2 shows the next batch

### Contact list pagination still works

- **Given** more than 20 contacts exist
- **When** the contact list is displayed
- **Then** the first page shows 20 contacts
- **And** the pagination shows the correct total count and page numbers
- **And** navigating to page 2 shows the next batch

### Record count display still works

- **Given** 42 companies exist matching the current filter
- **When** the company list is displayed
- **Then** the record count shows "42" (derived from `page.totalElements`)

## Print Views

### Company print view loads all pages

- **Given** 50 companies exist (more than one page of 250)
- **When** the company print view is opened
- **Then** all 50 companies are displayed in the print table
- **And** the paginated fetch loop terminates correctly

### Contact print view loads all pages

- **Given** 300 contacts exist (more than one page of 250)
- **When** the contact print view is opened
- **Then** all 300 contacts are displayed in the print table
- **And** the paginated fetch loop terminates correctly (fetches page 0 and page 1)

## CSV Export

### CSV export fetches all pages

- **Given** more than 250 contacts exist
- **When** a CSV export is triggered
- **Then** the export contains all contacts (the paginated fetch loop completes correctly)

## Edge Cases

### Empty result set

- **Given** no companies match the current filter
- **When** `GET /api/companies?name=nonexistent` is called
- **Then** the response contains `content: []` and `page: { totalElements: 0, totalPages: 0, number: 0, size: 20 }`

### Single page of results

- **Given** 5 companies exist
- **When** `GET /api/companies` is called
- **Then** the response contains all 5 in `content` and `page: { totalElements: 5, totalPages: 1, number: 0, size: 20 }`
- **And** the frontend correctly identifies this as the last (and only) page
