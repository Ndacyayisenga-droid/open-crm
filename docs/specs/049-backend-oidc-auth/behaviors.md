# Behaviors: Backend OIDC Auth

## Protected Endpoints

### Request with valid token succeeds

- **Given** the backend is running with OIDC configured
- **And** a valid JWT token is issued by the configured provider
- **When** `GET /api/companies` is called with `Authorization: Bearer <valid-token>`
- **Then** the response status is 200

### Request without token returns 401

- **Given** the backend is running with OIDC configured
- **When** `GET /api/companies` is called without an Authorization header
- **Then** the response status is 401 Unauthorized

### Request with invalid token returns 401

- **Given** the backend is running with OIDC configured
- **When** `GET /api/companies` is called with `Authorization: Bearer invalid-token`
- **Then** the response status is 401 Unauthorized

### Request with expired token returns 401

- **Given** the backend is running with OIDC configured
- **And** a JWT token that has expired
- **When** `GET /api/companies` is called with the expired token
- **Then** the response status is 401 Unauthorized

### All CRUD endpoints are protected

- **Given** the backend is running with OIDC configured
- **When** any of the following are called without a token: `POST /api/companies`, `PUT /api/companies/{id}`, `DELETE /api/companies/{id}`, `GET /api/contacts`, `POST /api/contacts`, `GET /api/companies/{id}/comments`, `POST /api/companies/{id}/comments`
- **Then** each returns 401 Unauthorized

### CSV export endpoint is protected

- **Given** the backend is running with OIDC configured
- **When** `GET /api/companies/export` is called without a token
- **Then** the response status is 401 Unauthorized

### Image endpoints are protected

- **Given** the backend is running with OIDC configured
- **When** `GET /api/companies/{id}/logo` or `POST /api/companies/{id}/logo` is called without a token
- **Then** the response status is 401 Unauthorized

### Brevo sync endpoint is protected

- **Given** the backend is running with OIDC configured
- **When** `POST /api/brevo/sync` is called without a token
- **Then** the response status is 401 Unauthorized

## Public Endpoints

### Health endpoint accessible without token

- **Given** the backend is running with OIDC configured
- **When** `GET /api/health` is called without an Authorization header
- **Then** the response status is 200

### Swagger UI accessible without token

- **Given** the backend is running with OIDC configured
- **When** `/swagger-ui.html` is accessed without an Authorization header
- **Then** the Swagger UI page loads successfully

### OpenAPI docs accessible without token

- **Given** the backend is running with OIDC configured
- **When** `GET /v3/api-docs` is called without an Authorization header
- **Then** the OpenAPI JSON is returned with status 200

## Swagger UI Authorize

### Swagger UI shows Authorize button

- **Given** Swagger UI is loaded
- **When** the page renders
- **Then** an "Authorize" button is visible

### Protected endpoints callable after authorization in Swagger

- **Given** a user has authorized in Swagger UI with a valid token
- **When** they execute a request to a protected endpoint (e.g., `GET /api/companies`)
- **Then** the request includes the Authorization header
- **And** the response is successful

## User Extraction from Token

### UserService returns user from JWT claims

- **Given** a request is made with a valid token containing claims `name: "Alice"` and `email: "alice@example.com"`
- **When** `userService.getCurrentUser()` is called
- **Then** it returns `UserInfo("Alice", "alice@example.com")`

### UserService handles missing name claim

- **Given** a request is made with a valid token without a `name` claim
- **When** `userService.getCurrentUser()` is called
- **Then** it returns a UserInfo with name "Unknown"

### UserService handles missing email claim

- **Given** a request is made with a valid token without an `email` claim
- **When** `userService.getCurrentUser()` is called
- **Then** it returns a UserInfo with an empty email string

### UserService throws without authentication

- **Given** no authentication is in the SecurityContext (e.g., a public endpoint)
- **When** `userService.getCurrentUser()` is called
- **Then** it throws an `IllegalStateException`

## Comment Author from Token

### Comment author set from authenticated user

- **Given** a user is authenticated with name "Alice"
- **When** they create a comment on a company
- **Then** the comment's author is set to "Alice"

### Different users create comments with their own names

- **Given** user "Alice" creates a comment
- **And** user "Bob" creates another comment
- **When** the comments are retrieved
- **Then** Alice's comment has author "Alice"
- **And** Bob's comment has author "Bob"

## Token Validation

### Token validated against JWKS

- **Given** the backend is configured with `OIDC_ISSUER_URI` pointing to the mock server
- **When** a request is made with a token signed by the mock server
- **Then** the token signature is validated against the mock server's JWKS endpoint
- **And** the request succeeds

### Token from wrong issuer rejected

- **Given** the backend is configured with `OIDC_ISSUER_URI` pointing to one provider
- **When** a request is made with a token issued by a different provider
- **Then** the response status is 401 Unauthorized

## Tests

### Service tests work with mock security context

- **Given** the test sets up a mock JWT in the SecurityContext
- **When** `CommentService.addToCompany()` is called
- **Then** the comment author is set from the mock JWT's name claim

### Existing tests pass with security enabled

- **Given** all existing tests are updated with mock tokens
- **When** `mvn clean verify` is run
- **Then** all tests pass

## Docker Compose

### OIDC_ISSUER_URI passed to backend service

- **Given** the `docker-compose.yml` file
- **When** the backend service definition is inspected
- **Then** it includes `OIDC_ISSUER_URI` in its environment

### Override points to mock server internal URL

- **Given** the `docker-compose.override.yml` file
- **When** the backend service definition is inspected
- **Then** `OIDC_ISSUER_URI` is set to the mock server's internal Docker hostname (e.g., `http://mock-oauth2:8080/default`)
