# Behaviors: Brevo Unlink on Deletion

## Contact Unlinking

### Contact removed from Brevo is unlinked

- **Given** Contact A exists in CRM with `brevoId = "123"`
- **When** a Brevo sync runs and Brevo does not return a contact with ID 123
- **Then** Contact A's `brevoId` is set to null

### Unlinked contact retains all field values

- **Given** Contact A has `brevoId = "123"`, firstName = "John", lastName = "Doe", email = "john@example.com", language = "DE"
- **When** Contact A is unlinked during sync
- **Then** `brevoId` is null, but firstName, lastName, email, and language remain unchanged

### Unlinked contact becomes fully editable

- **Given** Contact A was a Brevo contact with read-only firstName, lastName, email, language fields
- **When** Contact A is unlinked (brevoId set to null)
- **Then** all fields are editable in the frontend (no Brevo badge, no read-only restrictions)

### Contact still in Brevo is not unlinked

- **Given** Contact A exists in CRM with `brevoId = "123"`
- **When** a Brevo sync runs and Brevo returns a contact with ID 123
- **Then** Contact A's `brevoId` remains "123"

### Contact without brevoId is not affected

- **Given** Contact B exists in CRM without a `brevoId` (manually created)
- **When** a Brevo sync runs
- **Then** Contact B is not modified

### Multiple contacts unlinked in one sync

- **Given** Contacts A (brevoId = "1"), B (brevoId = "2"), C (brevoId = "3") exist in CRM
- **When** a Brevo sync runs and Brevo only returns contacts with IDs 1 and 3
- **Then** Contact B's `brevoId` is set to null, Contacts A and C remain linked

## Company Unlinking

### Company removed from Brevo is unlinked

- **Given** Company X exists in CRM with `brevoCompanyId = "abc"`
- **When** a Brevo sync runs and Brevo does not return a company with ID "abc"
- **Then** Company X's `brevoCompanyId` is set to null

### Unlinked company retains all field values

- **Given** Company X has `brevoCompanyId = "abc"`, name = "Acme", website = "acme.com"
- **When** Company X is unlinked during sync
- **Then** `brevoCompanyId` is null, but name and website remain unchanged

### Unlinked company loses Brevo badge

- **Given** Company X was a Brevo-linked company showing the Brevo badge
- **When** Company X is unlinked (brevoCompanyId set to null)
- **Then** the Brevo badge is no longer shown in the detail view

### Company still in Brevo is not unlinked

- **Given** Company X exists in CRM with `brevoCompanyId = "abc"`
- **When** a Brevo sync runs and Brevo returns a company with ID "abc"
- **Then** Company X's `brevoCompanyId` remains "abc"

## Sync Result Reporting

### Unlinked counts reported in result

- **Given** 2 contacts and 1 company are unlinked during sync
- **When** the sync completes
- **Then** the result contains `contactsUnlinked = 2` and `companiesUnlinked = 1`

### Zero unlinked when nothing changed

- **Given** all Brevo-linked CRM entries are still present in Brevo
- **When** the sync completes
- **Then** the result contains `contactsUnlinked = 0` and `companiesUnlinked = 0`

### Unlinked counts displayed in frontend

- **Given** a sync completed with `contactsUnlinked = 3` and `companiesUnlinked = 1`
- **When** the sync results are displayed in the UI
- **Then** "Firmen entkoppelt: 1" and "Kontakte entkoppelt: 3" are shown

### Unlinking does not appear in error list

- **Given** 2 contacts were unlinked during sync
- **When** the sync results are displayed
- **Then** the error list does not contain any entries related to unlinking

## Re-linking After Unlink

### Unlinked contact re-linked on next import via email match

- **Given** Contact A was unlinked (brevoId = null, email = "john@example.com")
- **When** a Brevo sync runs and Brevo returns a contact with email "john@example.com"
- **Then** Contact A's `brevoId` is set to the new Brevo contact ID

### Re-linked contact has Brevo-managed fields overwritten

- **Given** Contact A was unlinked and its firstName was manually changed to "Jonathan"
- **When** Contact A is re-linked via email matching and Brevo has firstName = "John"
- **Then** Contact A's firstName is set to "John" (Brevo-managed fields overwritten on re-import)

### Unlinked company re-linked on next import via name match

- **Given** Company X was unlinked (brevoCompanyId = null, name = "Acme")
- **When** a Brevo sync runs and Brevo returns a company with name "Acme"
- **Then** Company X's `brevoCompanyId` is set to the Brevo company ID

## Edge Cases

### First sync with no existing Brevo entries

- **Given** no CRM entries have a `brevoId` or `brevoCompanyId`
- **When** a Brevo sync runs for the first time
- **Then** no unlinking occurs and unlinked counts are both 0

### Sync with empty Brevo account

- **Given** 10 contacts and 5 companies are Brevo-linked in CRM
- **When** a Brevo sync runs and Brevo returns zero contacts and zero companies
- **Then** all 10 contacts and 5 companies are unlinked

### Failed contact processing does not cause false unlink

- **Given** Contact A has `brevoId = "123"` and Brevo returns contact 123
- **When** the sync fails to process contact 123 (e.g., validation error) but the ID was in the API response
- **Then** Contact A is NOT unlinked (the ID was in the seen set from the API response)
