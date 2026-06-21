# Behaviors: Admin Page Rework

## Bearer Token Card

### Token is masked by default

- **Given** a user navigates to the admin page
- **When** the Bearer Token card renders
- **Then** the token value is displayed as masked text (e.g., `••••••••••••••••`)
- **And** a "Show" button is visible

### Token is revealed on Show click

- **Given** the Bearer Token card is displaying a masked token
- **When** the user clicks the "Show" button
- **Then** the full access token is displayed in plain text
- **And** the button label changes to "Hide"

### Token is masked again on Hide click

- **Given** the Bearer Token card is displaying the plain-text token
- **When** the user clicks the "Hide" button
- **Then** the token is masked again
- **And** the button label changes to "Show"

### Token stays visible until navigation

- **Given** the user has revealed the token
- **When** the user does not navigate away from the admin page
- **Then** the token remains visible

### Copy token to clipboard

- **Given** the Bearer Token card is displayed (token masked or visible)
- **When** the user clicks the "Copy" button
- **Then** the raw access token is copied to the clipboard
- **And** the button shows brief "Copied" / checkmark feedback

### Token validity countdown is displayed

- **Given** a user has a valid access token with an `expiresAt` timestamp
- **When** the Bearer Token card renders
- **Then** the remaining validity is displayed (e.g., "Valid for 4:32 min")
- **And** the display updates approximately every 10 seconds

### Expired token is indicated

- **Given** the access token's `expiresAt` timestamp is in the past
- **When** the Bearer Token card renders
- **Then** the text "Expired" is displayed in red (`text-oe-red`)

### No token available

- **Given** the session does not contain an access token
- **When** the Bearer Token card renders
- **Then** a message "No token available" is displayed
- **And** the Copy and Show buttons are not shown

## Unified Card Styling

### All cards use full content width

- **Given** a user views the admin page
- **When** the page renders
- **Then** all cards (Health, Token, Brevo Settings, Brevo Import) span the full content width
- **And** no card has a `max-w-*` constraint

### All cards use consistent styling

- **Given** a user views the admin page
- **When** the page renders
- **Then** all cards have the same border style (`border-oe-gray-light`)
- **And** all card titles use the same font, size, and color (`font-heading text-lg text-oe-dark`)

### Brevo cards show "Brevo" in title

- **Given** a user views the admin page
- **When** the Brevo cards render
- **Then** the settings card title is "Brevo Settings" (EN) / "Brevo-Einstellungen" (DE)
- **And** the import card title is "Brevo Import" (EN) / "Brevo-Import" (DE)

### No duplicate headings

- **Given** a user views the admin page
- **When** the page renders
- **Then** there is exactly one `<h1>` on the page (the admin page title)
- **And** no card component renders its own `<h1>`

### Card order is consistent

- **Given** a user views the admin page
- **When** the page renders
- **Then** the cards appear in order: Server Health, Bearer Token, Brevo Settings, Brevo Import

## Internationalization

### Token card respects language setting

- **Given** the user's language is set to German
- **When** the admin page renders
- **Then** the Bearer Token card uses German labels ("Anzeigen", "Verbergen", "Kopieren", "Gültig für", "Abgelaufen")

### Token card defaults to English

- **Given** the user's language is set to English
- **When** the admin page renders
- **Then** the Bearer Token card uses English labels ("Show", "Hide", "Copy", "Valid for", "Expired")
