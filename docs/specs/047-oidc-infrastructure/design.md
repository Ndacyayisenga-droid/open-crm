# Design: OIDC Infrastructure

## GitHub Issue

—

## Summary

Prepare the Docker Compose infrastructure for OIDC-based authentication. Add a mock-oauth2-server as a local development service and define OIDC environment variables that can point to either the mock server (local) or a real Authentik instance (production/Coolify). No backend or frontend code changes — this spec only sets up the infrastructure that later specs will integrate against.

## Goals

- Provide a local OIDC provider for development without requiring access to a real Authentik instance
- Define standardized OIDC environment variables usable by both backend and frontend
- Keep local development and production deployment configurable via the same variable names

## Non-goals

- Backend Spring Security integration (separate spec)
- Frontend OIDC login flow (separate spec)
- Assigning OIDC variables to backend/frontend services in docker-compose (separate specs)
- Authentik setup/configuration
- Role-based access control

## Technical Approach

### mock-oauth2-server

Add [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) as a Docker service in `docker-compose.override.yml` (local development only). The mock server provides a full OIDC implementation:

- Discovery endpoint (`/.well-known/openid-configuration`)
- Authorization Code Flow with interactive login form
- Token endpoint
- Userinfo endpoint
- JWKS endpoint

**Docker image:** `ghcr.io/navikt/mock-oauth2-server`

**Configuration:** A `mock-oauth2-config.json` file in the repository root configures the mock server with:
- `interactiveLogin: true` — shows a login form where developers can enter custom username and claims
- Token callbacks providing `name`, `email`, and `picture` claims
- A pre-configured client ID and client secret matching the `.env.example` defaults

**Rationale:** A config file is needed because mock-oauth2-server cannot be fully configured via environment variables alone. The `JSON_CONFIG_PATH` environment variable in the Compose service points to the mounted config file.

### Environment Variables

Three OIDC variables are added to `.env.example` with defaults pointing to the local mock server:

| Variable | Description | Default (local) |
|----------|-------------|-----------------|
| `OIDC_ISSUER_URI` | OIDC issuer / discovery URL | `http://localhost:8888/default` |
| `OIDC_CLIENT_ID` | OAuth2 client ID | `open-crm` |
| `OIDC_CLIENT_SECRET` | OAuth2 client secret | `open-crm-secret` |

In Coolify/production, these are set to the real Authentik values (e.g., `https://auth.example.com/application/o/open-crm/`).

The variables are **not yet assigned** to the backend or frontend services in `docker-compose.yml` — that happens in the respective integration specs.

### Docker Compose Changes

**`docker-compose.override.yml`** — add mock-oauth2-server service:

```yaml
mock-oauth2:
  image: ghcr.io/navikt/mock-oauth2-server
  ports:
    - "${MOCK_OAUTH2_PORT:-8888}:8080"
  volumes:
    - ./mock-oauth2-config.json:/config.json
  environment:
    - JSON_CONFIG_PATH=/config.json
    - LOG_LEVEL=INFO
```

The mock server only runs locally (override file) and is not deployed to Coolify.

### Files Affected

**New:**
- `mock-oauth2-config.json` — mock-oauth2-server configuration (interactive login, claims, client)

**Modified:**
- `docker-compose.override.yml` — add mock-oauth2 service
- `.env.example` — add OIDC variables and mock-oauth2 port

## Open Questions

None — all details resolved during design discussion.
