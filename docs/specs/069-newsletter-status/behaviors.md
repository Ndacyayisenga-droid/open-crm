# Behaviors: Newsletter Status on Contact

## Brevo Sync — Newsletter Mapping

### Contact with DOUBLE_OPT-IN and not blacklisted receives newsletter

- **Given** a Brevo contact with `DOUBLE_OPT-IN = 1` and `emailBlacklisted = false`
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is set to `true`

### Contact with DOUBLE_OPT-IN but blacklisted does not receive newsletter

- **Given** a Brevo contact with `DOUBLE_OPT-IN = 1` and `emailBlacklisted = true`
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is set to `false`

### Contact without DOUBLE_OPT-IN does not receive newsletter

- **Given** a Brevo contact without the `DOUBLE_OPT-IN` attribute (or `DOUBLE_OPT-IN = 2`)
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is set to `false` regardless of `emailBlacklisted`

### Contact with DOUBLE_OPT-IN set to No does not receive newsletter

- **Given** a Brevo contact with `DOUBLE_OPT-IN = 2` (No) and `emailBlacklisted = false`
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is set to `false`

## Brevo Sync — Re-Import

### Newsletter status is updated on every re-import

- **Given** a Brevo contact that previously had `receivesNewsletter = true`
- **And** the contact has since set `emailBlacklisted = true` in Brevo
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is updated to `false`

### Newsletter status is restored on re-import

- **Given** a Brevo contact that previously had `receivesNewsletter = false` (was blacklisted)
- **And** the contact has since set `emailBlacklisted = false` in Brevo
- **When** the Brevo sync runs
- **Then** the contact's `receivesNewsletter` is updated to `true` (assuming `DOUBLE_OPT-IN = 1`)

## Brevo Sync — Unlink

### Newsletter status is cleared on unlink

- **Given** a contact with `brevoId` set and `receivesNewsletter = true`
- **And** the contact no longer exists in Brevo
- **When** the Brevo sync runs (unlink phase)
- **Then** the contact's `brevoId` is set to `null`
- **And** the contact's `receivesNewsletter` is set to `false`

## Non-Brevo Contacts

### Non-Brevo contacts default to false

- **Given** a contact created manually in the CRM (no Brevo association)
- **When** the contact is viewed
- **Then** `receivesNewsletter` is `false`

### Non-Brevo contacts cannot have newsletter status changed

- **Given** a contact without a Brevo association
- **When** the contact is updated via the API
- **Then** the `receivesNewsletter` field is not part of the update DTO and cannot be set

## API

### ContactDto includes receivesNewsletter

- **Given** a contact with `receivesNewsletter = true`
- **When** the contact is fetched via `GET /api/contacts/{id}`
- **Then** the response includes `"receivesNewsletter": true`

### receivesNewsletter is not in create or update DTOs

- **Given** a client sends a POST or PUT request with a `receivesNewsletter` field in the body
- **When** the request is processed
- **Then** the `receivesNewsletter` field is ignored (not part of CreateDto/UpdateDto)

## UI — Contact Detail View

### Newsletter tag shown for subscribed contacts

- **Given** a contact with `receivesNewsletter = true`
- **When** the contact detail view is displayed
- **Then** a green "Newsletter" tag with a mail icon is shown next to the Brevo badge

### Newsletter tag hidden for non-subscribed contacts

- **Given** a contact with `receivesNewsletter = false`
- **When** the contact detail view is displayed
- **Then** no "Newsletter" tag is shown

### Newsletter tag not shown in list table

- **Given** a contact with `receivesNewsletter = true`
- **When** the contact list table is displayed
- **Then** there is no newsletter indicator in the table

## Edge Cases

### Contact with emailBlacklisted missing in API response

- **Given** a Brevo contact where `emailBlacklisted` is not present in the API response
- **When** the Brevo sync runs
- **Then** `emailBlacklisted` defaults to `false`
- **And** newsletter status is computed normally based on `DOUBLE_OPT-IN`

### DOUBLE_OPT-IN attribute with unexpected value

- **Given** a Brevo contact with `DOUBLE_OPT-IN` set to a value other than `1` or `2` (e.g., `0`, `3`, or a string)
- **When** the Brevo sync runs
- **Then** `receivesNewsletter` is set to `false`
