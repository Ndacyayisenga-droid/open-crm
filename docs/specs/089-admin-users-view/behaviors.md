# Behaviors: Admin Users View

## Page Access

### IT-ADMIN can access the users page

- **Given** a logged-in user with the IT-ADMIN role
- **When** the user navigates to `/admin/users`
- **Then** the users page is displayed with the paginated user table

### Non-IT-ADMIN is shown forbidden page

- **Given** a logged-in user without the IT-ADMIN role
- **When** the user navigates to `/admin/users`
- **Then** the ForbiddenPage is displayed

### Unauthenticated user cannot access the endpoint

- **Given** no authentication token is provided
- **When** a request is made to `GET /api/users`
- **Then** the response status is 401 Unauthorized

### Non-IT-ADMIN cannot access the endpoint

- **Given** a valid authentication token for a user without the IT-ADMIN role
- **When** a request is made to `GET /api/users`
- **Then** the response status is 403 Forbidden

## Sidebar Navigation

### Users item visible for IT-ADMIN

- **Given** a logged-in user with the IT-ADMIN role
- **When** the sidebar is rendered
- **Then** the admin sub-menu contains a "Users" nav item linking to `/admin/users`

### Users item hidden for non-IT-ADMIN

- **Given** a logged-in user without the IT-ADMIN role
- **When** the sidebar is rendered
- **Then** the admin sub-menu (including the "Users" item) is not visible

## User Table — Happy Path

### Display users with all fields

- **Given** 3 users exist in the system, each with name, email, and avatarUrl
- **When** the users page is loaded
- **Then** a table with 3 rows is displayed, each showing the user's avatar image, name, and email

### Display user with null avatarUrl

- **Given** a user exists with `avatarUrl` set to `null`
- **When** the users page is loaded
- **Then** the user's row shows a fallback icon instead of an avatar image

### Avatar is displayed as circular thumbnail

- **Given** a user exists with a valid `avatarUrl`
- **When** the users page is loaded
- **Then** the avatar is rendered as a small circular image (32x32px)

## Loading and Empty States

### Loading state shows skeletons

- **Given** the users page is loading data
- **When** the page is rendered during the loading phase
- **Then** skeleton placeholders are displayed instead of the table

### Empty state when no users exist

- **Given** no users exist in the system
- **When** the users page is loaded
- **Then** an empty state message is displayed ("No users registered yet." / "Noch keine Benutzer registriert.")

## Pagination

### Default page size is 20

- **Given** no page size preference is stored in localStorage
- **When** the users page is loaded
- **Then** the page requests 20 items per page from the API

### Page size is persisted in localStorage

- **Given** the user selects page size 50 from the page-size selector
- **When** the user navigates away and returns to the users page
- **Then** the page size is still 50

### Page size options match other tables

- **Given** the users page is loaded
- **When** the user opens the page-size selector
- **Then** the options are 10, 20, 50, 100, and 200

### Total user count is displayed

- **Given** 35 users exist in the system
- **When** the users page is loaded with page size 20
- **Then** the text "35 Users" (or "35 Benutzer") is displayed next to the page-size selector

### Pagination buttons shown when needed

- **Given** 35 users exist and page size is 20
- **When** the users page is loaded
- **Then** Previous and Next buttons are displayed

### Pagination buttons hidden for single page

- **Given** 5 users exist and page size is 20
- **When** the users page is loaded
- **Then** no Previous/Next buttons are displayed

### Previous button disabled on first page

- **Given** the user is on page 1 of the users table
- **When** the page is rendered
- **Then** the Previous button is disabled

### Next button disabled on last page

- **Given** the user is on the last page of the users table
- **When** the page is rendered
- **Then** the Next button is disabled

### Navigate to next page

- **Given** the user is on page 1 and more pages exist
- **When** the user clicks Next
- **Then** page 2 is loaded and displayed

### Navigate to previous page

- **Given** the user is on page 2
- **When** the user clicks Previous
- **Then** page 1 is loaded and displayed

### Changing page size resets to first page

- **Given** the user is on page 3 with page size 10
- **When** the user changes page size to 50
- **Then** the table resets to page 1 and loads 50 items per page

## Backend Endpoint

### Paginated response structure

- **Given** 25 users exist in the system
- **When** `GET /api/users?page=0&size=10` is called
- **Then** the response contains 10 users in `content`, `page.number` is 0, `page.size` is 10, `page.totalElements` is 25, `page.totalPages` is 3

### Default pagination without parameters

- **Given** users exist in the system
- **When** `GET /api/users` is called without page/size parameters
- **Then** the response uses default pagination (page 0, size 20)
