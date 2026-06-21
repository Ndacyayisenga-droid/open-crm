# Behaviors: Print View A4 Fit

## Table Fits A4 Portrait

### Company table fits on A4 portrait page

- **Given** companies exist in the system
- **When** the company print view is opened and printed in DIN A4 portrait orientation
- **Then** the table fits within the page width without being cut off

### Contact table fits on A4 portrait page

- **Given** contacts exist in the system
- **When** the contact print view is opened and printed in DIN A4 portrait orientation
- **Then** the table fits within the page width without being cut off

## Comment Count Column Removed

### Company print table has no Comments column

- **Given** companies exist in the system
- **When** the company print view is displayed
- **Then** the table shows columns: Logo, Name, Website, Contacts
- **And** there is no Comments column

### Contact print table has no Comments column

- **Given** contacts exist in the system
- **When** the contact print view is displayed
- **Then** the table shows columns: Photo, Name, Email, Company
- **And** there is no Comments column

## Text Wrapping

### Long company name wraps in print

- **Given** a company with a very long name (e.g. "International Association of Professional Software Engineers and Consultants GmbH")
- **When** the company print view is printed
- **Then** the company name wraps to multiple lines within its cell
- **And** the table does not overflow the page width

### Long email wraps in contact print

- **Given** a contact with a very long email address
- **When** the contact print view is printed
- **Then** the email wraps within its cell
- **And** the table does not overflow the page width

### Long URL wraps in company print

- **Given** a company with a very long website URL
- **When** the company print view is printed
- **Then** the URL wraps within its cell
- **And** the table does not overflow the page width

## Table Header Repeats on Page Breaks

### Company table header repeats on second page

- **Given** more companies exist than fit on a single A4 page
- **When** the company print view is printed
- **Then** the table header row (Logo, Name, Website, Contacts) appears at the top of every printed page

### Contact table header repeats on second page

- **Given** more contacts exist than fit on a single A4 page
- **When** the contact print view is printed
- **Then** the table header row (Photo, Name, Email, Company) appears at the top of every printed page

## Row Page Break Prevention (unchanged)

### Table rows do not split across pages

- **Given** the print view has many records spanning multiple pages
- **When** the print view is printed
- **Then** no individual table row is split across a page break

## Logos and Photos Remain

### Company logos still shown in print

- **Given** a company with a logo
- **When** the company print view is printed
- **Then** the company logo is visible in the table

### Contact photos still shown in print

- **Given** a contact with a photo
- **When** the contact print view is printed
- **Then** the contact photo is visible in the table
