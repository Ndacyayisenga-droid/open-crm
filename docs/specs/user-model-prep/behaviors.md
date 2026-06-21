# Behaviors: User Model Preparation

## Comment Author

### New comments use dummy user name

- **Given** the application is running with the hardcoded dummy user
- **When** a user creates a comment on a company
- **Then** the comment's author is set to "Demo User"

### New contact comments use dummy user name

- **Given** the application is running with the hardcoded dummy user
- **When** a user creates a comment on a contact
- **Then** the comment's author is set to "Demo User"

### Existing comments remain unchanged

- **Given** comments exist with author "UNKNOWN"
- **When** the application starts after the update
- **Then** existing comments still show "UNKNOWN" as author

### Comment author displayed in UI

- **Given** a comment was created with author "Demo User"
- **When** the comment is displayed in the detail view
- **Then** "Demo User" is shown as the comment author

## Sidebar User Section

### User displayed at bottom of sidebar (desktop)

- **Given** the desktop sidebar is displayed
- **When** the page renders
- **Then** a user section appears at the very bottom of the sidebar (below language switch)
- **And** it shows a placeholder user icon
- **And** it shows the name "Demo User"

### User displayed at bottom of sidebar (mobile)

- **Given** the mobile sidebar drawer is opened
- **When** the drawer renders
- **Then** a user section appears at the very bottom (below language switch)
- **And** it shows a placeholder user icon
- **And** it shows the name "Demo User"

### Logout button visible

- **Given** the sidebar is displayed
- **When** the user section is visible
- **Then** a "Logout" / "Abmelden" button is shown

### Logout button translated (DE)

- **Given** the UI language is set to German
- **When** the sidebar is displayed
- **Then** the logout button reads "Abmelden"

### Logout button translated (EN)

- **Given** the UI language is set to English
- **When** the sidebar is displayed
- **Then** the logout button reads "Logout"

### Logout button is no-op

- **Given** the sidebar is displayed
- **When** the user clicks the "Logout" button
- **Then** nothing happens (no navigation, no error, no state change)

## No User API

### No user endpoint exists

- **Given** the backend is running
- **When** `GET /api/user` or `GET /api/users` is called
- **Then** the response is 404 (no such endpoint)

## No User in Database

### No user table exists

- **Given** the database schema
- **When** the tables are inspected
- **Then** there is no `user` or `users` table
