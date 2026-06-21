# Behaviors: Brevo Import

## Settings Management

### Save API key successfully

- **Given** no API key is configured
- **When** the user submits a valid Brevo API key via PUT /api/brevo/settings
- **Then** the key is stored in the settings table under key `brevo.api-key`
- **Then** the response contains `{ apiKeyConfigured: true }`
- **Then** the key is validated against Brevo GET /account before saving

### Save invalid API key

- **Given** no API key is configured
- **When** the user submits an invalid Brevo API key via PUT /api/brevo/settings
- **Then** the Brevo GET /account validation call returns 401
- **Then** the response is 400 with an error message indicating the key is invalid
- **Then** no key is stored in the settings table

### Save blank API key

- **Given** no API key is configured
- **When** the user submits a blank API key via PUT /api/brevo/settings
- **Then** the response is 400 with a validation error
- **Then** no key is stored in the settings table

### Update existing API key

- **Given** an API key is already configured
- **When** the user submits a new valid API key via PUT /api/brevo/settings
- **Then** the stored key is replaced with the new one
- **Then** the response contains `{ apiKeyConfigured: true }`

### Remove API key

- **Given** an API key is configured
- **When** the user sends DELETE /api/brevo/settings
- **Then** the key is removed from the settings table
- **Then** the response is 204 No Content

### Check settings when key is configured

- **Given** an API key is stored in the settings table
- **When** the user sends GET /api/brevo/settings
- **Then** the response contains `{ apiKeyConfigured: true }`
- **Then** the actual API key value is NOT returned

### Check settings when no key is configured

- **Given** no API key is stored
- **When** the user sends GET /api/brevo/settings
- **Then** the response contains `{ apiKeyConfigured: false }`

---

## Company Import

### Import new company from Brevo CRM

- **Given** Brevo has a company with id=100, name="Acme Corp", domain="acme.com"
- **Given** no company with brevoCompanyId=100 exists in the CRM
- **Given** no company named "Acme Corp" exists in the CRM
- **When** the import runs
- **Then** a new CompanyEntity is created with name="Acme Corp", website="acme.com", brevoCompanyId=100

### Update existing company matched by Brevo ID

- **Given** Brevo has a company with id=100, name="Acme Corp Updated", domain="acme.io"
- **Given** a CRM company exists with brevoCompanyId=100, name="Acme Corp", website="acme.com"
- **When** the import runs
- **Then** the existing company is updated: name="Acme Corp Updated", website="acme.io"

### First import matches company by name

- **Given** Brevo has a company with id=100, name="Acme Corp", domain="acme.com"
- **Given** a CRM company exists with name="Acme Corp", brevoCompanyId=null
- **When** the import runs
- **Then** the existing company is updated: website="acme.com", brevoCompanyId=100
- **Then** no duplicate company is created

### Company name matching is case-insensitive

- **Given** Brevo has a company with id=100, name="ACME CORP"
- **Given** a CRM company exists with name="Acme Corp", brevoCompanyId=null
- **When** the import runs
- **Then** the existing company is matched and updated with brevoCompanyId=100

### Import company without domain

- **Given** Brevo has a company with id=100, name="Acme Corp", domain=null
- **When** the import runs
- **Then** a new CompanyEntity is created with name="Acme Corp", website=null

### CRM-only companies are untouched

- **Given** a CRM company "Local Only GmbH" exists with brevoCompanyId=null
- **Given** no Brevo company with that name exists
- **When** the import runs
- **Then** the company "Local Only GmbH" remains unchanged

---

## Contact Import

### Import new contact from Brevo

- **Given** Brevo has a contact with id=200, VORNAME="John", NACHNAME="Doe", E-MAIL="john@example.com", SMS="+49123456", JOB_TITLE="CTO", LINKEDIN="https://linkedin.com/in/johndoe", SPRACHE=1, DOUBLE_OPT-IN=true
- **Given** no contact with brevoId=200 or email="john@example.com" exists in the CRM
- **When** the import runs
- **Then** a new ContactEntity is created with firstName="John", lastName="Doe", email="john@example.com", phoneNumber="+49123456", position="CTO", linkedInUrl="https://linkedin.com/in/johndoe", language=DE, doubleOptIn=true, syncedToBrevo=true, brevoId=200

