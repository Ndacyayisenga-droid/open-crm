# Behaviors: Frontend OIDC Auth

## Login Flow

### Unauthenticated user is redirected to OIDC provider

- **Given** a user is not logged in
- **When** they navigate to any page (e.g., `/companies`)
- **Then** they are redirected to the OIDC provider's authorization endpoint

### Successful login redirects back to the app

- **Given** a user is redirected to the OIDC provider
- **When** they successfully authenticate
- **Then** they are redirected back to the originally requested page
- **And** a session cookie is set

### Login works with mock-oauth2-server

- **Given** the app is running locally with mock-oauth2-server
- **When** a user accesses the app
- **Then** they are redirected to the mock server's interactive login form
- **And** after entering credentials, they are logged in to the app

### Login works with Authentik

- **Given** the app is deployed with `OIDC_ISSUER_URI` pointing to Authentik
- **When** a user accesses the app
- **Then** they are redirected to the Authentik login page
- **And** after authenticating, they are logged in to the app

## Route Protection

### All pages require authentication

- **Given** a user is not logged in
- **When** they navigate to `/companies`, `/contacts`, `/health`, `/brevo-sync`, or any other page
- **Then** they are redirected to the login flow

### Auth.js API routes are not protected

- **Given** the middleware configuration
- **When** a request is made to `/api/auth/*`
- **Then** it is not intercepted by the auth middleware (no redirect loop)

## Session

### Session stored as JWT cookie

- **Given** a user has logged in
- **When** the session is inspected
- **Then** it is stored in an httpOnly encrypted cookie
- **And** the cookie is not accessible via client-side JavaScript

### Session contains user claims

- **Given** a user has logged in with name "Alice", email "alice@example.com", and picture URL
- **When** `useSession()` is called in a component
- **Then** `session.user.name` is "Alice"
- **And** `session.user.email` is "alice@example.com"
- **And** `session.user.image` is the picture URL

## Token Refresh

### Expired access token is refreshed automatically

- **Given** a user is logged in
- **And** the access token has expired
- **And** the refresh token is still valid
- **When** the user makes an API call or navigates to a page
- **Then** Auth.js refreshes the access token using the refresh token
- **And** the user remains logged in without interruption

### Expired refresh token redirects to login

- **Given** a user is logged in
- **And** both the access token and refresh token have expired
- **When** the user makes an API call or navigates to a page
- **Then** the user is redirected to the login flow

## API Proxy

### API calls include Authorization header

- **Given** a user is logged in with a valid access token
- **When** the frontend makes a request to `/api/companies`
- **Then** the Route Handler forwards the request to the backend at `http://backend:8080/api/companies`
- **And** the request includes an `Authorization: Bearer <token>` header

### API proxy forwards all HTTP methods

- **Given** a user is logged in
- **When** the frontend makes a GET, POST, PUT, or DELETE request to `/api/*`
- **Then** the Route Handler forwards the request with the same HTTP method to the backend

### API proxy returns backend response

- **Given** a user is logged in
- **When** the frontend makes an API call via the proxy
- **Then** the backend's response status and body are returned to the frontend unchanged

### API proxy handles backend errors

- **Given** a user is logged in
- **When** the backend returns a 400, 404, or 500 error
- **Then** the proxy returns the same error status and body to the frontend

### Rewrite rule removed from next.config.ts

- **Given** the `next.config.ts` file
- **When** its content is inspected
- **Then** there is no `rewrites()` function proxying `/api/*` to the backend

## Logout

### Logout clears frontend session

- **Given** a user is logged in
- **When** they click the "Abmelden"/"Logout" button in the sidebar
- **Then** the Auth.js session cookie is deleted

### Logout terminates provider session

- **Given** a user is logged in
- **When** they click the "Abmelden"/"Logout" button
- **Then** they are redirected to the OIDC provider's end-session endpoint
- **And** the provider session is terminated

### After logout, user is redirected to login

- **Given** a user has logged out
- **When** the provider logout completes
- **Then** the user is redirected back to the app
- **And** they see the login flow (redirect to OIDC provider)

## Sidebar User Display

### Real user name shown in sidebar

- **Given** a user is logged in with name "Alice"
- **When** the sidebar is displayed
- **Then** "Alice" is shown in the user section instead of "Demo User"

### Real profile picture shown in sidebar

- **Given** a user is logged in with a `picture` claim URL
- **When** the sidebar is displayed
- **Then** the profile picture from the URL is shown instead of the placeholder icon

### Placeholder icon shown when no picture claim

- **Given** a user is logged in without a `picture` claim (or with null)
- **When** the sidebar is displayed
- **Then** the placeholder user icon is shown

### User section shown on mobile sidebar

- **Given** a user is logged in
- **When** the mobile sidebar drawer is opened
- **Then** the user section shows the real name and profile picture (or placeholder)

## Hardcoded User Removed

### user.ts removed

- **Given** the frontend source code
- **When** `frontend/src/lib/user.ts` is checked
- **Then** the file no longer exists

### No references to hardcoded currentUser

- **Given** the frontend source code
- **When** searching for imports of `user.ts` or `currentUser`
- **Then** no references are found

## Environment Variables

### AUTH_SECRET in .env.example

- **Given** the `.env.example` file
- **When** its content is inspected
- **Then** it contains `AUTH_SECRET` with a placeholder value

### OIDC vars passed to frontend service in docker-compose

- **Given** the `docker-compose.yml` file
- **When** the frontend service definition is inspected
- **Then** it includes `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, and `AUTH_SECRET` in its environment
