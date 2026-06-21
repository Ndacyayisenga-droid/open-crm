# Behaviors: Remove Comment Count Column

## Company List Table

### Comment count column not shown

- **Given** the user opens the company list
- **When** the table is rendered
- **Then** there is no comment count column header
- **Then** there is no comment count data in any row

### Other columns remain unchanged

- **Given** the user opens the company list
- **When** the table is rendered
- **Then** all other columns (name, contacts, actions, etc.) are still displayed

### Comment action button still works

- **Given** the user opens the company list
- **When** the user clicks the comment action button (MessageSquarePlus) on a row
- **Then** the add comment dialog opens as before

## Contact List Table

### Comment count column not shown

- **Given** the user opens the contact list
- **When** the table is rendered
- **Then** there is no comment count column header
- **Then** there is no comment count data in any row

### Other columns remain unchanged

- **Given** the user opens the contact list
- **When** the table is rendered
- **Then** all other columns (name, email, company, actions, etc.) are still displayed

### Comment action button still works

- **Given** the user opens the contact list
- **When** the user clicks the comment action button on a row
- **Then** the add comment dialog opens as before

## CSV Export

### Comment count not available in company export

- **Given** the user opens the CSV export dialog for companies
- **When** the column selection is displayed
- **Then** there is no comment count option

### Comment count not available in contact export

- **Given** the user opens the CSV export dialog for contacts
- **When** the column selection is displayed
- **Then** there is no comment count option

## Detail Views Unchanged

### Company detail still shows comment count

- **Given** a company has 5 comments
- **When** the user opens the company detail view
- **Then** the comment count is displayed in the comments heading

### Contact detail still shows comment count

- **Given** a contact has 3 comments
- **When** the user opens the contact detail view
- **Then** the comment count is displayed in the comments heading

## Print Views Unchanged

### Company print view unaffected

- **Given** the user opens the company print view
- **When** the table is rendered
- **Then** there is no comment count column (same as before)

### Contact print view unaffected

- **Given** the user opens the contact print view
- **When** the table is rendered
- **Then** there is no comment count column (same as before)
