# Behaviors: Role Support

## Frontend — OIDC Scope Request

### Roles scope is requested on login

- **Given** a user initiates OIDC login
- **When** the frontend redirects to the OIDC provider
- **Then** the authorization request includes `roles` in the scope parameter

### Unknown roles scope does not break login

- **Given** the OIDC provider has no `roles` scope mapping configured
- **When** a user logs in successfully
- **Then** the login succeeds and the session contains an empty `roles` array

## Frontend — Session Roles

### Roles are stored in session from JWT

- **Given** the OIDC provider returns a JWT with `"roles": ["CRM-ADMIN"]`
- **When** the user completes login
- **Then** the Auth.js session contains `roles: ["CRM-ADMIN"]`

### Multiple roles are stored in session

- **Given** the OIDC provider returns a JWT with `"roles": ["CRM-ADMIN", "CRM-READONLY"]`
- **When** the user completes login
- **Then** the Auth.js session contains `roles: ["CRM-ADMIN", "CRM-READONLY"]`

### Missing roles claim defaults to empty array

- **Given** the OIDC provider returns a JWT without a `roles` claim
- **When** the user completes login
- **Then** the Auth.js session contains `roles: []`

### Roles claim with non-array value defaults to empty array

- **Given** the OIDC provider returns a JWT with `"roles": "not-an-array"`
- **When** the user completes login
- **Then** the Auth.js session contains `roles: []`

## Frontend — Sidebar Tooltip

### Tooltip shows roles for user with roles

- **Given** a logged-in user with `roles: ["CRM-ADMIN"]` in the session
- **When** the user hovers over their name in the sidebar
- **Then** a tooltip appears displaying `CRM-ADMIN`

### Tooltip shows multiple roles

- **Given** a logged-in user with `roles: ["CRM-ADMIN", "CRM-READONLY"]` in the session
- **When** the user hovers over their name in the sidebar
- **Then** a tooltip appears displaying `CRM-ADMIN, CRM-READONLY`

### Tooltip shows "No roles assigned" for user without roles

- **Given** a logged-in user with `roles: []` in the session
- **When** the user hovers over their name in the sidebar
- **Then** a tooltip appears displaying "No roles assigned"

### Tooltip is localized

- **Given** a logged-in user with `roles: []` and the app language set to German
- **When** the user hovers over their name in the sidebar
- **Then** a tooltip appears displaying "Keine Rollen zugewiesen"

## Backend — JWT Authority Mapping

### Roles claim is mapped to Spring Security authorities

- **Given** a JWT with `"roles": ["CRM-ADMIN"]`
- **When** the backend processes an authenticated request
- **Then** the authentication has authority `ROLE_CRM-ADMIN`

### Multiple roles are mapped to multiple authorities

- **Given** a JWT with `"roles": ["CRM-ADMIN", "CRM-READONLY"]`
- **When** the backend processes an authenticated request
- **Then** the authentication has authorities `ROLE_CRM-ADMIN` and `ROLE_CRM-READONLY`

### Missing roles claim results in no role authorities

- **Given** a JWT without a `roles` claim
- **When** the backend processes an authenticated request
- **Then** the authentication has no `ROLE_` authorities (only default scope-based authorities, if any)

### Empty roles array results in no role authorities

- **Given** a JWT with `"roles": []`
- **When** the backend processes an authenticated request
- **Then** the authentication has no `ROLE_` authorities

### Default scope-based authorities are preserved

- **Given** a JWT with both standard scopes and a `roles` claim
- **When** the backend processes an authenticated request
- **Then** the authentication has both scope-based authorities and role-based authorities

## Mock OAuth2 Server

### Mock user has roles in JWT

- **Given** the mock OAuth2 server is running with the updated configuration
- **When** a test user logs in
- **Then** the issued JWT contains `"roles": ["CRM-ADMIN"]`

## Test Utilities

### Test JWT includes roles

- **Given** a test using `TestSecurityUtil.testJwt()`
- **When** the mock JWT is created
- **Then** it contains a `roles` claim with `["CRM-ADMIN"]`

### Test security context includes roles

- **Given** a test using `TestSecurityUtil.setSecurityContext()`
- **When** the security context is set
- **Then** the JWT in the context contains a `roles` claim with `["CRM-ADMIN"]`
