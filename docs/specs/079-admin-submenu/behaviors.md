# Behaviors: Admin Sub-Menu in Sidebar

## Desktop Sidebar — Collapsible Sub-Menu

### Admin menu is collapsed by default on non-admin pages

- **Given** the user is on the Companies page (`/companies`)
- **When** the sidebar is visible
- **Then** "Admin" is shown as a single item with a collapsed chevron
- **Then** no sub-items are visible

### Admin menu expands on click

- **Given** the admin sub-menu is collapsed
- **When** the user clicks the "Admin" parent item
- **Then** the sub-menu expands showing five items: Server Status, Bearer Token, Brevo Integration, API Keys, Webhooks
- **Then** the chevron rotates to indicate expanded state
- **Then** no page navigation occurs

### Admin menu collapses on click

- **Given** the admin sub-menu is expanded
- **When** the user clicks the "Admin" parent item
- **Then** the sub-menu collapses and sub-items are hidden
- **Then** the chevron rotates back to collapsed state

### Sub-item navigates to page

- **Given** the admin sub-menu is expanded
- **When** the user clicks "Server Status"
- **Then** the browser navigates to `/admin/status`
- **Then** the sub-menu remains expanded

### Active sub-item is highlighted

- **Given** the user is on `/admin/token`
- **When** the sidebar is visible
- **Then** "Bearer Token" has the active style (green background/text)
- **Then** the "Admin" parent item has a subtle highlight (brighter text)
- **Then** the other sub-items have default styling

### Admin menu is open on admin pages after refresh

- **Given** the user is on `/admin/brevo`
- **When** the page is refreshed
- **Then** the admin sub-menu is open
- **Then** "Brevo Integration" is highlighted as active

### Admin menu is open on API Keys page

- **Given** the user navigates to `/api-keys`
- **When** the sidebar is visible
- **Then** the admin sub-menu is open
- **Then** "API Keys" is highlighted as active

### Admin menu is open on Webhooks page

- **Given** the user navigates to `/webhooks`
- **When** the sidebar is visible
- **Then** the admin sub-menu is open
- **Then** "Webhooks" is highlighted as active

### Admin menu closes when navigating away

- **Given** the admin sub-menu is open and the user is on `/admin/status`
- **When** the user clicks "Companies" in the sidebar
- **Then** the browser navigates to `/companies`
- **Then** the admin sub-menu is collapsed

### Clicking Admin parent does not navigate

- **Given** the user is on `/companies`
- **When** the user clicks the "Admin" parent item
- **Then** the sub-menu toggles open
- **Then** the URL remains `/companies`

## Mobile Sidebar — Flat List

### Mobile shows flat admin items

- **Given** the user is on a mobile device
- **When** the user opens the hamburger menu
- **Then** all five admin items are shown as top-level entries: Server Status, Bearer Token, Brevo Integration, API Keys, Webhooks
- **Then** there is no "Admin" parent item or collapsible group

### Mobile item navigates and closes sheet

- **Given** the mobile sidebar sheet is open
- **When** the user taps "Webhooks"
- **Then** the browser navigates to `/webhooks`
- **Then** the sheet closes

### Mobile active state

- **Given** the user is on `/admin/token` on a mobile device
- **When** the hamburger menu is opened
- **Then** "Bearer Token" has the active style (green background/text)

## Admin Page Split — Server Status

### Server Status page shows health check

- **Given** the backend is running
- **When** the user navigates to `/admin/status`
- **Then** a page with the heading "Server Status" is shown
- **Then** the health status indicator shows "UP" (green)

### Server Status page when backend is down

- **Given** the backend is not reachable
- **When** the user navigates to `/admin/status`
- **Then** the health status indicator shows unavailable/down state

## Admin Page Split — Bearer Token

### Bearer Token page shows token card

- **Given** the user is authenticated with a valid session
- **When** the user navigates to `/admin/token`
- **Then** a page with the heading "Bearer Token" is shown
- **Then** the token is masked by default
- **Then** show/hide, copy buttons are available
- **Then** token validity countdown is displayed

### Bearer Token page without session

- **Given** no access token is available in the session
- **When** the user navigates to `/admin/token`
- **Then** a "No token available" message is shown

## Admin Page Split — Brevo Integration

### Brevo page shows settings and import

- **Given** the user is authenticated
- **When** the user navigates to `/admin/brevo`
- **Then** a page with the heading "Brevo Integration" is shown
- **Then** the Brevo settings card and import functionality are displayed

## Route Handling

### Old /admin route redirects

- **Given** the user navigates to `/admin` directly (e.g. bookmark)
- **When** the page loads
- **Then** the user is redirected to `/admin/status`

### Unknown admin sub-route

- **Given** the user navigates to `/admin/nonexistent`
- **When** the page loads
- **Then** the standard Next.js 404 page is shown
