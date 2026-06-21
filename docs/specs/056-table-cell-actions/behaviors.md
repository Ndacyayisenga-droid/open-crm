# Behaviors: Table Cell Inline Actions

## Company Table — Website Actions

### Copy website to clipboard

- **Given** a company with website "example.com" is shown in the table
- **When** the user clicks the Copy icon next to the website
- **Then** "example.com" is copied to the clipboard and the icon changes to a green Check for 2 seconds

### Open website in new tab

- **Given** a company with website "example.com" is shown in the table
- **When** the user clicks the ExternalLink icon next to the website
- **Then** a new browser tab opens with the website URL

### Website null shows dash without icons

- **Given** a company with no website (null)
- **When** the table row renders
- **Then** the Website cell shows "—" with no action icons

### Website action does not trigger row navigation

- **Given** a company row in the table
- **When** the user clicks the Copy icon on the website
- **Then** the clipboard is updated but the user does NOT navigate to the company detail page

## Company Table — Contacts Count Action

### Navigate to filtered contact list

- **Given** a company with 5 contacts is shown in the table
- **When** the user clicks the ExternalLink icon next to the contact count
- **Then** the browser navigates to `/contacts?companyId={companyId}`

### Contacts count zero shows no icon

- **Given** a company with 0 contacts
- **When** the table row renders
- **Then** the Contacts cell shows "0" with no action icon

### Contacts action does not trigger row navigation

- **Given** a company row in the table
- **When** the user clicks the ExternalLink icon on the contacts count
- **Then** the user navigates to the contact list, NOT to the company detail page

## Contact Table — Email Actions

### Copy email to clipboard

- **Given** a contact with email "john@example.com" is shown in the table
- **When** the user clicks the Copy icon next to the email
- **Then** "john@example.com" is copied to the clipboard and the icon changes to a green Check for 2 seconds

### Send email via mailto

- **Given** a contact with email "john@example.com" is shown in the table
- **When** the user clicks the Mail icon next to the email
- **Then** the default email client opens with "john@example.com" as the recipient

### Email null shows dash without icons

- **Given** a contact with no email (null)
- **When** the table row renders
- **Then** the Email cell shows "—" with no action icons

### Email action does not trigger row navigation

- **Given** a contact row in the table
- **When** the user clicks the Copy icon on the email
- **Then** the clipboard is updated but the user does NOT navigate to the contact detail page

## Contact Table — Company Actions

### Copy company name to clipboard

- **Given** a contact with company "Acme Corp" is shown in the table
- **When** the user clicks the Copy icon next to the company name
- **Then** "Acme Corp" is copied to the clipboard and the icon changes to a green Check for 2 seconds

### Open company details

- **Given** a contact with company "Acme Corp" (companyId = "abc-123") is shown in the table
- **When** the user clicks the ExternalLink icon next to the company name
- **Then** the browser navigates to `/companies/abc-123`

### Company null shows dash without icons

- **Given** a contact with no company association (null)
- **When** the table row renders
- **Then** the Company cell shows "—" with no action icons

### Company action does not trigger row navigation

- **Given** a contact row in the table
- **When** the user clicks the ExternalLink icon on the company name
- **Then** the user navigates to the company detail page, NOT to the contact detail page

## Styling

### Icons match detail view style

- **Given** any table cell with action icons
- **When** the cell renders
- **Then** icons are small (h-3.5 w-3.5), light gray (text-oe-gray-light), and darken on hover (text-oe-dark)

### Icons dark on touch devices

- **Given** any table cell with action icons
- **When** viewed on a touch device (pointer: coarse)
- **Then** icons are always dark (text-oe-dark) for visibility

### Copy feedback resets after timeout

- **Given** the user clicked a Copy icon and the Check icon is showing
- **When** 2 seconds elapse
- **Then** the Check icon reverts back to the Copy icon