### Update existing contact matched by Brevo ID

- **Given** Brevo has a contact with id=200, VORNAME="Jane", NACHNAME="Smith"
- **Given** a CRM contact exists with brevoId=200, firstName="John", lastName="Doe"
- **When** the import runs
- **Then** the existing contact is updated: firstName="Jane", lastName="Smith"
- **Then** Brevo data overwrites CRM data

### First import matches contact by email

- **Given** Brevo has a contact with id=200, E-MAIL="john@example.com", VORNAME="John"
- **Given** a CRM contact exists with email="john@example.com", brevoId=null
- **When** the import runs
- **Then** the existing contact is updated with brevoId=200 and field values from Brevo
- **Then** no duplicate contact is created

### Contact email matching is case-insensitive

- **Given** Brevo has a contact with id=200, E-MAIL="John@Example.COM"
- **Given** a CRM contact exists with email="john@example.com", brevoId=null
- **When** the import runs
- **Then** the existing contact is matched and updated with brevoId=200

### Import contact with SPRACHE=Deutsch

- **Given** Brevo has a contact with SPRACHE=1
- **When** the import runs
- **Then** the contact's language is set to DE

### Import contact with SPRACHE=Englisch

- **Given** Brevo has a contact with SPRACHE=2
- **When** the import runs
- **Then** the contact's language is set to EN

### Import contact with SPRACHE=Unbekannt

- **Given** Brevo has a contact with SPRACHE=3
- **When** the import runs
- **Then** the contact's language is set to null

### Import contact with SPRACHE not set

- **Given** Brevo has a contact with no SPRACHE attribute
- **When** the import runs
- **Then** the contact's language is set to null

### Import sets syncedToBrevo flag

- **Given** Brevo has a contact with id=200
- **When** the import runs
- **Then** the contact's syncedToBrevo is set to true

### CRM-only contacts are untouched

- **Given** a CRM contact "Local Person" exists with brevoId=null
- **Given** no Brevo contact has a matching email
- **When** the import runs
- **Then** the contact "Local Person" remains unchanged

---

## Company-Contact Association

### Contact linked to Brevo CRM company

- **Given** Brevo company id=100 ("Acme Corp") has linkedContactsIds=[200]
- **Given** Brevo contact id=200 exists
- **When** the import runs
- **Then** the CRM contact (brevoId=200) is associated with the CRM company (brevoCompanyId=100)

### Contact with FIRMA_MANUELL and no CRM company link

- **Given** Brevo contact id=200 has FIRMA_MANUELL="Startup XYZ"
- **Given** Brevo contact id=200 is NOT in any Brevo company's linkedContactsIds
- **When** the import runs
- **Then** a new CompanyEntity is created with name="Startup XYZ"
- **Then** the contact is associated with this new company

### CRM company link takes priority over FIRMA_MANUELL

- **Given** Brevo company id=100 ("Acme Corp") has linkedContactsIds=[200]
- **Given** Brevo contact id=200 has FIRMA_MANUELL="Different Company"
- **When** the import runs
- **Then** the contact is associated with "Acme Corp" (brevoCompanyId=100)
- **Then** FIRMA_MANUELL is ignored

### FIRMA_MANUELL always creates new company

- **Given** Brevo contact id=200 has FIRMA_MANUELL="Existing Corp"
- **Given** a CRM company named "Existing Corp" already exists
- **Given** Brevo contact id=200 is NOT linked to any Brevo CRM company
- **When** the import runs
- **Then** a new CompanyEntity is created with name="Existing Corp"
- **Then** the contact is associated with the newly created company (not the existing one)

### Contact with neither CRM company link nor FIRMA_MANUELL

- **Given** Brevo contact id=200 is NOT in any Brevo company's linkedContactsIds
- **Given** Brevo contact id=200 has no FIRMA_MANUELL attribute (or it is empty)
- **When** the import runs
- **Then** the contact has no company association (company=null)

### FIRMA_MANUELL with empty string

