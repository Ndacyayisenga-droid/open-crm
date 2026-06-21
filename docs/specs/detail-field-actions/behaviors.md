# Behaviors: Detail Field Actions

## Shared DetailField Component

### DetailField used in both detail views

- **Given** the company detail view is displayed
- **When** the component renders
- **Then** it uses the shared `DetailField` component (not a local definition)

- **Given** the contact detail view is displayed
- **When** the component renders
- **Then** it uses the shared `DetailField` component (not a local definition)

## Icon Visibility

### Icons hidden by default

- **Given** a company detail view with a filled email field
- **When** the mouse is not hovering over the email field
- **Then** no action icons are visible

### Icons appear on hover

- **Given** a company detail view with a filled email field
- **When** the user hovers over the email field
- **Then** the copy and mailto icons become visible

### No icons for empty fields

- **Given** a company with email set to null
- **When** the user hovers over the email field (showing "—")
- **Then** no action icons appear

## Copy to Clipboard

### Copy company email

- **Given** a company with email "info@acme.com"
- **When** the user hovers over the email field and clicks the copy icon
- **Then** "info@acme.com" is copied to the clipboard

### Copy company website

- **Given** a company with website "https://acme.com"
- **When** the user hovers over the website field and clicks the copy icon
- **Then** "https://acme.com" is copied to the clipboard

### Copy company address (multiline)

- **Given** a company with address "Musterstraße 42", "12345", "Berlin", "Deutschland"
- **When** the user hovers over the address field and clicks the copy icon
- **Then** the clipboard contains the multi-line text:
  ```
  Musterstraße 42
  12345 Berlin
  Deutschland
  ```

### Copy contact email

- **Given** a contact with email "anna@example.com"
- **When** the user hovers over the email field and clicks the copy icon
- **Then** "anna@example.com" is copied to the clipboard

### Copy contact LinkedIn URL

- **Given** a contact with linkedInUrl "https://linkedin.com/in/anna"
- **When** the user hovers over the LinkedIn field and clicks the copy icon
- **Then** "https://linkedin.com/in/anna" is copied to the clipboard

### Copy contact phone number

- **Given** a contact with phoneNumber "+49 30 12345678"
- **When** the user hovers over the phone field and clicks the copy icon
- **Then** "+49 30 12345678" is copied to the clipboard

### Copy feedback: icon changes to checkmark

- **Given** a field with a copy icon visible on hover
- **When** the user clicks the copy icon
- **Then** the icon changes to a checkmark for approximately 2 seconds
- **And** then reverts back to the copy icon

## Open URL in New Tab

### Open company website

- **Given** a company with website "https://acme.com"
- **When** the user hovers over the website field and clicks the external link icon
- **Then** "https://acme.com" opens in a new browser tab

### Open contact LinkedIn

- **Given** a contact with linkedInUrl "https://linkedin.com/in/anna"
- **When** the user hovers over the LinkedIn field and clicks the external link icon
- **Then** "https://linkedin.com/in/anna" opens in a new browser tab

### URL normalization: missing protocol

- **Given** a contact with linkedInUrl "linkedin.com/in/anna"
- **When** the user clicks the external link icon
- **Then** "https://linkedin.com/in/anna" opens in a new browser tab

### URL normalization: http preserved

- **Given** a company with website "http://legacy-site.com"
- **When** the user clicks the external link icon
- **Then** "http://legacy-site.com" opens in a new browser tab (http preserved, not forced to https)

## Mailto

### Mailto on company email

- **Given** a company with email "info@acme.com"
- **When** the user hovers over the email field and clicks the mail icon
- **Then** the browser navigates to "mailto:info@acme.com"

### Mailto on contact email

- **Given** a contact with email "anna@example.com"
- **When** the user hovers over the email field and clicks the mail icon
- **Then** the browser navigates to "mailto:anna@example.com"

## Tel

### Tel on contact phone

- **Given** a contact with phoneNumber "+49 30 12345678"
- **When** the user hovers over the phone field and clicks the phone icon
- **Then** the browser navigates to "tel:+4930123456​78"

## Fields Without Actions

### No actions on position field

- **Given** a contact with position "CEO"
- **When** the user hovers over the position field
- **Then** no action icons appear

### No actions on gender field

- **Given** a contact with gender "Male"
- **When** the user hovers over the gender field
- **Then** no action icons appear

### No actions on birthday field

- **Given** a contact with a birthday set
- **When** the user hovers over the birthday field
- **Then** no action icons appear

### No actions on language field

- **Given** a contact with language "DE"
- **When** the user hovers over the language field
- **Then** no action icons appear

### No actions on company name field

- **Given** a company detail view with a filled name field
- **When** the user hovers over the name field
- **Then** no action icons appear
