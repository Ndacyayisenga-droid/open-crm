# Behaviors: Custom Branded Login Page

## Page Display

### Login page shows branding elements

- **Given** a user is not authenticated
- **When** the user navigates to `/login`
- **Then** the page displays the Open Elements logo, "Open CRM" heading, a login button, and "Developed by Open Elements" credit
- **And** all text uses the correct fonts (Montserrat for heading, Lato for body)

### Desktop layout shows centered dark box

- **Given** a user views the login page on a viewport wider than the `md` breakpoint
- **When** the page renders
- **Then** a dark box (`#020144`) is centered on a light background
- **And** the box contains all branding elements with white/light text
- **And** the box has rounded corners and padding

### Mobile layout shows full-screen dark background

- **Given** a user views the login page on a viewport narrower than the `md` breakpoint
- **When** the page renders
- **Then** the entire viewport has a dark background (`#020144`)
- **And** the branding elements are centered vertically and horizontally
- **And** no floating box or visible border is shown

## Authentication Flow

### Clicking login redirects to OIDC provider

- **Given** a user is on the login page
- **When** the user clicks the login button
- **Then** Auth.js initiates the OIDC sign-in flow
- **And** the user is redirected to the OIDC provider's authorization endpoint

### Successful login redirects to the app

- **Given** a user has completed authentication at the OIDC provider
- **When** the OIDC provider redirects back with a valid authorization code
- **Then** Auth.js creates a session
- **And** the user is redirected to the app (default: `/companies`)

### Unauthenticated access to any page redirects to login

- **Given** a user is not authenticated
- **When** the user navigates to any protected page (e.g., `/companies`, `/contacts`)
- **Then** the Auth.js middleware redirects the user to `/login`

## Error Handling

### Authentication error shows generic message

- **Given** the OIDC provider returns an error (e.g., user cancelled, provider error)
- **When** Auth.js redirects to `/login?error=OAuthCallbackError`
- **Then** the login page displays "Anmeldung fehlgeschlagen. Bitte erneut versuchen." (DE) or "Login failed. Please try again." (EN)
- **And** the error message is styled in red (`#E63277`), visible on the dark background
- **And** the login button is still displayed below the error message

### Session expired shows same generic message

- **Given** a user's session has expired and the token refresh failed
- **When** Auth.js redirects to `/login?error=SessionRequired`
- **Then** the same generic error message is displayed
- **And** the login button is available for re-authentication

### Error details are logged to browser console

- **Given** the login page is loaded with an `?error=...` query parameter
- **When** the page renders
- **Then** the raw error value from the query parameter is logged to `console.error`
- **And** no technical error details are shown to the user in the UI

### No error parameter shows no error message

- **Given** a user navigates to `/login` without an `error` query parameter
- **When** the page renders
- **Then** no error message is displayed
- **And** only the branding elements and login button are shown

## Internationalization

### Login page respects language setting

- **Given** the user's language is set to German (via localStorage or browser detection)
- **When** the login page renders
- **Then** the login button shows "Anmelden"
- **And** the error message (if present) is in German
- **And** the developer credit shows "Entwickelt von"

### Login page defaults to English

- **Given** the user has no language preference set and the browser language is not German
- **When** the login page renders
- **Then** the login button shows "Log in"
- **And** the developer credit shows "Developed by"

## Edge Cases

### Already authenticated user visiting login page

- **Given** a user is already authenticated with a valid session
- **When** the user navigates to `/login`
- **Then** the user is redirected to the app home page (no login page shown)

### Login page accessible without authentication

- **Given** the `/login` route is excluded from the Auth.js middleware matcher
- **When** an unauthenticated user navigates to `/login`
- **Then** the page loads without a redirect loop
- **And** the page content is displayed correctly
