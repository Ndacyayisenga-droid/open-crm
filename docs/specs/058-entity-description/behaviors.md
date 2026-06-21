# Behaviors: Entity Description Field

## Company Description

### Create company with description

- **Given** the user is on the company create form
- **When** the user fills in the name and enters a description text and submits
- **Then** the company is created with the description stored in the database
- **Then** the detail view shows the description between the fields and the comments section

### Create company without description

- **Given** the user is on the company create form
- **When** the user fills in the name and leaves the description empty and submits
- **Then** the company is created with `description = null`
- **Then** the detail view does not show a description block

### Edit company to add description

- **Given** a company exists without a description
- **When** the user opens the edit form and enters a description and saves
- **Then** the company's description is updated
- **Then** the detail view now shows the description

### Edit company to change description

- **Given** a company exists with a description
- **When** the user opens the edit form and modifies the description text and saves
- **Then** the description is updated to the new text

### Edit company to remove description

- **Given** a company exists with a description
- **When** the user opens the edit form and clears the description field and saves
- **Then** the company's description is set to `null`
- **Then** the detail view no longer shows a description block

### Description preserves line breaks

- **Given** a company has a description with multiple lines (newline characters)
- **When** the user views the company detail page
- **Then** the description is rendered with the line breaks preserved

### Description via API

- **Given** a valid API request to create or update a company includes a `description` field
- **When** the request is processed
- **Then** the description is stored and returned in the response DTO

### Description not in CSV export

- **Given** a company has a description
- **When** the user exports companies to CSV
- **Then** the description column is not included in the export

### Description not in print view

- **Given** a company has a description
- **When** the user opens the print view for companies
- **Then** the description is not displayed

## Contact Description

### Create contact with description

- **Given** the user is on the contact create form
- **When** the user fills in first name, last name, and enters a description and submits
- **Then** the contact is created with the description stored in the database
- **Then** the detail view shows the description between the fields and the comments section

### Create contact without description

- **Given** the user is on the contact create form
- **When** the user fills in first name, last name, and leaves the description empty and submits
- **Then** the contact is created with `description = null`
- **Then** the detail view does not show a description block

### Edit contact to add description

- **Given** a contact exists without a description
- **When** the user opens the edit form and enters a description and saves
- **Then** the contact's description is updated
- **Then** the detail view now shows the description

### Edit contact to remove description

- **Given** a contact exists with a description
- **When** the user opens the edit form and clears the description field and saves
- **Then** the contact's description is set to `null`
- **Then** the detail view no longer shows a description block

### Description preserves line breaks on contact

- **Given** a contact has a description with multiple lines
- **When** the user views the contact detail page
- **Then** the description is rendered with line breaks preserved

## Brevo Sync

### Brevo import does not affect description

- **Given** a company or contact exists with a description
- **When** a Brevo import is triggered and the entity is matched by brevoId
- **Then** the description field is not modified by the import

### Brevo import creates entities without description

- **Given** a new company or contact is imported from Brevo
- **When** the import creates the entity
- **Then** the description is `null` (Brevo does not provide a description)

## Database Migration

### Existing records get null description

- **Given** the database contains existing companies and contacts
- **When** migration V13 runs
- **Then** all existing records have `description = NULL`
- **Then** the application starts successfully
