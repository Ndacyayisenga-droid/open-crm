# Behaviors: Remove Double Opt-In Flag from Contacts

## Database Migration

### Column is dropped cleanly

- **Given** the `contacts` table has a `double_opt_in` column
- **When** migration V9 runs
- **Then** the `double_opt_in` column no longer exists
- **And** no data loss occurs on other columns

## API Response

### Contact DTO no longer contains doubleOptIn

- **Given** a contact exists in the database
- **When** `GET /api/contacts/{id}` is called
- **Then** the response JSON does not contain a `doubleOptIn` field

### Contact list DTO no longer contains doubleOptIn

- **Given** contacts exist in the database
- **When** `GET /api/contacts` is called
- **Then** no contact object in the response contains a `doubleOptIn` field

## Frontend

### Contact detail view does not show Double Opt-In checkbox

- **Given** a contact exists
- **When** the user navigates to the contact detail view
- **Then** no "Double Opt-In" checkbox is displayed
- **And** the "Synced to Brevo" checkbox is still displayed (unaffected)

## Brevo Import

### Brevo sync ignores DOUBLE_OPT-IN attribute

- **Given** a Brevo contact has attribute `DOUBLE_OPT-IN = true`
- **When** a Brevo sync is triggered
- **Then** the contact is imported successfully
- **And** no `doubleOptIn` field is set on the contact entity
- **And** the sync does not fail or produce an error

### Other Brevo attributes are still mapped correctly

- **Given** a Brevo contact has attributes `VORNAME`, `NACHNAME`, `SPRACHE`, and `DOUBLE_OPT-IN`
- **When** a Brevo sync is triggered
- **Then** `VORNAME`, `NACHNAME`, and `SPRACHE` are mapped correctly
- **And** `DOUBLE_OPT-IN` is silently ignored

## No Side Effects

### syncedToBrevo flag is unaffected

- **Given** a contact was imported from Brevo with `syncedToBrevo = true`
- **When** the migration runs and the application restarts
- **Then** the contact still has `syncedToBrevo = true`

### Existing contacts remain intact

- **Given** contacts exist with various data (name, email, company, etc.)
- **When** the migration drops the `double_opt_in` column
- **Then** all other contact data remains unchanged
