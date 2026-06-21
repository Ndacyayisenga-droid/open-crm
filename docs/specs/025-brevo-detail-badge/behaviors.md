# Behaviors: Brevo Origin Badge in Company and Contact Detail Views

## Company Detail — Backend

### Company DTO includes brevo field

- **Given** a company has `brevoCompanyId = "abc123"`
- **When** `GET /api/companies/{id}` is called
- **Then** the response contains `"brevo": true`

### Non-Brevo company DTO has brevo false

- **Given** a company has `brevoCompanyId = NULL`
- **When** `GET /api/companies/{id}` is called
- **Then** the response contains `"brevo": false`

### Company list also includes brevo field

- **Given** companies exist, some with brevoCompanyId and some without
- **When** `GET /api/companies` is called
- **Then** each company in the response has a `brevo` field (true or false)

## Company Detail — Frontend

### Brevo tag is shown for Brevo companies

- **Given** a company was imported from Brevo (`brevo = true`)
- **When** the user views the company detail page
- **Then** a "Brevo" tag is displayed below the company name

### No tag is shown for non-Brevo companies

- **Given** a company was created manually (`brevo = false`)
- **When** the user views the company detail page
- **Then** no tag is displayed below the company name

### Layout does not shift between Brevo and non-Brevo companies

- **Given** company "Alpha" has `brevo = true` and company "Beta" has `brevo = false`
- **When** the user navigates between the two detail views
- **Then** the vertical position of the detail fields below the name area remains the same
- **And** the tag area reserves the same space regardless of tag visibility

## Contact Detail — Frontend

### Brevo tag is shown for Brevo contacts

- **Given** a contact was imported from Brevo (`brevo = true`)
- **When** the user views the contact detail page
- **Then** a "Brevo" tag is displayed below the contact name

### No tag is shown for non-Brevo contacts

- **Given** a contact was created manually (`brevo = false`)
- **When** the user views the contact detail page
- **Then** no tag is displayed below the contact name

### Layout does not shift between Brevo and non-Brevo contacts

- **Given** contact "Anna" has `brevo = true` and contact "Bob" has `brevo = false`
- **When** the user navigates between the two detail views
- **Then** the vertical position of the detail fields below the name area remains the same

### Synced to Brevo checkbox is removed

- **Given** a contact was imported from Brevo (`brevo = true`)
- **When** the user views the contact detail page
- **Then** no "Synced to Brevo" / "Mit Brevo synchronisiert" checkbox is displayed
- **And** the Brevo origin is shown only via the tag below the name

## Visual Design

### Tag uses consistent styling

- **Given** a Brevo-imported record (company or contact)
- **When** the detail view is rendered
- **Then** the tag displays the text "Brevo"
- **And** the tag uses small, muted styling (not a prominent alert or warning)
- **And** the tag is left-aligned below the name heading

### Tag text is the product name Brevo

- **Given** the user's language is set to German or English
- **When** a Brevo-imported record's detail view is rendered
- **Then** the tag text is "Brevo" in both languages (product name, not translated)
