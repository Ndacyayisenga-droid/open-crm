# Behaviors: Print Functionality for Company and Contact List Tables

## Print Button

### Print button is visible on company list

- **Given** the user views the company list
- **When** the page loads
- **Then** a "Print" / "Drucken" button is visible in the header area

### Print button is visible on contact list

- **Given** the user views the contact list
- **When** the page loads
- **Then** a "Print" / "Drucken" button is visible in the header area

### Print button opens new tab with current filters

- **Given** the company list is filtered by name "Acme" and brevo "true"
- **When** the user clicks the print button
- **Then** a new browser tab opens with URL `/companies/print?name=Acme&brevo=true`

### Print button with no active filters opens clean URL

- **Given** the company list has no active filters
- **When** the user clicks the print button
- **Then** a new browser tab opens with URL `/companies/print`

## Data Loading

### All filtered records are loaded across multiple pages

- **Given** 500 companies match the current filters
- **When** the print page opens
- **Then** all 500 companies are loaded (via multiple API requests with size=250)
- **And** the table shows all 500 rows

### Loading state is shown while fetching

- **Given** the print page is loading data
- **When** the first page is being fetched
- **Then** a loading indicator with "Loading data..." / "Daten werden geladen..." is shown

### Print dialog opens after data is loaded

- **Given** the print page has finished loading all data
- **When** the table is rendered
- **Then** the browser print dialog opens automatically

### Print dialog does not open for empty results

- **Given** no records match the filters
- **When** the print page loads
- **Then** "No records found" / "Keine Datensätze gefunden" is shown
- **And** the print dialog does not open

## Print Page Layout — Company

### Company print page shows title and filter summary

- **Given** the company print page loads with `?name=Acme&brevo=true`
- **When** the data is rendered
- **Then** the title "Firmen" / "Companies" is shown
- **And** below it: "Name: Acme · Brevo: Ja" (or "Yes" in English)

### Company print page shows all columns except actions

- **Given** companies are loaded on the print page
- **When** the table renders
- **Then** columns are: Logo, Name, Website, Contacts, Comments
- **And** no Actions column is shown

### No filters shows "Keine Filter"

- **Given** the company print page loads with no filter parameters
- **When** the data is rendered
- **Then** below the title: "Keine Filter" / "No filters"

## Print Page Layout — Contact

### Contact print page shows title and filter summary

- **Given** the contact print page loads with `?search=Anna&language=DE`
- **When** the data is rendered
- **Then** the title "Kontakte" / "Contacts" is shown
- **And** below it: "Suche: Anna · Sprache: DE" (or "Search: Anna · Language: DE")

### Contact print page shows all columns except actions

- **Given** contacts are loaded on the print page
- **When** the table renders
- **Then** columns are: Photo, Name, Email, Company, Comments
- **And** no Actions column is shown

## Filter Parameter Handling

### Company filters are applied on print page

- **Given** 50 companies exist, 10 from Brevo
- **When** the print page loads with `?brevo=true`
- **Then** only the 10 Brevo companies are shown

### Contact filters are applied on print page

- **Given** 100 contacts exist, 5 named "Anna"
- **When** the print page loads with `?search=Anna`
- **Then** only the 5 matching contacts are shown

### Archive toggle is applied on company print page

- **Given** 40 active and 10 archived companies exist
- **When** the print page loads with `?includeDeleted=true`
- **Then** all 50 companies are shown

## Print CSS

### Table rows do not break across pages

- **Given** a print page with many rows
- **When** the user prints
- **Then** table rows are not split across page boundaries

### Images are printed

- **Given** companies with logos are on the print page
- **When** the user prints
- **Then** logo thumbnails appear in the printout

## No Navigation Elements

### Print page has no sidebar

- **Given** the print page loads
- **When** the page renders
- **Then** no sidebar navigation is visible

### Print page has no filter inputs

- **Given** the print page loads
- **When** the page renders
- **Then** no filter inputs, dropdowns, or buttons are visible (except the browser print dialog)

### Print page has no pagination

- **Given** the print page loads
- **When** all data is rendered
- **Then** no pagination controls are visible
