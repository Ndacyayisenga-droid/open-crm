# Behaviors: External Links Table

## Data Migration

### Existing Brevo company links are migrated

- **Given** a company exists with `brevo_company_id = "abc123"`
- **When** migration V22 runs
- **Then** an `external_links` row exists with `entity_type=COMPANY`, `entity_id=<company UUID>`, `system=BREVO`, `external_id="abc123"`
- **Then** the `brevo_company_id` column no longer exists on the `companies` table

### Existing Brevo contact links are migrated

- **Given** a contact exists with `brevo_id = "456def"`
- **When** migration V22 runs
- **Then** an `external_links` row exists with `entity_type=CONTACT`, `entity_id=<contact UUID>`, `system=BREVO`, `external_id="456def"`
- **Then** the `brevo_id` column no longer exists on the `contacts` table

### Entities without Brevo IDs are not migrated

- **Given** a company exists with `brevo_company_id = NULL`
- **When** migration V22 runs
- **Then** no `external_links` row exists for that company

### Newsletter field remains on contact

- **Given** a contact exists with `receives_newsletter = true` and `brevo_id = "123"`
- **When** migration V22 runs
- **Then** `receives_newsletter` remains `true` on the contact entity
- **Then** the Brevo link is in the `external_links` table

## External Link Storage

### Create link for entity

- **Given** a company exists without external links
- **When** `ExternalLinkService.createLink(COMPANY, companyId, BREVO, "abc123")` is called
- **Then** an `external_links` row is created with the correct values

### Prevent duplicate link per system

- **Given** a company already has a BREVO link with external ID "abc123"
- **When** `createLink(COMPANY, companyId, BREVO, "xyz789")` is called
- **Then** the existing link is updated with external ID "xyz789"
- **Then** only one BREVO link exists for that company

### One entity can have links from different systems

- **Given** a company has a BREVO link
- **When** a SEVDESK link is created for the same company
- **Then** both links exist in the `external_links` table
- **Then** `getExternalSources()` returns `["BREVO", "SEVDESK"]`

### Remove link for specific system

- **Given** a company has a BREVO link
- **When** `removeLink(COMPANY, companyId, BREVO)` is called
- **Then** the BREVO link is deleted
- **Then** links from other systems (if any) remain

### Remove all links on entity deletion

- **Given** a company has links to BREVO and SEVDESK
- **When** the company is deleted
- **Then** all `external_links` rows for that company are deleted

## Brevo Sync — Company Import (migrated behavior)

### Import new company from Brevo

- **Given** a Brevo company "Acme Corp" does not exist in the CRM
- **When** the Brevo sync runs
- **Then** a new company "Acme Corp" is created
- **Then** an external link `BREVO` is created for that company

### Update existing Brevo-linked company

- **Given** a company exists with a BREVO external link (external ID "abc")
- **Given** the Brevo company "abc" has updated its domain
- **When** the Brevo sync runs
- **Then** the company's website is updated
- **Then** the external link remains unchanged

### Match company by name when no link exists

- **Given** a company "Acme Corp" exists without any external link
- **Given** a Brevo company named "acme corp" exists (case-insensitive match)
- **When** the Brevo sync runs
- **Then** the existing company is linked to Brevo (new external link created)
- **Then** no duplicate company is created

### Skip company already linked to another system

- **Given** a company "Acme Corp" exists with a SEVDESK external link
- **Given** a Brevo company named "Acme Corp" exists
- **When** the Brevo sync runs
- **Then** the company is skipped
- **Then** the sync result counts it as failed
- **Then** an error message indicates the company is already linked to another system

## Brevo Sync — Contact Import (migrated behavior)

### Import new contact from Brevo

- **Given** a Brevo contact with email "alice@example.com" does not exist in the CRM
- **When** the Brevo sync runs
- **Then** a new contact is created
- **Then** an external link `BREVO` is created for that contact

### Match contact by email when no link exists

- **Given** a contact with email "alice@example.com" exists without any external link
- **When** the Brevo sync imports a contact with the same email
- **Then** the existing contact is linked to Brevo
- **Then** no duplicate contact is created

### Skip contact already linked to another system

- **Given** a contact exists with a SEVDESK external link
- **Given** a Brevo contact with the same email exists
- **When** the Brevo sync runs
- **Then** the contact is skipped
- **Then** the sync result counts it as failed with an appropriate error message

## Brevo Sync — Unlink (migrated behavior)

### Unlink company removed from Brevo

- **Given** a company has a BREVO external link with ID "abc"
- **Given** the Brevo API no longer returns a company with ID "abc"
- **When** the Brevo sync runs
- **Then** the BREVO external link is deleted
- **Then** the company itself remains in the CRM
- **Then** the sync result counts it as unlinked

