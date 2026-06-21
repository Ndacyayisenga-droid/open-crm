# Behaviors: Page Size Selector

## Page Size Selection

### Default page size is 20

- **Given** a user opens the company list for the first time (no localStorage entry)
- **When** the page loads
- **Then** the page size selector shows "20" as selected
- **Then** the API is called with `size=20`
- **Then** up to 20 records are displayed

### User selects a different page size

- **Given** the user is viewing the company list with the default page size of 20
- **When** the user selects "50" from the page size dropdown
- **Then** the page resets to page 1
- **Then** the API is called with `size=50&page=0`
- **Then** up to 50 records are displayed
- **Then** the record count and page info update correctly

### Page size persists across page reloads

- **Given** the user has selected page size 100 on the contact list
- **When** the user reloads the browser page
- **Then** the page size selector shows "100"
- **Then** the API is called with `size=100`

### Page size is stored per list independently

- **Given** the user has selected page size 50 on the company list
- **When** the user navigates to the contact list
- **Then** the contact list uses its own stored page size (or default 20)
- **Then** the company list retains its page size of 50 when navigated back to

### Page size persists during filter changes

- **Given** the user has selected page size 100 on the company list
- **When** the user changes the name filter or Brevo filter
- **Then** the page size remains at 100
- **Then** the page resets to page 1

### Page size persists during page navigation

- **Given** the user has selected page size 50
- **When** the user clicks "Next" to go to page 2
- **Then** the page size remains at 50

## Page Size Options

### All five options are available

- **Given** the user opens the page size selector dropdown
- **When** the dropdown is expanded
- **Then** the options 10, 20, 50, 100, and 200 are shown

### Selecting the smallest page size

- **Given** the user selects page size 10
- **When** there are 25 records total
- **Then** the pagination shows 3 pages
- **Then** up to 10 records are displayed on the first page

### Selecting the largest page size

- **Given** the user selects page size 200
- **When** there are 50 records total
- **Then** all 50 records are displayed on a single page
- **Then** the pagination shows 1 page

## Record Count Display

### Record count updates with page size change

- **Given** there are 42 companies
- **When** the user selects page size 10
- **Then** the display shows "42 Firmen · Seite 1 von 5"

### Record count with page size larger than total

- **Given** there are 15 contacts
- **When** the user selects page size 50
- **Then** the display shows "15 Kontakte · Seite 1 von 1"

## Tag List Specific

### Tag list range display updates correctly

- **Given** there are 55 tags and the user selects page size 10
- **When** the first page is displayed
- **Then** the display shows "Zeige 1–10 von 55 Tags"

### Tag list range display on last page

- **Given** there are 55 tags and the user selects page size 10
- **When** the user navigates to page 6 (the last page)
- **Then** the display shows "Zeige 51–55 von 55 Tags"

## Edge Cases

### Invalid localStorage value

- **Given** the localStorage key `pageSize.companies` contains an invalid value (e.g., "abc" or "30")
- **When** the company list loads
- **Then** the default page size of 20 is used
- **Then** the invalid value is overwritten with 20

### Page size change when on a page beyond new range

- **Given** the user is on page 5 with page size 10 (records 41-50 of 80)
- **When** the user changes page size to 50
- **Then** the page resets to page 1
- **Then** records 1-50 are displayed

## Not Affected

### Print view ignores page size setting

- **Given** the user has selected page size 10 on the company list
- **When** the user opens the print view
- **Then** all matching records are displayed (not limited to 10)

### CSV export ignores page size setting

- **Given** the user has selected page size 10 on the contact list
- **When** the user exports to CSV
- **Then** all matching records are exported (not limited to 10)

### Comment lists are not affected

- **Given** the company or contact detail view is open
- **When** comments are loaded
- **Then** comments still use the fixed "load more" pattern with 20 per batch
- **Then** no page size selector is shown for comments
