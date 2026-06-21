# Behaviors: Fix Brevo Company ID Parsing

## Company ID Parsing

### Hex string IDs are parsed correctly

- **Given** the Brevo API returns a company with ID `"699309fa6f8c46a643a7b922"`
- **When** `BrevoApiClient.fetchAllCompanies()` processes this company
- **Then** the resulting `BrevoCompany` has `id = "699309fa6f8c46a643a7b922"`

### Numeric-looking IDs are parsed as strings

- **Given** the Brevo API returns a company with ID `"12345"`
- **When** `BrevoApiClient.fetchAllCompanies()` processes this company
- **Then** the resulting `BrevoCompany` has `id = "12345"` (String, not long)

### Contact IDs remain numeric

- **Given** the Brevo API returns a contact with numeric ID `130`
- **When** `BrevoApiClient.fetchAllContacts()` processes this contact
- **Then** the resulting `BrevoContact` has `id = 130` (long)

## Company Import — Distinct Companies

### Each Brevo company creates a separate database entry

- **Given** Brevo has 3 companies: "Alpha" (ID "aaa"), "Beta" (ID "bbb"), "Gamma" (ID "ccc")
- **And** the CRM database has no existing companies
- **When** a Brevo sync is triggered
- **Then** 3 new companies are created in the database
- **And** each has a distinct `brevo_company_id` ("aaa", "bbb", "ccc")
- **And** the sync result shows `companiesImported = 3, companiesUpdated = 0`

### Re-import matches by Brevo ID correctly

- **Given** a previous sync imported company "Alpha" with `brevo_company_id = "aaa"`
- **When** a second Brevo sync is triggered
- **And** Brevo still has company "Alpha" with ID "aaa"
- **Then** the existing company is updated (not duplicated)
- **And** the sync result shows `companiesImported = 0, companiesUpdated = 1`

### Companies with same name but different Brevo IDs are treated as separate

- **Given** Brevo has company "Acme" (ID "aaa") and company "Acme" (ID "bbb")
- **And** the CRM database has no existing companies
- **When** a Brevo sync is triggered
- **Then** 2 separate companies named "Acme" are created
- **And** each has its own `brevo_company_id`

## Contact-Company Association

### Contacts are assigned to their correct Brevo company

- **Given** Brevo has company "Alpha" (ID "aaa") with `linkedContactsIds = [10]`
- **And** Brevo has company "Beta" (ID "bbb") with `linkedContactsIds = [20]`
- **And** Brevo has contact ID 10 (email "a@test.com") and contact ID 20 (email "b@test.com")
- **When** a Brevo sync is triggered
- **Then** contact "a@test.com" is associated with company "Alpha"
- **And** contact "b@test.com" is associated with company "Beta"

### Contacts are not all assigned to the same company

- **Given** Brevo has 36 companies with distinct hex IDs
- **And** each company has at least 1 linked contact
- **When** a Brevo sync is triggered
- **Then** contacts are distributed across all 36 companies according to their `linkedContactsIds`
- **And** no single company has all contacts assigned to it

### FIRMA_MANUELL fallback still works with string company IDs

- **Given** a contact has attribute `FIRMA_MANUELL = "NewCorp"`
- **And** the contact is not linked to any Brevo company via `linkedContactsIds`
- **And** no company named "NewCorp" exists in the database
- **When** a Brevo sync is triggered
- **Then** a new company "NewCorp" is created (without `brevo_company_id`)
- **And** the contact is associated with it

## Database Migration

### Existing data is migrated correctly

- **Given** the `companies` table has `brevo_company_id` as `BIGINT`
- **And** one company has `brevo_company_id = 0` (the broken value)
- **When** migration V8 runs
- **Then** the column type changes to `VARCHAR(50)`
- **And** the company with `brevo_company_id = '0'` is reset to `NULL`
- **And** the unique index is preserved

### Unique constraint on brevo_company_id is enforced

- **Given** company "Alpha" has `brevo_company_id = "aaa"`
- **When** a new company is saved with `brevo_company_id = "aaa"`
- **Then** a unique constraint violation occurs

## Logging

### Company sync operations are logged

- **Given** a Brevo sync is triggered
- **When** company "Alpha" (Brevo ID "aaa") is synced
- **Then** the backend logs whether the company was created or updated
- **And** the log includes the Brevo ID and company name

### Company fetch from Brevo is logged at debug level

- **Given** a Brevo sync is triggered
- **When** companies are fetched from the Brevo API
- **Then** each fetched company (ID, name) is logged at DEBUG level

### Contact company resolution is logged at debug level

- **Given** a Brevo sync is triggered
- **When** a contact's company is resolved
- **Then** the log includes which company was matched (or null) and by which method (Brevo ID or FIRMA_MANUELL)

## Sync Result Accuracy

### Import counters match actual database state

- **Given** Brevo has 36 companies, none exist in the CRM
- **When** a Brevo sync is triggered
- **Then** the sync result shows `companiesImported = 36`
- **And** 36 distinct companies exist in the database

### Failed companies are counted correctly

- **Given** Brevo has a company with `name = null`
- **When** a Brevo sync is triggered
- **Then** the company sync fails (NOT NULL constraint)
- **And** `companiesFailed` is incremented
- **And** the error message is included in the result
