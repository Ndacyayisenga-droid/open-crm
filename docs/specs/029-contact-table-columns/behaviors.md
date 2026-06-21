# Behaviors: Merge Name Columns and Add Email Column to Contact List Table

## Name Column

### Full name is displayed as single column

- **Given** a contact with firstName="Anna" and lastName="Schmidt"
- **When** the contact list renders
- **Then** the Name column shows "Anna Schmidt"

### Only last name shown when first name is empty

- **Given** a contact with firstName="" and lastName="Schmidt"
- **When** the contact list renders
- **Then** the Name column shows "Schmidt" (no leading space)

### Only first name shown when last name is empty

- **Given** a contact with firstName="Anna" and lastName=""
- **When** the contact list renders
- **Then** the Name column shows "Anna" (no trailing space)

### Name column order is always firstName then lastName

- **Given** the UI language is set to German or English
- **When** the contact list renders
- **Then** names are displayed as "Vorname Nachname" / "firstName lastName" regardless of language

## Email Column

### Email is displayed between Name and Company

- **Given** a contact with email="anna@test.com"
- **When** the contact list renders
- **Then** the column order is: Photo | Name | Email | Company | Comments | Actions
- **And** the Email column shows "anna@test.com"

### Null email shows dash

- **Given** a contact with email=null
- **When** the contact list renders
- **Then** the Email column shows "—"

## Column Headers

### Table headers reflect new structure

- **Given** the user views the contact list
- **When** the table renders
- **Then** the headers are: (empty) | "Name" | "Email"/"E-Mail" | "Company"/"Firma" | "Comments"/"Kommentare" | "Actions"/"Aktionen"
- **And** there are no separate "First Name" or "Last Name" headers

## Sorting

### Default sort by last name still works

- **Given** contacts "Anna Zimmermann", "Bob Adams", and "Clara Müller" exist
- **When** the contact list loads with default sort (lastName ascending)
- **Then** the contacts appear in order: "Bob Adams", "Clara Müller", "Anna Zimmermann"

## Filters

### Separate firstName and lastName filters still work

- **Given** contacts "Anna Schmidt" and "Bob Schmidt" exist
- **When** the user filters by firstName="Anna"
- **Then** only "Anna Schmidt" is shown in the Name column
- **And** the firstName and lastName filter inputs remain separate

## Existing Functionality

### Row click still navigates to detail

- **Given** the contact list shows "Anna Schmidt"
- **When** the user clicks the row
- **Then** the user is navigated to the contact detail page for Anna Schmidt

### Pagination still works

- **Given** 30 contacts exist
- **When** the contact list renders
- **Then** pagination shows 20 contacts per page with the new column layout