- **Given** Brevo contact id=200 has FIRMA_MANUELL="" (empty string)
- **Given** Brevo contact id=200 is NOT linked to any Brevo CRM company
- **When** the import runs
- **Then** no company is created from the empty string
- **Then** the contact has no company association

---

## Error Handling

### Sync without API key

- **Given** no API key is configured
- **When** the user triggers POST /api/brevo/sync
- **Then** the response is 400 with message "Brevo API key not configured"
- **Then** no import is started

### Concurrent sync prevention

- **Given** a sync is already in progress
- **When** the user triggers another POST /api/brevo/sync
- **Then** the response is 409 Conflict
- **Then** the running sync is not interrupted

### Invalid API key during sync

- **Given** an API key is configured but has been revoked in Brevo
- **When** the user triggers POST /api/brevo/sync
- **Then** the Brevo API returns 401
- **Then** the sync aborts immediately with an error message

### Contact missing required fields

- **Given** Brevo has a contact with id=200, VORNAME=null, NACHNAME=null
- **When** the import runs
- **Then** this contact is skipped
- **Then** the error is recorded in the result's errors list
- **Then** the import continues with the next contact

### Brevo API rate limit hit

- **Given** the Brevo API returns 429 Too Many Requests
- **When** the import encounters this response
- **Then** the client retries after the time indicated by `x-sib-ratelimit-reset`
- **Then** the client retries up to 3 times before recording an error

### Brevo API server error

- **Given** the Brevo API returns 500
- **When** the import encounters this response
- **Then** the client retries up to 3 times with exponential backoff (1s, 2s, 4s)
- **Then** if all retries fail, the error is recorded and the import continues

### Partial failure preserves successful imports

- **Given** Brevo has 100 contacts
- **Given** contact #50 has invalid data that causes an error
- **When** the import runs
- **Then** contacts #1-49 are successfully committed to the database
- **Then** contacts #51-100 are processed normally
- **Then** the result shows 99 imported/updated and 1 failed

---

## Import Result Summary

### Successful full import

- **Given** Brevo has 10 companies and 50 contacts, none exist in CRM
- **When** the import completes successfully
- **Then** the result shows companiesImported=10, contactsImported=50
- **Then** companiesUpdated=0, companiesFailed=0, contactsUpdated=0, contactsFailed=0
- **Then** errors list is empty

### Mixed import with updates

- **Given** Brevo has 10 companies: 3 new, 7 already in CRM (matched by brevoCompanyId)
- **Given** Brevo has 50 contacts: 20 new, 30 already in CRM (matched by brevoId)
- **When** the import completes
- **Then** the result shows companiesImported=3, companiesUpdated=7, contactsImported=20, contactsUpdated=30

### Import with failures

- **Given** Brevo has 50 contacts: 2 have neither VORNAME nor NACHNAME
- **When** the import completes
- **Then** the result shows contactsImported+contactsUpdated=48, contactsFailed=2
- **Then** errors list contains 2 descriptive error messages

---

## Frontend UI

### Settings card shows configured state

- **Given** an API key is configured (GET /api/brevo/settings returns apiKeyConfigured=true)
- **When** the Brevo Sync page loads
- **Then** the settings card shows a green "API Key configured" badge
- **Then** "Change" and "Remove" buttons are visible
- **Then** no input field is shown

### Settings card shows unconfigured state

- **Given** no API key is configured (GET /api/brevo/settings returns apiKeyConfigured=false)
- **When** the Brevo Sync page loads
- **Then** the settings card shows a password input field with placeholder text
- **Then** a "Save" button is visible
- **Then** the "Start Import" button in the sync card is disabled

### Sync button disabled without API key

- **Given** no API key is configured
- **When** the Brevo Sync page loads
- **Then** the "Start Import" button is disabled
- **Then** a hint message "Configure an API key first" is shown

### Sync in progress shows loading state

- **Given** an API key is configured
- **When** the user clicks "Start Import"
- **Then** the button becomes disabled
- **Then** a spinner and "Import running... This may take several minutes." text is shown

### Sync result is displayed

- **Given** an import has completed
- **When** the result is received
- **Then** the result summary shows counts for companies and contacts (imported, updated, failed)
- **Then** if errors exist, they are shown in an expandable list
