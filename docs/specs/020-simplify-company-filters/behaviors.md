# Behaviors: Simplify Company List Filters and Sorting

## Company List Filters

### Only name filter is displayed

- **Given** the user navigates to the company list page
- **When** the page loads
- **Then** only a single text input for filtering by name is visible
- **And** no city filter input is displayed
- **And** no country filter input is displayed
- **And** no sorting dropdown is displayed

### Name filter performs case-insensitive contains search

- **Given** companies "Open Elements", "Closed Systems", and "OpenAI" exist
- **When** the user types "open" in the name filter
- **Then** "Open Elements" and "OpenAI" are shown
- **And** "Closed Systems" is not shown

### Name filter with empty input shows all companies

- **Given** companies exist in the system
- **When** the name filter is empty
- **Then** all (non-archived) companies are shown

### Archive toggle still works alongside name filter

- **Given** company "Acme" is active and company "OldCorp" is archived
- **When** the user types nothing in the name filter and archive toggle is off
- **Then** only "Acme" is shown
- **When** the user enables the archive toggle
- **Then** both "Acme" and "OldCorp" are shown

### Name filter combined with archive toggle

- **Given** active company "Acme Corp" and archived company "Acme Old" exist
- **When** the user types "Acme" in the name filter with archive toggle off
- **Then** only "Acme Corp" is shown
- **When** the user enables the archive toggle
- **Then** both "Acme Corp" and "Acme Old" are shown

## Company List Sorting

### List is always sorted by name ascending

- **Given** companies "Zebra Inc", "Acme Corp", and "Middle GmbH" exist
- **When** the user views the company list
- **Then** the companies are displayed in order: "Acme Corp", "Middle GmbH", "Zebra Inc"

### No sorting UI is visible

- **Given** the user navigates to the company list page
- **When** the page loads
- **Then** no sorting dropdown or sort controls are displayed

### Sort order is consistent across pagination

- **Given** 30 companies exist (more than one page)
- **When** the user views page 1
- **Then** the first 20 companies are shown in alphabetical order by name
- **When** the user navigates to page 2
- **Then** the remaining companies continue in alphabetical order

## Backend API

### API no longer accepts city parameter

- **Given** the company list API endpoint
- **When** a request is made with `?city=Berlin`
- **Then** the `city` parameter is ignored (not recognized by the controller)
- **And** the response returns all companies (filtered only by name and includeDeleted if provided)

### API no longer accepts country parameter

- **Given** the company list API endpoint
- **When** a request is made with `?country=DE`
- **Then** the `country` parameter is ignored (not recognized by the controller)
- **And** the response returns all companies (filtered only by name and includeDeleted if provided)

### API name filter still works

- **Given** companies "Open Elements" and "Closed Systems" exist
- **When** a request is made with `?name=open`
- **Then** only "Open Elements" is returned

### API default sort is name ascending

- **Given** companies "Zebra Inc" and "Acme Corp" exist
- **When** a request is made without any sort parameter
- **Then** the results are sorted by name ascending ("Acme Corp" before "Zebra Inc")

### API includeDeleted parameter still works

- **Given** active company "Active Corp" and soft-deleted company "Deleted Corp" exist
- **When** a request is made with `?includeDeleted=true`
- **Then** both companies are returned
- **When** a request is made with `?includeDeleted=false`
- **Then** only "Active Corp" is returned

## Page Reset

### Page resets to first when name filter changes

- **Given** the user is on page 2 of the company list
- **When** the user types in the name filter
- **Then** the page resets to page 1

### Page resets when archive toggle changes

- **Given** the user is on page 2 of the company list
- **When** the user toggles the archive switch
- **Then** the page resets to page 1

## Company Select Dropdown (other views)

### Company select for contacts still works

- **Given** a user is creating or editing a contact
- **When** the company select dropdown loads
- **Then** companies are listed in alphabetical order (using backend default sort)
- **And** the request does not send a `sort` query parameter
