# Behaviors: Company Phone Number

## Detail View

### Phone number displayed next to email

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** the company detail view is displayed
- **Then** a "Phone" / "Telefon" field is shown next to the email field
- **And** the value reads "+49 30 12345678"

### Null phone number shows dash

- **Given** a company with phoneNumber null
- **When** the company detail view is displayed
- **Then** the phone field shows "—"

### Phone field has copy and tel actions (Spec 040)

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** the user hovers over the phone field in the detail view
- **Then** copy-to-clipboard and tel: action icons appear

## Create Form

### Phone number input visible in create form

- **Given** the user navigates to the create company form
- **When** the form renders
- **Then** a phone number input field is displayed next to the email field

### Create company with phone number

- **Given** the user fills in the create company form
- **And** enters "+49 30 12345678" as phone number
- **When** the user saves the form
- **Then** the company is created with phoneNumber "+49 30 12345678"

### Create company without phone number

- **Given** the user fills in the create company form
- **And** leaves the phone number field empty
- **When** the user saves the form
- **Then** the company is created with phoneNumber null

## Edit Form

### Phone number pre-filled in edit form

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** the user opens the edit form
- **Then** the phone number input shows "+49 30 12345678"

### Update phone number

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** the user changes the phone number to "+49 40 98765432" and saves
- **Then** the company's phoneNumber is updated to "+49 40 98765432"

### Clear phone number

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** the user clears the phone number field and saves
- **Then** the company's phoneNumber is set to null

### Null phone number shows empty input in edit form

- **Given** a company with phoneNumber null
- **When** the user opens the edit form
- **Then** the phone number input is empty

## API

### Phone number included in GET response

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** `GET /api/companies/{id}` is called
- **Then** the response JSON includes `"phoneNumber": "+49 30 12345678"`

### Null phone number in GET response

- **Given** a company with phoneNumber null
- **When** `GET /api/companies/{id}` is called
- **Then** the response JSON includes `"phoneNumber": null`

### Phone number accepted in POST

- **Given** a valid company creation payload with `"phoneNumber": "+49 30 12345678"`
- **When** `POST /api/companies` is called
- **Then** the company is created with the given phone number

### Phone number accepted in PUT

- **Given** an existing company
- **And** an update payload with `"phoneNumber": "+49 40 98765432"`
- **When** `PUT /api/companies/{id}` is called
- **Then** the company's phone number is updated

## CSV Export (Spec 038)

### Phone number available as export column

- **Given** the CSV export dialog is opened for companies
- **When** the dialog renders
- **Then** "Phone Number" / "Telefon" is listed as an available column

### Phone number exported in CSV

- **Given** a company with phoneNumber "+49 30 12345678"
- **When** CSV is exported with the PHONE_NUMBER column selected
- **Then** the CSV contains the phone number value for that company

## List Table

### Phone number not shown in list table

- **Given** companies exist with phone numbers
- **When** the company list table is displayed
- **Then** there is no phone number column in the table

## Database Migration

### Existing companies get null phone number

- **Given** companies exist in the database before the migration
- **When** the migration runs
- **Then** all existing companies have phoneNumber set to null
