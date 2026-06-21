# Behaviors: Replace syncedToBrevo Flag with String brevoId on Contacts

## Database Migration

### Column type is changed and old column is dropped

- **Given** the `contacts` table has `brevo_id BIGINT` and `synced_to_brevo BOOLEAN`
- **When** the migration runs
- **Then** `brevo_id` is of type `VARCHAR(50)`
- **And** the `synced_to_brevo` column no longer exists
- **And** the unique index on `brevo_id` is preserved

### Existing numeric brevo_id values are preserved as strings

- **Given** a contact has `brevo_id = 130` (BIGINT)
- **When** the migration runs
- **Then** the contact has `brevo_id = '130'` (VARCHAR)

### Null brevo_id values remain null

- **Given** a contact has `brevo_id = NULL`
- **When** the migration runs
- **Then** `brevo_id` remains `NULL`

## API Response

### Contact with brevoId shows brevo = true

- **Given** a contact was imported from Brevo and has `brevo_id = '130'`
- **When** `GET /api/contacts/{id}` is called
- **Then** the response contains `"brevo": true`
- **And** the response does not contain a `brevoId` field
- **And** the response does not contain a `syncedToBrevo` field

### Contact without brevoId shows brevo = false

- **Given** a contact was created manually (no Brevo sync)
- **When** `GET /api/contacts/{id}` is called
- **Then** the response contains `"brevo": false`

### Contact list also uses brevo field

- **Given** contacts exist, some with brevoId and some without
- **When** `GET /api/contacts` is called
- **Then** each contact in the response has a `brevo` field (true or false)
- **And** no contact has a `syncedToBrevo` or `brevoId` field

## Frontend

### Synced to Brevo status is displayed for Brevo contacts

- **Given** a contact has `brevo = true`
- **When** the user views the contact detail page
- **Then** "Synced to Brevo" (EN) / "Mit Brevo synchronisiert" (DE) is displayed as checked

### Non-Brevo contacts do not show synced status

- **Given** a contact has `brevo = false`
- **When** the user views the contact detail page
- **Then** "Synced to Brevo" is displayed as unchecked

## Brevo Import

### New contacts get string brevoId

- **Given** a Brevo contact with numeric ID `200` does not exist in the CRM
- **When** a Brevo sync is triggered
- **Then** a new contact is created with `brevo_id = '200'`
- **And** the API returns `brevo = true` for this contact

### Re-import matches by string brevoId

- **Given** a contact exists with `brevo_id = '200'`
- **When** a Brevo sync returns a contact with ID `200`
- **Then** the existing contact is updated (not duplicated)

### syncedToBrevo is no longer set during import

- **Given** a Brevo sync is triggered
- **When** a contact is imported
- **Then** the sync service does not call `setSyncedToBrevo` (field no longer exists)
- **And** the contact's Brevo status is determined solely by `brevoId != null`

## Edge Cases

### Contact with brevoId = '0' is treated as synced

- **Given** a contact somehow has `brevo_id = '0'`
- **When** `GET /api/contacts/{id}` is called
- **Then** the response contains `"brevo": true` (non-null string, even if '0')

### Unique constraint prevents duplicate brevoIds

- **Given** a contact has `brevo_id = '200'`
- **When** another contact is saved with `brevo_id = '200'`
- **Then** a unique constraint violation occurs
