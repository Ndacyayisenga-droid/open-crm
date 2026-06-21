# Behaviors: SevDesk Integration

## API Key Configuration

### Save valid API key

- **Given** the user is on the SevDesk admin page
- **When** the user enters a valid SevDesk API key and clicks Save
- **Then** the key is validated against the SevDesk API (`GET /Account`)
- **Then** the key is stored in the settings table as `sevdesk.api-key`
- **Then** the status badge shows "Configured"

### Save invalid API key

- **Given** the user enters an invalid API key
- **When** the user clicks Save
- **Then** the SevDesk API validation fails
- **Then** an error message is displayed
- **Then** the key is not stored

### Delete API key

- **Given** a SevDesk API key is configured
- **When** the user clicks Delete
- **Then** the key is removed from the settings table
- **Then** the status badge shows "Not configured"
- **Then** the Sync button is disabled

### Check settings status

- **Given** a SevDesk API key is configured
- **When** `GET /api/sevdesk/settings` is called
- **Then** the response contains `apiKeyConfigured: true`

### Check settings when not configured

- **Given** no SevDesk API key is stored
- **When** `GET /api/sevdesk/settings` is called
- **Then** the response contains `apiKeyConfigured: false`

## Company Import

### Import new company from SevDesk

- **Given** a SevDesk company "Acme GmbH" does not exist in the CRM
- **When** the SevDesk sync runs
- **Then** a new company is created with name "Acme GmbH"
- **Then** an external link `SEVDESK` is created for that company
- **Then** the sync result counts it as imported

### Import company with full details

- **Given** a SevDesk company has name, email, website, phone, and main address
- **When** the sync runs
- **Then** all fields are mapped: name, email, website, phoneNumber, street, houseNumber, zipCode, city, country
- **Then** the company detail view shows all imported data

### Update existing SevDesk-linked company

- **Given** a company exists with a SEVDESK external link
- **Given** the SevDesk company has updated its phone number
- **When** the sync runs
- **Then** the company's phoneNumber is updated in the CRM
- **Then** the sync result counts it as updated

### Match company by name when no link exists

- **Given** a company "Acme GmbH" exists in the CRM without external links
- **Given** a SevDesk company named "acme gmbh" exists (case-insensitive)
- **When** the sync runs
- **Then** the existing company is linked to SevDesk
- **Then** the company fields are updated from SevDesk
- **Then** no duplicate company is created

### Skip company already linked to Brevo

- **Given** a company "Acme GmbH" exists with a BREVO external link
- **Given** a SevDesk company named "Acme GmbH" exists
- **When** the sync runs
- **Then** the company is skipped
- **Then** the sync result counts it as failed
- **Then** the error list contains: "Company 'Acme GmbH' skipped: already linked to BREVO"

### Company without address

- **Given** a SevDesk company has no main address configured
- **When** the sync runs
- **Then** the company is imported with address fields set to null
- **Then** the import is successful (address is optional)

### Company without email or website

- **Given** a SevDesk company has no email and no website
- **When** the sync runs
- **Then** the company is imported with email and website set to null
- **Then** the import is successful

## Person (Contact) Import

### Import new person from SevDesk

- **Given** a SevDesk person "Alice Schmidt" with email "alice@acme.de" belongs to company "Acme GmbH"
- **Given** "Acme GmbH" was successfully imported in Phase 1
- **When** the sync runs Phase 2
- **Then** a new contact is created with firstName, lastName, email
- **Then** the contact is associated with the CRM company "Acme GmbH"
- **Then** an external link `SEVDESK` is created for that contact

### Import person with full details

- **Given** a SevDesk person has title "Dr.", first name, last name, email, phone, and description (used as position)
- **When** the sync runs
- **Then** all fields are mapped: title, firstName, lastName, email, phoneNumber, position
- **Then** the contact detail view shows all imported data

### Match person by email when no link exists

- **Given** a contact with email "alice@acme.de" exists without external links
- **Given** a SevDesk person with the same email exists
- **When** the sync runs
- **Then** the existing contact is linked to SevDesk
- **Then** no duplicate contact is created

### Skip person without email

- **Given** a SevDesk person "Bob Müller" has no email address
- **When** the sync runs
- **Then** the person is skipped
- **Then** the sync result counts it as failed
- **Then** the error list contains: "Contact 'Bob Müller' skipped: no email for matching"

### Skip person already linked to Brevo

- **Given** a contact with email "alice@acme.de" has a BREVO external link
- **Given** a SevDesk person with the same email exists
- **When** the sync runs
- **Then** the contact is skipped
- **Then** the sync result counts it as failed
- **Then** the error list contains a message about the Brevo conflict

### Skip person when parent company was skipped

- **Given** SevDesk company "Acme GmbH" was skipped because it's already linked to Brevo
- **Given** SevDesk person "Alice Schmidt" belongs to "Acme GmbH"
- **When** the sync runs Phase 2
- **Then** the person is skipped
- **Then** the sync result counts it as failed
- **Then** the error list contains: "Contact 'Alice Schmidt' skipped: parent company 'Acme GmbH' could not be imported (cascading error)"

