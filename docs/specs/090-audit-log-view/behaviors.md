# Behaviors: Audit Log View

## Access Control

### IT-ADMIN can access the audit log page

- **Given** a user with the IT-ADMIN role is logged in
- **When** the user navigates to `/admin/audit-logs`
- **Then** the audit log table is displayed

### Non-IT-ADMIN is shown the forbidden page

- **Given** a user without the IT-ADMIN role is logged in
- **When** the user navigates to `/admin/audit-logs`
- **Then** the `ForbiddenPage` component is rendered

### Unauthenticated request to API is rejected

- **Given** no authentication token is provided
- **When** a request is made to `GET /api/audit-logs`
- **Then** the response status is 401

### Non-IT-ADMIN request to API is rejected

- **Given** a user without the IT-ADMIN role
- **When** a request is made to `GET /api/audit-logs`
- **Then** the response status is 403

## Sidebar Navigation

### Audit log entry visible for IT-ADMIN

- **Given** a user with the IT-ADMIN role is logged in
- **When** the admin sub-menu in the sidebar is expanded
- **Then** an "Audit Log" entry is visible linking to `/admin/audit-logs`

### Audit log entry hidden for non-IT-ADMIN

- **Given** a user without the IT-ADMIN role is logged in
- **When** the sidebar is displayed
- **Then** no "Audit Log" entry is visible

## Table Display

### Audit log entries are displayed in a table

- **Given** audit log entries exist in the database
- **When** the audit log page loads
- **Then** a table is shown with columns: Type, Entity ID, Action, User, Date
- **And** entries are sorted by creation date descending (newest first)

### All fields are displayed correctly

- **Given** an audit log entry with entityType `CompanyDto`, entityId `550e8400-...`, action `INSERT`, user `Max Mustermann`, createdAt `2026-04-25T14:30:00Z`
- **When** the entry is rendered in the table
- **Then** the Type column shows `CompanyDto`
- **And** the Entity ID column shows the UUID
- **And** the Action column shows `INSERT`
- **And** the User column shows `Max Mustermann`
- **And** the Date column shows a localized date-time string

### System user entries are displayed

- **Given** an audit log entry with user `System`
- **When** no user filter is applied
- **Then** the entry is visible in the table with user column showing `System`

## Pagination

### Default page size is 20

- **Given** no page size preference is stored in localStorage
- **When** the audit log page loads
- **Then** the page size is 20
- **And** up to 20 entries are displayed

### Page size is persisted in localStorage

- **Given** the user selects page size 50
- **When** the user leaves and returns to the audit log page
- **Then** the page size is still 50

### Page size options are available

- **Given** the page-size selector is displayed
- **When** the user opens the selector
- **Then** the options 10, 20, 50, 100, 200 are available

### Changing page size resets to first page

- **Given** the user is on page 3 with page size 20
- **When** the user changes the page size to 50
- **Then** the page resets to page 0 (first page)

### Pagination controls shown when multiple pages exist

- **Given** 50 audit log entries exist and page size is 20
- **When** the table is displayed
- **Then** Previous and Next buttons are shown
- **And** total count "50 Entries" is displayed

### Pagination controls hidden for single page

- **Given** 10 audit log entries exist and page size is 20
- **When** the table is displayed
- **Then** Previous and Next buttons are not shown

### Navigate to next page

- **Given** the user is on page 0 and more pages exist
- **When** the user clicks "Next"
- **Then** page 1 is loaded and displayed

### Previous button disabled on first page

- **Given** the user is on page 0
- **When** the table is displayed
- **Then** the "Previous" button is disabled

### Next button disabled on last page

- **Given** the user is on the last page
- **When** the table is displayed
- **Then** the "Next" button is disabled

## Entity Type Filter

### Entity type dropdown is populated from API

- **Given** the audit log page loads
- **When** the entity type dropdown is opened
- **Then** it shows "All types" as the first option
- **And** lists all distinct entity types returned by `GET /api/audit-logs/entity-types`

