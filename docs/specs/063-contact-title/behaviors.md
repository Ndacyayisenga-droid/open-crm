# Behaviors: Contact Title Field

## Create Contact

### Create contact with title

- **Given** the user is on the contact create form
- **When** the user enters "Dr." as title, "Max" as first name, "Müller" as last name, and submits
- **Then** the contact is created with title "Dr."
- **Then** the detail view header shows "Dr. Max Müller"

### Create contact without title

- **Given** the user is on the contact create form
- **When** the user leaves the title empty and fills in first name and last name
- **Then** the contact is created with title = null
- **Then** the detail view header shows "Max Müller" (no leading space)

## Edit Contact

### Add title to existing contact

- **Given** a contact exists without a title
- **When** the user opens the edit form and enters "Prof." as title and saves
- **Then** the title is stored
- **Then** all name displays update to include the title

### Remove title from contact

- **Given** a contact exists with title "Dr."
- **When** the user opens the edit form and clears the title field and saves
- **Then** the title is set to null
- **Then** all name displays show the name without title

### Change title

- **Given** a contact exists with title "Dr."
- **When** the user changes the title to "Prof. Dr." and saves
- **Then** the title is updated to "Prof. Dr."

## Detail View

### Title displayed in header

- **Given** a contact exists with title "Prof.", first name "Anna", last name "Schmidt"
- **When** the user opens the contact detail view
- **Then** the header shows "Prof. Anna Schmidt"

### No title shows name without prefix

- **Given** a contact exists without a title
- **When** the user opens the contact detail view
- **Then** the header shows "Anna Schmidt" without leading space or prefix

## List Table

### Title shown in name column

- **Given** a contact exists with title "Dr.", first name "Max", last name "Müller"
- **When** the user opens the contact list
- **Then** the Name column shows "Dr. Max Müller"

### No title in name column

- **Given** a contact exists without a title
- **When** the user opens the contact list
- **Then** the Name column shows "Max Müller"

## Print View

### Title shown in print name column

- **Given** a contact exists with title "Prof. Dr."
- **When** the user opens the print view
- **Then** the Name column shows "Prof. Dr. Max Müller"

## CSV Export

### Title exported as separate column

- **Given** a contact has title "Dr."
- **When** the user exports contacts to CSV and selects the Title column
- **Then** the CSV contains a "Title" column with value "Dr."

### Contact without title exports empty

- **Given** a contact has no title
- **When** the user exports contacts to CSV with the Title column selected
- **Then** the Title column is empty for that contact

## Search

### Search finds contact by title

- **Given** a contact exists with title "Prof."
- **When** the user searches for "Prof" in the contact list search field
- **Then** the contact appears in the results

### Search combines title with other fields

- **Given** a contact exists with title "Dr.", last name "Müller"
- **When** the user searches for "Dr Müller"
- **Then** the contact appears in the results (both words match across title and lastName)

## Edit Form Layout

### Title field positioned before first name

- **Given** the user opens the contact create or edit form
- **When** the form is rendered
- **Then** the title field appears in the same row as first name and last name, before first name

## Brevo Sync

### Brevo import does not affect title

- **Given** a contact exists with title "Dr."
- **When** a Brevo import is triggered and the contact is matched by brevoId
- **Then** the title field is not modified

### Brevo import creates contact without title

- **Given** a new contact is imported from Brevo
- **When** the import creates the contact
- **Then** the title is null

## API

### Title via REST API

- **Given** a valid API request to create a contact includes a title field
- **When** the request is processed
- **Then** the title is stored and returned in the response DTO

### Title exceeding max length fails

- **Given** a title longer than 255 characters is provided
- **When** the create or update request is processed
- **Then** the response is 400 Bad Request

## Database Migration

### Existing contacts get null title

- **Given** the database contains existing contacts
- **When** migration V15 runs
- **Then** all existing contacts have title = NULL
- **Then** the application starts successfully
