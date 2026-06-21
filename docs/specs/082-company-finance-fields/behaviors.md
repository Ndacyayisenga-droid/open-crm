# Behaviors: Company Financial Fields

## Company Creation

### Create company with all financial fields

- **Given** the user is on the company create form
- **When** the user fills in Bank "Deutsche Bank", BIC "DEUTDEDB", IBAN "DE89370400440532013000", VAT ID "DE123456789" and submits
- **Then** the company is created with all four financial fields stored
- **Then** the detail view shows the "Finanzen" section with all four values

### Create company without financial fields

- **Given** the user is on the company create form
- **When** the user leaves all financial fields empty and submits
- **Then** the company is created with all four fields as null
- **Then** the detail view does not show the "Finanzen" section

### Create company with partial financial fields

- **Given** the user is on the company create form
- **When** the user fills in only IBAN and leaves Bank, BIC, VAT ID empty
- **Then** the company is created with only IBAN set
- **Then** the "Finanzen" section shows only the IBAN field

## IBAN Validation

### Valid IBAN is accepted

- **Given** the user submits a company with IBAN "DE89370400440532013000"
- **Then** the company is created successfully

### Valid IBAN with spaces is accepted

- **Given** the user submits a company with IBAN "DE89 3704 0044 0532 0130 00"
- **Then** whitespace is stripped and the IBAN is stored as "DE89370400440532013000"

### IBAN too short is rejected

- **Given** the user submits a company with IBAN "DE89"
- **Then** the response status is 400 Bad Request
- **Then** the error indicates invalid IBAN format

### IBAN with invalid country code is rejected

- **Given** the user submits a company with IBAN "1289370400440532013000" (starts with digits)
- **Then** the response status is 400 Bad Request

### IBAN exceeding max length is rejected

- **Given** the user submits a company with an IBAN of 35 characters
- **Then** the response status is 400 Bad Request

### Empty IBAN is accepted (field is optional)

- **Given** the user submits a company with IBAN as empty string or null
- **Then** the company is created with IBAN as null

## BIC Validation

### Valid 8-character BIC is accepted

- **Given** the user submits a company with BIC "DEUTDEFF"
- **Then** the company is created successfully

### Valid 11-character BIC is accepted

- **Given** the user submits a company with BIC "DEUTDEFF500"
- **Then** the company is created successfully

### BIC with wrong length is rejected

- **Given** the user submits a company with BIC "DEUTDE" (6 characters)
- **Then** the response status is 400 Bad Request
- **Then** the error indicates BIC must be 8 or 11 characters

### BIC with non-alphanumeric characters is rejected

- **Given** the user submits a company with BIC "DEUT-DEFF"
- **Then** the response status is 400 Bad Request

### Empty BIC is accepted (field is optional)

- **Given** the user submits a company with BIC as empty string or null
- **Then** the company is created with BIC as null

## VAT ID

### VAT ID stored as-is

- **Given** the user submits a company with VAT ID "DE123456789"
- **Then** the VAT ID is stored as "DE123456789"

### VAT ID with any format is accepted

- **Given** the user submits a company with VAT ID "ATU12345678"
- **Then** the company is created successfully (no format validation)

### VAT ID exceeding max length is rejected

- **Given** the user submits a company with a VAT ID of 21 characters
- **Then** the response status is 400 Bad Request

## Company Update

### Update financial fields

- **Given** a company exists without financial fields
- **When** the user edits the company and adds IBAN "DE89370400440532013000"
- **Then** the IBAN is saved
- **Then** the "Finanzen" section now appears in the detail view

### Clear financial fields

- **Given** a company exists with all four financial fields set
- **When** the user edits the company and clears all financial fields
- **Then** all four fields are set to null
- **Then** the "Finanzen" section disappears from the detail view

## Detail View

### Finanzen section displays when fields present

- **Given** a company has bankName "Deutsche Bank" and IBAN "DE89370400440532013000"
- **When** the detail view is displayed
- **Then** a "Finanzen" section is shown with Bank and IBAN values
- **Then** BIC and VAT ID are not shown (they are null)

### Finanzen section hidden when all fields null

- **Given** a company has no financial fields set
- **When** the detail view is displayed
- **Then** no "Finanzen" section is rendered

### Copy action on financial fields

- **Given** a company has IBAN "DE89370400440532013000"
- **When** the user clicks the copy icon next to IBAN
- **Then** the IBAN is copied to the clipboard

## Readonly Protection

### Financial fields readonly when externally managed

- **Given** a company is linked to an external system (e.g. SEVDESK)
- **When** the edit form is opened
- **Then** bankName, bic, iban, vatId fields are disabled
- **Then** each shows a "Managed by SevDesk" hint

### Financial fields editable for Brevo companies

- **Given** a company is linked to BREVO
- **When** the edit form is opened
- **Then** bankName, bic, iban, vatId fields are editable (Brevo does not manage bank data)

### Financial fields editable for unlinked companies

- **Given** a company has no external links
- **When** the edit form is opened
- **Then** all financial fields are editable without any hints

## CSV Export

### Financial fields included in export

- **Given** a company has Bank "Deutsche Bank", BIC "DEUTDEFF", IBAN "DE89370400440532013000", VAT ID "DE123456789"
- **When** a CSV export includes the financial columns
- **Then** the CSV contains columns Bank, BIC, IBAN, VAT ID with the correct values

### Empty financial fields exported as empty

- **Given** a company has no financial fields set
- **When** a CSV export includes the financial columns
- **Then** the financial columns contain empty values

## API

### GET company returns financial fields

- **Given** a company has IBAN "DE89370400440532013000"
- **When** `GET /api/companies/{id}` is called
- **Then** the response includes `iban: "DE89370400440532013000"`
- **Then** `bankName`, `bic`, `vatId` are null

### POST company with financial fields

- **Given** a valid company creation request includes `iban: "DE89370400440532013000"`
- **When** `POST /api/companies` is called
- **Then** the company is created with the IBAN set
- **Then** the response status is 201

### PUT company rejects invalid IBAN

- **Given** a company exists
- **When** `PUT /api/companies/{id}` is called with `iban: "INVALID"`
- **Then** the response status is 400 Bad Request