### Unlink contact removed from Brevo

- **Given** a contact has a BREVO external link with ID "456"
- **Given** the Brevo API no longer returns a contact with ID "456"
- **When** the Brevo sync runs
- **Then** the BREVO external link is deleted
- **Then** `receivesNewsletter` is set to `false`
- **Then** the contact itself remains in the CRM

## REST API — External Source Filter

### Filter companies by external source

- **Given** 3 companies exist: one linked to BREVO, one linked to SEVDESK, one with no links
- **When** `GET /api/companies?externalSource=BREVO` is called
- **Then** only the BREVO-linked company is returned

### Filter for no external source

- **Given** 3 companies exist: one linked to BREVO, one linked to SEVDESK, one with no links
- **When** `GET /api/companies?externalSource=NONE` is called
- **Then** only the unlinked company is returned

### No filter returns all

- **Given** 3 companies exist with mixed external links
- **When** `GET /api/companies` is called without `externalSource` parameter
- **Then** all 3 companies are returned

### Filter applies to contacts equally

- **Given** contacts with mixed external links exist
- **When** `GET /api/contacts?externalSource=BREVO` is called
- **Then** only BREVO-linked contacts are returned

### Invalid external source returns 400

- **Given** any request
- **When** `GET /api/companies?externalSource=INVALID` is called
- **Then** the response status is 400 Bad Request

### CSV export respects external source filter

- **Given** companies with mixed external links exist
- **When** `GET /api/companies/export?externalSource=BREVO` is called
- **Then** only BREVO-linked companies appear in the CSV

## DTO — External Sources Field

### Company with Brevo link exposes external sources

- **Given** a company has a BREVO external link
- **When** the company is retrieved via `GET /api/companies/{id}`
- **Then** `externalSources` contains `["BREVO"]`

### Company without links has empty external sources

- **Given** a company has no external links
- **When** the company is retrieved
- **Then** `externalSources` is `[]`

### Contact external sources work the same way

- **Given** a contact has a BREVO external link
- **When** the contact is retrieved
- **Then** `externalSources` contains `["BREVO"]`

## Readonly Field Protection

### Brevo-linked contact has readonly fields

- **Given** a contact has a BREVO external link
- **When** `PUT /api/contacts/{id}` attempts to change `firstName`
- **Then** the response status is 400 Bad Request
- **Then** the error message indicates the field is managed by Brevo

### Unlinked contact fields are editable

- **Given** a contact previously had a BREVO link that was removed (unlinked)
- **When** `PUT /api/contacts/{id}` changes `firstName`
- **Then** the response status is 200 OK
- **Then** the change is persisted

### Readonly fields depend on the external system

- **Given** a contact has a BREVO external link
- **Then** readonly fields are: firstName, lastName, email, language
- **Given** a contact has a SEVDESK external link (future)
- **Then** readonly fields would be: title, firstName, lastName, email, phone, position

## Frontend — External Source Badges

### Brevo badge on company detail

- **Given** a company has `externalSources: ["BREVO"]`
- **When** the company detail view is displayed
- **Then** a "Brevo" badge is shown below the name

### No badge when no external sources

- **Given** a company has `externalSources: []`
- **When** the company detail view is displayed
- **Then** no external source badge is shown

### Multiple badges if multiple sources

- **Given** a company has `externalSources: ["BREVO", "SEVDESK"]`
- **When** the company detail view is displayed
- **Then** both "Brevo" and "SevDesk" badges are shown

## Frontend — External Source Filter

### Filter dropdown shows external source options

- **Given** the company list is displayed
- **When** the user opens the external source filter
- **Then** options include: "All", "From Brevo", "No external source"

### Filter applies to table and pagination

- **Given** the user selects "From Brevo" in the filter
- **When** the table refreshes
- **Then** only Brevo-linked companies are shown
- **Then** the total count reflects the filtered result

### Filter passes to print and CSV export

- **Given** "From Brevo" filter is active
- **When** the user clicks print or CSV export
- **Then** only Brevo-linked companies appear in the output

## Frontend — Contact Form Readonly

### External contact shows managed hint

- **Given** a contact has `externalSources: ["BREVO"]`
- **When** the edit form is opened
- **Then** firstName, lastName, email, language fields are disabled
- **Then** each shows a hint "Managed by Brevo"

### Non-external contact form is fully editable

- **Given** a contact has `externalSources: []`
- **When** the edit form is opened
- **Then** all fields are editable
- **Then** no "Managed by" hints are shown
