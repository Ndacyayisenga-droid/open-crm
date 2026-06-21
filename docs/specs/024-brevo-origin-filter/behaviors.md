# Behaviors: Brevo Origin Filter for Company and Contact Lists

## Company List — Backend

### Filter for Brevo companies only

- **Given** company "Alpha" has `brevoCompanyId = "abc123"` and company "Beta" has `brevoCompanyId = NULL`
- **When** `GET /api/companies?brevo=true` is called
- **Then** only "Alpha" is returned

### Filter for non-Brevo companies only

- **Given** company "Alpha" has `brevoCompanyId = "abc123"` and company "Beta" has `brevoCompanyId = NULL`
- **When** `GET /api/companies?brevo=false` is called
- **Then** only "Beta" is returned

### No brevo parameter returns all companies

- **Given** company "Alpha" has `brevoCompanyId = "abc123"` and company "Beta" has `brevoCompanyId = NULL`
- **When** `GET /api/companies` is called without a `brevo` parameter
- **Then** both "Alpha" and "Beta" are returned

### Brevo filter combines with name filter (AND)

- **Given** companies "Acme Brevo" (brevoCompanyId set), "Acme Manual" (no brevoCompanyId), and "Other Brevo" (brevoCompanyId set) exist
- **When** `GET /api/companies?name=Acme&brevo=true` is called
- **Then** only "Acme Brevo" is returned

### Brevo filter combines with archive toggle (AND)

- **Given** a Brevo company "Active" (not deleted) and a Brevo company "Archived" (deleted) exist
- **When** `GET /api/companies?brevo=true&includeDeleted=false` is called
- **Then** only "Active" is returned

## Contact List — Backend

### Filter for Brevo contacts only

- **Given** contact "Anna" has `brevoId = "200"` and contact "Bob" has `brevoId = NULL`
- **When** `GET /api/contacts?brevo=true` is called
- **Then** only "Anna" is returned

### Filter for non-Brevo contacts only

- **Given** contact "Anna" has `brevoId = "200"` and contact "Bob" has `brevoId = NULL`
- **When** `GET /api/contacts?brevo=false` is called
- **Then** only "Bob" is returned

### No brevo parameter returns all contacts

- **Given** contact "Anna" has `brevoId = "200"` and contact "Bob" has `brevoId = NULL`
- **When** `GET /api/contacts` is called without a `brevo` parameter
- **Then** both "Anna" and "Bob" are returned

### Brevo filter combines with name filter (AND)

- **Given** contacts "Anna Smith" (brevoId set) and "Anna Jones" (no brevoId) exist
- **When** `GET /api/contacts?firstName=Anna&brevo=true` is called
- **Then** only "Anna Smith" is returned

### Brevo filter combines with company filter (AND)

- **Given** contact "Anna" (brevoId set, company "Acme") and contact "Bob" (brevoId set, company "Beta") exist
- **When** `GET /api/contacts?companyId={acmeId}&brevo=true` is called
- **Then** only "Anna" is returned

## Company List — Frontend

### Brevo filter dropdown is displayed

- **Given** the user navigates to the company list
- **When** the page loads
- **Then** a dropdown with options "All", "From Brevo", "Not from Brevo" is visible
- **And** "All" is selected by default

### Selecting "From Brevo" filters the list

- **Given** the company list shows all companies
- **When** the user selects "From Brevo" from the dropdown
- **Then** only companies imported from Brevo are shown
- **And** the page resets to page 1

### Selecting "Not from Brevo" filters the list

- **Given** the company list shows all companies
- **When** the user selects "Not from Brevo" from the dropdown
- **Then** only manually created companies are shown
- **And** the page resets to page 1

### Selecting "All" shows all companies again

- **Given** the company list is filtered to "From Brevo"
- **When** the user selects "All" from the dropdown
- **Then** all companies are shown again

## Contact List — Frontend

### Brevo filter dropdown is displayed

- **Given** the user navigates to the contact list
- **When** the page loads
- **Then** a dropdown with options "All", "From Brevo", "Not from Brevo" is visible
- **And** "All" is selected by default

### Selecting "From Brevo" filters the contact list

- **Given** the contact list shows all contacts
- **When** the user selects "From Brevo" from the dropdown
- **Then** only contacts imported from Brevo are shown
- **And** the page resets to page 1

### Selecting "Not from Brevo" filters the contact list

- **Given** the contact list shows all contacts
- **When** the user selects "Not from Brevo" from the dropdown
- **Then** only manually created contacts are shown
- **And** the page resets to page 1

## Edge Cases

### Empty result when no Brevo companies exist

- **Given** no companies have a `brevoCompanyId`
- **When** the user selects "From Brevo" in the company list
- **Then** the empty state message is shown ("No companies found...")

### Empty result when all contacts are from Brevo

- **Given** all contacts have a `brevoId`
- **When** the user selects "Not from Brevo" in the contact list
- **Then** the empty state message is shown ("No contacts found")

### Pagination works with brevo filter

- **Given** 30 Brevo companies exist
- **When** the user selects "From Brevo" and navigates to page 2
- **Then** the remaining Brevo companies are shown
- **And** non-Brevo companies are not included in any page
