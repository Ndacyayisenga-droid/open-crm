# Behaviors: Print Safari Fix

## Safari Print — Company

### Company table fits on A4 portrait in Safari

- **Given** companies exist in the system
- **When** the company print view is opened in Safari and printed in DIN A4 portrait
- **Then** the table fits within the page width without being cut off

### Long company name wraps in Safari print

- **Given** a company with a very long name
- **When** the company print view is printed in Safari
- **Then** the company name wraps within its cell
- **And** the table does not overflow the page width

### Long website URL wraps in Safari print

- **Given** a company with a very long website URL
- **When** the company print view is printed in Safari
- **Then** the URL wraps within its cell
- **And** the table does not overflow the page width

## Safari Print — Contact

### Contact table fits on A4 portrait in Safari

- **Given** contacts exist in the system
- **When** the contact print view is opened in Safari and printed in DIN A4 portrait
- **Then** the table fits within the page width without being cut off

### Long contact name wraps in Safari print

- **Given** a contact with a very long first or last name
- **When** the contact print view is printed in Safari
- **Then** the name wraps within its cell
- **And** the table does not overflow the page width

### Long email wraps in Safari print

- **Given** a contact with a very long email address
- **When** the contact print view is printed in Safari
- **Then** the email wraps within its cell
- **And** the table does not overflow the page width

## Chromium Browsers — No Regression

### Company print still works in Chromium browsers

- **Given** companies exist in the system
- **When** the company print view is printed in a Chromium browser (Chrome, Brave, Edge)
- **Then** the table fits within the page width (no regression)

### Contact print still works in Chromium browsers

- **Given** contacts exist in the system
- **When** the contact print view is printed in a Chromium browser (Chrome, Brave, Edge)
- **Then** the table fits within the page width (no regression)
