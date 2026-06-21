# Behaviors: Replace Separate Contact Filters with Unified Search Field

## Single Word Search

### Search matches firstName

- **Given** contact "Anna Schmidt" exists
- **When** the user types "Anna" in the search field
- **Then** "Anna Schmidt" is shown

### Search matches lastName

- **Given** contact "Anna Schmidt" exists
- **When** the user types "Schmidt" in the search field
- **Then** "Anna Schmidt" is shown

### Search matches email

- **Given** contact "Anna Schmidt" with email "anna@test.com" exists
- **When** the user types "anna@test" in the search field
- **Then** "Anna Schmidt" is shown

### Search matches company name

- **Given** contact "Bob Jones" is associated with company "Acme Corp"
- **When** the user types "Acme" in the search field
- **Then** "Bob Jones" is shown

### Search is case-insensitive

- **Given** contact "Anna Schmidt" exists
- **When** the user types "anna" (lowercase) in the search field
- **Then** "Anna Schmidt" is shown

### Search is contains-based

- **Given** contact "Anna Schmidt" exists
- **When** the user types "chmi" in the search field
- **Then** "Anna Schmidt" is shown (contains match on lastName)

## Multi-Word Search

### Both words must match (AND)

- **Given** contacts "Anna Schmidt" and "Anna Müller" exist
- **When** the user types "Anna Schmidt" in the search field
- **Then** only "Anna Schmidt" is shown
- **And** "Anna Müller" is not shown

### Words can match across different fields

- **Given** contact "Anna Müller" is associated with company "Schmidt GmbH"
- **When** the user types "Anna Schmidt" in the search field
- **Then** "Anna Müller" is shown (Anna matches firstName, Schmidt matches company name)

### Three-word search

- **Given** contact "Anna Schmidt" with email "anna@acme.com" at company "Acme Corp"
- **When** the user types "Anna Schmidt Acme" in the search field
- **Then** "Anna Schmidt" is shown (all three words match in some field)

## Contacts Without Company

### Search by name still finds contacts without company

- **Given** contact "Anna Schmidt" has no company assigned
- **When** the user types "Anna" in the search field
- **Then** "Anna Schmidt" is shown (LEFT JOIN ensures no exclusion)

### Company search term does not crash for contacts without company

- **Given** contact "Anna Schmidt" has no company assigned
- **When** the user types "Acme" in the search field
- **Then** "Anna Schmidt" is not shown
- **And** no error occurs

## Combination with Dropdown Filters

### Search combines with company dropdown (AND)

- **Given** contact "Anna Schmidt" at company "Acme" and contact "Anna Jones" at company "Beta"
- **When** the user types "Anna" in search AND selects company "Acme" in the dropdown
- **Then** only "Anna Schmidt" is shown

### Search combines with language dropdown (AND)

- **Given** contact "Anna Schmidt" (language=DE) and contact "Anna Jones" (language=EN)
- **When** the user types "Anna" in search AND selects language "DE"
- **Then** only "Anna Schmidt" is shown

### Search combines with Brevo filter (AND)

- **Given** Brevo contact "Anna Schmidt" and non-Brevo contact "Anna Jones"
- **When** the user types "Anna" in search AND selects "From Brevo"
- **Then** only "Anna Schmidt" is shown

## UI

### Single search input replaces three filter inputs

- **Given** the user navigates to the contact list
- **When** the page loads
- **Then** there is one search input with placeholder "Search..." / "Suche..."
- **And** there are no separate firstName, lastName, or email filter inputs

### Page resets when search changes

- **Given** the user is on page 2 of the contact list
- **When** the user types in the search field
- **Then** the page resets to page 1

## Edge Cases

### Empty search shows all contacts

- **Given** contacts exist
- **When** the search field is empty
- **Then** all contacts are shown (filtered only by dropdown filters)

### Whitespace-only search is treated as empty

- **Given** contacts exist
- **When** the user types only spaces in the search field
- **Then** all contacts are shown

### Extra whitespace between words is ignored

- **Given** contact "Anna Schmidt" exists
- **When** the user types "Anna    Schmidt" (multiple spaces)
- **Then** "Anna Schmidt" is shown (split by whitespace, extra spaces ignored)

## API

### Old parameters are removed

- **Given** the contact list API endpoint
- **When** a request is made with `?firstName=Anna`
- **Then** the `firstName` parameter is ignored (not recognized)

### New search parameter works

- **Given** contact "Anna Schmidt" exists
- **When** `GET /api/contacts?search=Anna` is called
- **Then** "Anna Schmidt" is returned
