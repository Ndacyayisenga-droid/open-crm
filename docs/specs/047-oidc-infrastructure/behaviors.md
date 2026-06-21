# Behaviors: OIDC Infrastructure

## mock-oauth2-server Starts Locally

### Mock server starts with docker compose up

- **Given** a developer runs `docker compose up` locally
- **When** all services start
- **Then** the mock-oauth2-server is running and accessible at `http://localhost:8888`

### Mock server not deployed to Coolify

- **Given** the application is deployed via Coolify
- **When** Coolify uses only `docker-compose.yml`
- **Then** no mock-oauth2-server service is started

## OIDC Discovery

### Discovery endpoint available

- **Given** the mock-oauth2-server is running
- **When** `GET http://localhost:8888/default/.well-known/openid-configuration` is called
- **Then** a valid OIDC discovery document is returned
- **And** it contains `authorization_endpoint`, `token_endpoint`, `userinfo_endpoint`, and `jwks_uri`

## Interactive Login

### Login form displayed

- **Given** the mock-oauth2-server is running with `interactiveLogin: true`
- **When** a client initiates an Authorization Code Flow (redirects to the authorization endpoint)
- **Then** the mock server displays an interactive login form

### Custom claims can be entered

- **Given** the mock-oauth2-server login form is displayed
- **When** a developer enters a username and submits
- **Then** a token is issued containing `name`, `email`, and `picture` claims

### Different users can log in

- **Given** the mock-oauth2-server login form is displayed
- **When** a developer enters "Alice" as username in one session and "Bob" in another
- **Then** each session receives a token with the respective user's claims

## Token Endpoints

### Token endpoint available

- **Given** the mock-oauth2-server is running
- **When** a valid authorization code is exchanged at the token endpoint
- **Then** a JWT access token and ID token are returned

### JWKS endpoint available

- **Given** the mock-oauth2-server is running
- **When** `GET http://localhost:8888/default/jwks` is called
- **Then** a valid JWKS response is returned containing the signing keys

### Userinfo endpoint available

- **Given** the mock-oauth2-server is running
- **And** a valid access token has been issued
- **When** `GET http://localhost:8888/default/userinfo` is called with the access token
- **Then** the user's claims (name, email, picture) are returned

## Environment Variables

### OIDC variables in .env.example

- **Given** the `.env.example` file
- **When** a developer reads it
- **Then** it contains `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, and `OIDC_CLIENT_SECRET` with default values pointing to the local mock server

### Default OIDC_ISSUER_URI points to mock server

- **Given** the `.env.example` defaults are used
- **When** the OIDC_ISSUER_URI is resolved
- **Then** it equals `http://localhost:8888/default`

### Default OIDC_CLIENT_ID matches mock config

- **Given** the `.env.example` defaults are used
- **When** the OIDC_CLIENT_ID is resolved
- **Then** it equals `open-crm`
- **And** it matches the client ID configured in `mock-oauth2-config.json`

### Default OIDC_CLIENT_SECRET matches mock config

- **Given** the `.env.example` defaults are used
- **When** the OIDC_CLIENT_SECRET is resolved
- **Then** it equals `open-crm-secret`
- **And** it matches the client secret configured in `mock-oauth2-config.json`

### Variables not yet assigned to services

- **Given** the `docker-compose.yml` file
- **When** the backend and frontend service definitions are inspected
- **Then** neither service has `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, or `OIDC_CLIENT_SECRET` in its environment section

## Configuration File

### mock-oauth2-config.json exists in repo root

- **Given** the repository
- **When** the root directory is listed
- **Then** `mock-oauth2-config.json` exists

### Config enables interactive login

- **Given** the `mock-oauth2-config.json` file
- **When** its content is inspected
- **Then** `interactiveLogin` is set to `true`

### Config defines client credentials

- **Given** the `mock-oauth2-config.json` file
- **When** its content is inspected
- **Then** it contains a client configuration with ID `open-crm` and secret `open-crm-secret`
