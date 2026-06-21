# Behaviors: Contact Birthday Field

## Contact Form — Birthday Input

### Birthday can be set when creating a contact

- **Given** the user is on the "create contact" form
- **When** the user selects a date in the birthday picker and saves
- **Then** the contact is created with the selected birthday

### Birthday can be left empty when creating a contact

- **Given** the user is on the "create contact" form
- **When** the user does not select a birthday and saves
- **Then** the contact is created with no birthday (null)

### Birthday can be set when editing a contact

- **Given** an existing contact without a birthday
- **When** the user edits the contact and selects a birthday
- **Then** the contact is updated with the selected birthday

### Birthday can be cleared when editing a contact

- **Given** an existing contact with a birthday set
- **When** the user edits the contact and clears the birthday field
- **Then** the contact is updated with no birthday (null)

### Birthday can be changed when editing a contact

- **Given** an existing contact with birthday 1990-03-15
- **When** the user edits the contact and selects 1985-07-22
- **Then** the contact is updated with birthday 1985-07-22

## Contact Detail View — Birthday Display

### Birthday is displayed when set

- **Given** a contact with birthday 1990-03-15
- **When** the user views the contact detail page
- **Then** the birthday is displayed as "15.03.1990" (DE) or "03/15/1990" (EN)

### No birthday shows dash

- **Given** a contact without a birthday
- **When** the user views the contact detail page
- **Then** the birthday field displays "—"

## Contact List — No Birthday Column

### Birthday is not shown in the contact list table

- **Given** the contact list table
- **When** it renders
- **Then** there is no birthday column

## Backend — API

### ContactDto includes birthday when set

- **Given** a contact with birthday 1990-03-15
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `birthday: "1990-03-15"`

### ContactDto includes null birthday when not set

- **Given** a contact without a birthday
- **When** the API returns the contact via `GET /api/contacts/{id}`
- **Then** the response includes `birthday: null`

### Birthday can be set via create endpoint

- **Given** a valid contact creation request with `birthday: "1990-03-15"`
- **When** the API receives `POST /api/contacts`
- **Then** the contact is created with the specified birthday

### Birthday can be omitted in create request

- **Given** a valid contact creation request without a `birthday` field
- **When** the API receives `POST /api/contacts`
- **Then** the contact is created with birthday as null

### Birthday can be set via update endpoint

- **Given** an existing contact
- **When** the API receives `PUT /api/contacts/{id}` with `birthday: "1985-07-22"`
- **Then** the contact's birthday is updated to the specified date

### Birthday can be cleared via update endpoint

- **Given** an existing contact with a birthday
- **When** the API receives `PUT /api/contacts/{id}` with `birthday: null`
- **Then** the contact's birthday is set to null

### Invalid birthday date is rejected

- **Given** a contact creation or update request with `birthday: "2023-13-45"`
- **When** the API receives the request
- **Then** the API returns a 400 Bad Request error

## Database Migration

### Existing contacts are unaffected

- **Given** existing contacts in the database before the migration
- **When** migration V4 runs
- **Then** all existing contacts have birthday as null
- **And** all other contact data is unchanged

## i18n

### German label for birthday

- **Given** the UI is set to German
- **When** the contact detail or form renders
- **Then** the birthday label reads "Geburtstag"

### English label for birthday

- **Given** the UI is set to English
- **When** the contact detail or form renders
- **Then** the birthday label reads "Birthday"

## February 29th Edge Case

### Leap day birthday is accepted

- **Given** the user enters birthday 2000-02-29 (leap year)
- **When** the contact is saved
- **Then** the birthday is stored as 2000-02-29

### Invalid leap day is rejected

- **Given** the user enters birthday 1900-02-29 (not a leap year)
- **When** the contact is saved
- **Then** the API returns a 400 Bad Request error
