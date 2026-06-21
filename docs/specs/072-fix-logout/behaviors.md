# Behaviors: Fix Logout Flow

## Cookie Clearing

### Session cookie is cleared in production (HTTPS)

- **Given** a user is logged in on a production deployment (HTTPS with Authentik)
- **When** the user clicks "Logout" in the sidebar
- **Then** the `__Secure-authjs.session-token` cookie is deleted with matching attributes (`Path=/; Secure; HttpOnly; SameSite=Lax`)
- **And** the cookie is no longer present in the browser after the redirect

### Session cookie is cleared in local development (HTTP)

- **Given** a user is logged in on a local development setup (HTTP with mock-oauth2-server)
- **When** the user clicks "Logout" in the sidebar
- **Then** the `authjs.session-token` cookie is deleted
- **And** the cookie is no longer present in the browser after the redirect

### Both cookie names are targeted for deletion

- **Given** a user clicks "Logout"
- **When** the logout route processes the request
- **Then** deletion is attempted for both `authjs.session-token` and `__Secure-authjs.session-token`
- **And** the correct cookie is removed regardless of whether the app runs under HTTP or HTTPS

## OIDC Provider Redirect

### Redirect to OIDC end-session endpoint with id_token_hint

- **Given** a user is logged in and has a valid session with an id_token
- **When** the user clicks "Logout"
- **Then** the browser is redirected to the OIDC provider's `end_session_endpoint`
- **And** the `id_token_hint` query parameter contains the user's id_token
- **And** the `post_logout_redirect_uri` query parameter contains the app's base URL

### Redirect back to app after OIDC session termination

- **Given** the Logout URI is configured in Authentik to the app's base URL
- **And** the user has been redirected to Authentik's `end_session_endpoint`
- **When** Authentik terminates the OIDC session
- **Then** Authentik redirects the user back to the app's base URL
- **And** the user is not shown a plain-text "Logout successful" page

### Graceful handling when id_token is missing

- **Given** a user is logged in but the session does not contain an id_token (e.g., token was lost during refresh)
- **When** the user clicks "Logout"
- **Then** the browser is redirected to the OIDC provider's `end_session_endpoint` without the `id_token_hint` parameter
- **And** the `post_logout_redirect_uri` is still included
- **And** the session cookies are still cleared

### Graceful fallback when OIDC discovery fails

- **Given** a user is logged in
- **And** the OIDC provider's `.well-known/openid-configuration` endpoint is unreachable
- **When** the user clicks "Logout"
- **Then** the session cookies are cleared
- **And** the user is redirected to the app's base URL (fallback, no OIDC end-session)

## Post-Logout Access Control

### Accessing the app after logout redirects to login

- **Given** a user has successfully logged out (session cookies cleared)
- **When** the user navigates to any app page (e.g., `/companies`, `/contacts`)
- **Then** the Auth.js middleware detects no valid session
- **And** the user is redirected to the OIDC login page

### Accessing the API after logout returns 401

- **Given** a user has successfully logged out (session cookies cleared)
- **When** the browser makes an API request through the proxy (e.g., `GET /api/companies`)
- **Then** the proxy route reads no access token from the session
- **And** the backend request is sent without an Authorization header
- **And** the backend returns 401 Unauthorized
