# Behaviors: Remove Sorting Dropdown from Contact List Table

## Sorting UI

### No sorting dropdown is visible

- **Given** the user navigates to the contact list
- **When** the page loads
- **Then** no sorting dropdown is displayed

## Default Sort Order

### Contacts are always sorted by lastName ascending

- **Given** contacts "Anna Zimmermann", "Bob Adams", and "Clara Müller" exist
- **When** the user views the contact list
- **Then** the contacts are displayed in order: "Bob Adams", "Clara Müller", "Anna Zimmermann"

### Sort order is consistent across pagination

- **Given** 30 contacts exist (more than one page)
- **When** the user views page 1
- **Then** the first 20 contacts are sorted by lastName ascending
- **When** the user navigates to page 2
- **Then** the remaining contacts continue in lastName ascending order

## API

### Frontend does not send sort parameter

- **Given** the contact list loads
- **When** the API request is made
- **Then** no `sort` query parameter is included in the request URL

## Filter Interaction

### Filters work without sort parameter

- **Given** the user filters by lastName "Schmidt"
- **When** the filtered results load
- **Then** the results are sorted by lastName ascending (backend default)
- **And** no sort parameter is sent

### Page resets when filter changes

- **Given** the user is on page 2 of the contact list
- **When** the user types in a filter input
- **Then** the page resets to page 1

## Translation Cleanup

### Sort-related translations are removed

- **Given** the i18n configuration
- **When** the app renders
- **Then** no `contacts.sort.*` keys are referenced