### Filtering by entity type

- **Given** audit log entries exist for `CompanyDto` and `ContactDto`
- **When** the user selects `CompanyDto` in the entity type dropdown
- **Then** only entries with entityType `CompanyDto` are displayed
- **And** the page resets to 0

### Clearing entity type filter

- **Given** the entity type filter is set to `CompanyDto`
- **When** the user selects "All types"
- **Then** all entries are displayed again (subject to other active filters)
- **And** the page resets to 0

## User Filter

### User dropdown is populated from user list

- **Given** the audit log page loads
- **When** the user dropdown is opened
- **Then** it shows "All users" as the first option
- **And** lists all registered users by name from `GET /api/users`

### System is not in the user dropdown

- **Given** audit log entries exist with user `System`
- **When** the user dropdown is opened
- **Then** `System` is not listed as an option

### Filtering by user

- **Given** audit log entries exist for users `Max Mustermann` and `Anna Schmidt`
- **When** the user selects `Max Mustermann` in the user dropdown
- **Then** only entries with user `Max Mustermann` are displayed
- **And** the page resets to 0

### Clearing user filter

- **Given** the user filter is set to `Max Mustermann`
- **When** the user selects "All users"
- **Then** all entries are displayed again (subject to other active filters)
- **And** the page resets to 0

## Combined Filters

### Both filters applied simultaneously

- **Given** audit log entries exist for various entity types and users
- **When** the user selects entityType `CompanyDto` and user `Max Mustermann`
- **Then** only entries matching both filters are displayed

### Clearing one filter keeps the other active

- **Given** both filters are set: entityType `CompanyDto` and user `Max Mustermann`
- **When** the user clears the entity type filter (selects "All types")
- **Then** entries are filtered by user `Max Mustermann` only

## Empty States

### No audit log entries exist

- **Given** the audit log table is empty
- **When** the audit log page loads
- **Then** an empty state with an icon and message "No audit log entries." is displayed

### Filters produce no results

- **Given** audit log entries exist but none match the selected filter combination
- **When** the filters are applied
- **Then** the empty state is displayed

## Loading State

### Skeleton rows shown while loading

- **Given** the audit log page is loading data
- **When** the API request is in progress
- **Then** 5 skeleton rows are displayed

## Backend API

### GET /api/audit-logs without filters

- **Given** audit log entries exist
- **When** `GET /api/audit-logs?page=0&size=20&sort=createdAt,desc` is called
- **Then** the response contains a `Page<AuditLogDto>` with up to 20 entries sorted by createdAt descending

### GET /api/audit-logs with entityType filter

- **Given** audit log entries exist for `CompanyDto` and `ContactDto`
- **When** `GET /api/audit-logs?entityType=CompanyDto&page=0&size=20` is called
- **Then** only entries with entityType `CompanyDto` are returned

### GET /api/audit-logs with user filter

- **Given** audit log entries exist for users `Max Mustermann` and `System`
- **When** `GET /api/audit-logs?user=Max%20Mustermann&page=0&size=20` is called
- **Then** only entries with user `Max Mustermann` are returned

### GET /api/audit-logs with both filters

- **Given** audit log entries exist for various combinations
- **When** `GET /api/audit-logs?entityType=CompanyDto&user=Max%20Mustermann&page=0&size=20` is called
- **Then** only entries matching both entityType `CompanyDto` and user `Max Mustermann` are returned

### GET /api/audit-logs/entity-types

- **Given** audit log entries exist for `CompanyDto`, `ContactDto`, and `TagDto`
- **When** `GET /api/audit-logs/entity-types` is called
- **Then** the response is `["CompanyDto", "ContactDto", "TagDto"]`

### GET /api/audit-logs/entity-types with empty table

- **Given** no audit log entries exist
- **When** `GET /api/audit-logs/entity-types` is called
- **Then** the response is `[]`
