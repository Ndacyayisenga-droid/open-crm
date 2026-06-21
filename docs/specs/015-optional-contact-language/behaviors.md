# Behaviors: Optional Contact Language

## Contact Creation

### Create contact without language

- **Given** the user is on the create contact form
- **When** the user fills in all required fields but selects "Unbekannt" / "Unknown" as language
- **Then** the contact is created successfully with `language: null` in the database

### Create contact with language

- **Given** the user is on the create contact form
- **When** the user fills in all required fields and selects "DE" as language
- **Then** the contact is created with `language: DE` (existing behavior, unchanged)

### Form does not pre-select a language

- **Given** the user opens the create contact form
- **When** the form renders
- **Then** the language dropdown has no pre-selected value and the user must actively choose

## Contact Editing

### Change language to unknown

- **Given** a contact exists with language "DE"
- **When** the user edits the contact and changes language to "Unbekannt" / "Unknown"
- **Then** the contact is saved with `language: null`

### Change language from unknown to a value

- **Given** a contact exists with `language: null`
- **When** the user edits the contact and selects "EN"
- **Then** the contact is saved with `language: EN`

### Edit form shows current unknown language

- **Given** a contact exists with `language: null`
- **When** the user opens the edit form
- **Then** the language dropdown shows "Unbekannt" / "Unknown" as the selected value

## Contact Detail View

### Display unknown language

- **Given** a contact exists with `language: null`
- **When** the user views the contact detail page
- **Then** the language field shows "Unbekannt" (DE UI) or "Unknown" (EN UI)

### Display known language

- **Given** a contact exists with `language: DE`
- **When** the user views the contact detail page
- **Then** the language field shows "DE" (existing behavior, unchanged)

## Contact List Filter

### Filter by unknown language

- **Given** contacts exist with languages DE, EN, and null
- **When** the user selects "Unbekannt" / "Unknown" in the language filter
- **Then** only contacts with `language: null` are shown

### Filter by specific language excludes unknown

- **Given** contacts exist with languages DE, EN, and null
- **When** the user selects "DE" in the language filter
- **Then** only contacts with `language: DE` are shown (contacts with null are excluded)

### Filter by all languages includes unknown

- **Given** contacts exist with languages DE, EN, and null
- **When** the user selects "Alle Sprachen" / "All languages"
- **Then** all contacts are shown, including those with `language: null`

## API

### Create contact via API with null language

- **Given** the API receives a POST to `/api/contacts` with `"language": null`
- **When** the request is processed
- **Then** the contact is created with `language: null` and returns 201

### Create contact via API without language field

- **Given** the API receives a POST to `/api/contacts` without a `language` field in the body
- **When** the request is processed
- **Then** the contact is created with `language: null` and returns 201

### Update contact via API to null language

- **Given** a contact exists with `language: DE`
- **When** the API receives a PUT with `"language": null`
- **Then** the contact is updated to `language: null` and returns 200

## Database

### Existing contacts unchanged after migration

- **Given** the database contains contacts with language DE and EN
- **When** the migration runs
- **Then** all existing contacts retain their original language values