### Update existing SevDesk-linked person

- **Given** a contact exists with a SEVDESK external link
- **Given** the SevDesk person has updated their phone number
- **When** the sync runs
- **Then** the contact's phoneNumber is updated
- **Then** the sync result counts it as updated

### Person company association is preserved

- **Given** a SevDesk person belongs to SevDesk company "Acme GmbH"
- **Given** "Acme GmbH" was imported and has CRM ID "uuid-123"
- **When** the person is imported
- **Then** the CRM contact's `companyId` is set to "uuid-123"

## Unlink

### Unlink company removed from SevDesk

- **Given** a company has a SEVDESK external link with ID "sev-100"
- **Given** the SevDesk API no longer returns company "sev-100"
- **When** the sync runs
- **Then** the SEVDESK external link is removed
- **Then** the company remains in the CRM (now editable)
- **Then** the sync result counts it as unlinked

### Unlink person removed from SevDesk

- **Given** a contact has a SEVDESK external link with ID "sev-200"
- **Given** the SevDesk API no longer returns person "sev-200"
- **When** the sync runs
- **Then** the SEVDESK external link is removed
- **Then** the contact remains in the CRM (now editable)
- **Then** the sync result counts it as unlinked

## Readonly Field Protection

### SevDesk company fields are readonly

- **Given** a company has a SEVDESK external link
- **When** `PUT /api/companies/{id}` attempts to change `name`
- **Then** the response status is 400 Bad Request
- **Then** the error message indicates the field is managed by SevDesk

### SevDesk contact fields are readonly

- **Given** a contact has a SEVDESK external link
- **When** `PUT /api/contacts/{id}` attempts to change `firstName`
- **Then** the response status is 400 Bad Request
- **Then** the error message indicates the field is managed by SevDesk

### Non-readonly fields remain editable

- **Given** a contact has a SEVDESK external link
- **When** `PUT /api/contacts/{id}` changes `linkedInUrl` (not managed by SevDesk)
- **Then** the response status is 200 OK
- **Then** the change is persisted

### Unlinked entity becomes fully editable

- **Given** a company previously had a SEVDESK link that was removed
- **When** `PUT /api/companies/{id}` changes `name`
- **Then** the response status is 200 OK

## Frontend — SevDesk Admin Page

### Admin page shows settings and sync cards

- **Given** the user navigates to `/admin/sevdesk`
- **Then** a Settings card with API key input is shown
- **Then** a Sync card with "Start Sync" button is shown
- **Then** the Sync card is disabled until the API key is configured

### Sync results display

- **Given** a SevDesk sync completes with 5 companies imported, 2 contacts failed
- **When** the results are displayed
- **Then** a grid shows all counters (imported, updated, failed, unlinked) for companies and contacts
- **Then** the error list shows the 2 failure reasons

### Concurrent sync prevention

- **Given** a SevDesk sync is already in progress
- **When** the user clicks "Start Sync" again
- **Then** the server returns 409 Conflict
- **Then** an appropriate message is shown

## Frontend — Sidebar

### SevDesk appears in Admin sub-menu

- **Given** the user is logged in
- **When** the Admin sub-menu is expanded
- **Then** "SevDesk Integration" is listed as a sub-item
- **Then** clicking it navigates to `/admin/sevdesk`

## Frontend — External Source Filter

### SevDesk option in filter dropdown

- **Given** the company list is displayed
- **When** the user opens the external source filter
- **Then** "From SevDesk" is available as an option
- **When** the user selects "From SevDesk"
- **Then** only SevDesk-linked companies are shown

## Frontend — Badges and Form

### SevDesk badge on detail view

- **Given** a company has `externalSources: ["SEVDESK"]`
- **When** the company detail view is displayed
- **Then** a "SevDesk" badge is shown below the name

### SevDesk contact form readonly hint

- **Given** a contact has `externalSources: ["SEVDESK"]`
- **When** the edit form is opened
- **Then** title, firstName, lastName, email, phoneNumber, position fields are disabled
- **Then** each shows a hint "Managed by SevDesk"
- **Then** other fields (linkedInUrl, gender, birthday, etc.) remain editable

## Error Scenarios

### SevDesk API unreachable

- **Given** the SevDesk API is unreachable
- **When** the sync is triggered
- **Then** the sync fails with an error message
- **Then** no partial data is imported

### Rate limited by SevDesk

- **Given** the SevDesk API returns 429 (Too Many Requests)
- **When** a request is rate limited
- **Then** the client retries with exponential backoff (up to 3 times)
- **Then** if all retries fail, the sync reports the error

### API key removed during sync

- **Given** a sync is in progress
- **Given** the API key is deleted via another session
- **When** the next SevDesk API call is made
- **Then** the call fails with 401
- **Then** the sync stops and reports the error
