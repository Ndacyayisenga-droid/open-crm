# Behaviors: Make Brevo-Managed Contact Fields Read-Only

## Backend — Field Protection

### Changing firstName on Brevo contact is rejected

- **Given** a contact with `brevoId = "200"` and `firstName = "Anna"`
- **When** `PUT /api/contacts/{id}` is called with `firstName = "Anne"` (and all other fields unchanged)
- **Then** the response is `400 Bad Request`
- **And** the error message states that `firstName` is managed by Brevo and cannot be modified
- **And** the contact's firstName remains "Anna" in the database

### Changing lastName on Brevo contact is rejected

- **Given** a contact with `brevoId = "200"` and `lastName = "Schmidt"`
- **When** `PUT /api/contacts/{id}` is called with `lastName = "Müller"` (and all other fields unchanged)
- **Then** the response is `400 Bad Request`
- **And** the error message mentions `lastName`

### Changing email on Brevo contact is rejected

- **Given** a contact with `brevoId = "200"` and `email = "anna@test.com"`
- **When** `PUT /api/contacts/{id}` is called with `email = "anna@new.com"`
- **Then** the response is `400 Bad Request`
- **And** the error message mentions `email`

### Changing language on Brevo contact is rejected

- **Given** a contact with `brevoId = "200"` and `language = DE`
- **When** `PUT /api/contacts/{id}` is called with `language = EN`
- **Then** the response is `400 Bad Request`
- **And** the error message mentions `language`

### Changing multiple protected fields lists all violations

- **Given** a contact with `brevoId = "200"`
- **When** `PUT /api/contacts/{id}` is called with changed `firstName` and `email`
- **Then** the response is `400 Bad Request`
- **And** the error message mentions both `firstName` and `email`

### Updating non-protected fields on Brevo contact succeeds

- **Given** a contact with `brevoId = "200"` and `position = "CEO"`
- **When** `PUT /api/contacts/{id}` is called with `position = "CTO"` (and protected fields unchanged)
- **Then** the response is `200 OK`
- **And** the contact's position is updated to "CTO"

### Sending unchanged protected fields is accepted

- **Given** a contact with `brevoId = "200"`, `firstName = "Anna"`, `lastName = "Schmidt"`
- **When** `PUT /api/contacts/{id}` is called with `firstName = "Anna"`, `lastName = "Schmidt"` (same values)
- **Then** the response is `200 OK`
- **And** the update succeeds

### Non-Brevo contact can change all fields freely

- **Given** a contact with `brevoId = NULL` and `firstName = "Bob"`
- **When** `PUT /api/contacts/{id}` is called with `firstName = "Robert"`
- **Then** the response is `200 OK`
- **And** the contact's firstName is updated to "Robert"

## Frontend — Edit Form

### Protected fields are disabled for Brevo contacts

- **Given** a Brevo contact (`brevo = true`) is being edited
- **When** the edit form renders
- **Then** the firstName, lastName, email, and language fields are visually disabled (greyed out)
- **And** the fields show the current values
- **And** the user cannot type in or change these fields

### Hint text is shown below each disabled field

- **Given** a Brevo contact (`brevo = true`) is being edited
- **When** the edit form renders
- **Then** each of the four disabled fields (firstName, lastName, email, language) has a small hint text below it reading "Managed by Brevo" (EN) or "Wird von Brevo verwaltet" (DE)

### Non-protected fields remain editable for Brevo contacts

- **Given** a Brevo contact (`brevo = true`) is being edited
- **When** the edit form renders
- **Then** position, phone, LinkedIn, birthday, company, gender, and photo fields are fully editable
- **And** no hint text is shown below these fields

### All fields are editable for non-Brevo contacts

- **Given** a non-Brevo contact (`brevo = false`) is being edited
- **When** the edit form renders
- **Then** all fields including firstName, lastName, email, and language are editable
- **And** no "Managed by Brevo" hint text is shown

### All fields are editable in create mode

- **Given** the user is creating a new contact
- **When** the create form renders
- **Then** all fields are editable
- **And** no "Managed by Brevo" hint text is shown

### Form submission works with disabled fields

- **Given** a Brevo contact is being edited
- **And** the user changes the position field from "CEO" to "CTO"
- **When** the user clicks save
- **Then** the form sends the full DTO including the unchanged protected field values
- **And** the update succeeds
- **And** the user is redirected to the contact detail view

## Edge Cases

### Brevo contact with null email can be updated without error

- **Given** a Brevo contact with `email = NULL`
- **When** `PUT /api/contacts/{id}` is called with `email = NULL` (unchanged)
- **Then** the response is `200 OK`

### Brevo contact with null language can be updated without error

- **Given** a Brevo contact with `language = NULL`
- **When** `PUT /api/contacts/{id}` is called with `language = NULL` (unchanged)
- **Then** the response is `200 OK`
