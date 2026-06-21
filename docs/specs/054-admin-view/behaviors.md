# Behaviors: Admin View

## Admin Page — Layout

### Page displays title

- **Given** the user navigates to `/admin`
- **When** the page renders
- **Then** the heading "Administration" is displayed at the top

### Three sections in correct order

- **Given** the user is on the Admin page
- **When** the page renders
- **Then** three card sections are displayed in order: Server Health, Brevo API Settings, Brevo Import

### Server Health card shows backend status

- **Given** the backend is running
- **When** the Admin page loads
- **Then** the Server Health card shows a green indicator and "Backend is running"

### Server Health card shows backend unavailable

- **Given** the backend is not reachable
- **When** the Admin page loads
- **Then** the Server Health card shows a red indicator and "Backend is unavailable"

### Brevo API Settings card works

- **Given** the user is on the Admin page
- **When** the user configures, changes, or removes the Brevo API key
- **Then** the API settings card behaves exactly as before (save, change, remove, error states)

### Brevo Import card works

- **Given** a Brevo API key is configured
- **When** the user clicks "Start Import" on the Admin page
- **Then** the import runs and shows results (imported, updated, failed counts) exactly as before

### Brevo Import disabled without API key

- **Given** no Brevo API key is configured
- **When** the user views the Admin page
- **Then** the Import card shows the "configure first" message and the import button is disabled

## Sidebar — Navigation

### Admin item positioned at bottom

- **Given** the sidebar is rendered (desktop)
- **When** the user views the navigation
- **Then** Companies, Contacts, Tags are at the top, and Admin is at the bottom with flexible space in between

### Admin item has Settings icon

- **Given** the sidebar is rendered
- **When** the user views the Admin nav item
- **Then** it shows a gear/settings icon and the label "Admin"

### Border between Admin and language toggle

- **Given** the sidebar is rendered (desktop)
- **When** the user views the bottom section
- **Then** a visible border line separates the Admin item from the language toggle below

### Admin item active state

- **Given** the user is on the `/admin` page
- **When** the sidebar renders
- **Then** the Admin nav item is highlighted with the active style

### Brevo Import and Server Health nav items removed

- **Given** the sidebar is rendered
- **When** the user views the navigation
- **Then** there are no "Brevo Import" or "Server Health" nav items

### Mobile menu reflects same structure

- **Given** the user is on a mobile device
- **When** the hamburger menu is opened
- **Then** the nav shows Companies, Contacts, Tags at top and Admin at the bottom, matching the desktop layout

## Route Cleanup

### Old Brevo route returns 404

- **Given** the `/brevo-sync` route has been removed
- **When** a user navigates to `/brevo-sync`
- **Then** the Next.js default 404 page is shown

### Old Health route returns 404

- **Given** the `/health` route has been removed
- **When** a user navigates to `/health`
- **Then** the Next.js default 404 page is shown

### Admin route works

- **Given** the admin page exists
- **When** a user navigates to `/admin`
- **Then** the Administration page with all three sections is displayed

## i18n

### Admin page title respects language

- **Given** the language is set to German
- **When** the user opens the Admin page
- **Then** the title shows "Administration"

### Admin nav label respects language

- **Given** the language is set to English
- **When** the sidebar renders
- **Then** the Admin nav item shows "Admin"

### Brevo and Health translations still work

- **Given** the language is set to German
- **When** the Admin page renders
- **Then** all card labels, buttons, and messages use the German translations (e.g., "Systemstatus", "API-Einstellungen", "Import")
