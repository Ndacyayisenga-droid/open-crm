# Open CRM

An open-source CRM system designed for startups to manage customers, contacts, and companies with ease.

**Status:** Early development

## Overview

Open CRM provides a straightforward way to organize and maintain business relationships.
It focuses on the core needs of small teams: keeping track of companies, contacts, and customer records without the
complexity of enterprise CRM solutions.

## Key Features

- **Company Management** — Create and manage company records with relevant business details
- **Contact Management** — Track individual contacts and their associations with companies
- **Image Upload** — Upload and manage company logos (SVG, PNG, JPEG) and contact photos (JPEG) with thumbnails in list
  views
- **Customer Management** — Manage customer relationships and history
- **Single Sign-On (SSO)** — Integration with [Authentik](https://goauthentik.io/) for authentication and user
  management
- **Brevo Sync** — Automatic synchronization of customers and companies with [Brevo](https://www.brevo.com/) for
  marketing and communication workflows

## Tech Stack

- **Backend:** Spring Boot 3.4 (Java 21)
- **Frontend:** Next.js 15 (React 19, TypeScript, Tailwind CSS, shadcn/ui)
- **Database:** PostgreSQL 17
- **Authentication:** Authentik (SSO via OpenID Connect) — planned
- **External Integrations:** Brevo API — planned

## Architecture

```
┌────────────┐     ┌──────────────┐     ┌────────────┐
│  Frontend  │────▶│  Spring Boot │────▶│ PostgreSQL │
└────────────┘     │   Backend    │     └────────────┘
                   └──────┬───────┘
                          │
                ┌─────────┼─────────┐
                ▼                   ▼
         ┌────────────┐     ┌────────────┐
         │  Authentik │     │   Brevo    │
         │   (SSO)    │     │   (Sync)   │
         └────────────┘     └────────────┘
```

## Prerequisites

- **Java 21** — recommended via [SDKMAN!](https://sdkman.io/) (see `backend/.sdkmanrc`)
- **Node.js 22** — recommended via [nvm](https://github.com/nvm-sh/nvm) (see `frontend/.nvmrc`)
- **pnpm** — package manager for the frontend
- **Docker & Docker Compose** — for running the full stack
- **PostgreSQL 17** — for standalone backend development (or use Docker)

## Getting Started

### Setup

1. Clone the repository
2. Copy the environment file:
   ```bash
   cp .env.example .env
   ```
3. Adjust values in `.env` if needed (defaults work for local development)

### Run with Docker Compose (Local Development)

Start all services (PostgreSQL, backend, frontend):

```bash
docker compose up --build
```

- Frontend: [http://localhost:4001](http://localhost:4001)
- Backend API: [http://localhost:9081/api/health](http://localhost:9081/api/health)
- Swagger UI: [http://localhost:9081/swagger-ui.html](http://localhost:9081/swagger-ui.html)

Port bindings are defined in `docker-compose.override.yml`, which Docker Compose merges automatically. The main
`docker-compose.yml` contains no `ports` entries — see [Docker Compose & Coolify](#docker-compose--coolify) for details.

Stop services:

```bash
docker compose down
```

Stop services and remove database data:

```bash
docker compose down -v
```

### Run Locally (with Docker for infrastructure)

Start the database and mock OAuth2 server via Docker Compose:

```bash
docker compose up -d db mock-oauth2
```

Then run backend and frontend locally:

**Backend:**

```bash
cd backend
sdk env install          # install Java 21 via SDKMAN!
./mvnw spring-boot:run   # starts on port 8080
```

The default values in `application.yml` already point to `localhost:5432` (database) and `localhost:8888` (mock OAuth2),
so no extra configuration is needed.

For verbose SQL logging and debug output, activate the `local` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend:**

```bash
cd frontend
nvm install              # install Node.js 22 via nvm
pnpm install
pnpm dev                 # starts on port 3000
```

The default values in `frontend/.env.local` already point to `localhost:8080` (backend) and `localhost:8888` (mock
OAuth2), so no extra configuration is needed.

Stop the infrastructure services when done:

```bash
docker compose down       # keeps data
docker compose down -v    # removes database data
```

### Build

**Backend:**

```bash
cd backend && ./mvnw clean verify
```

> Backend tests run against a real PostgreSQL 17 container started by
> [Testcontainers](https://testcontainers.com/). **Docker (Docker Desktop or
> any Docker-compatible daemon) must be running** before `./mvnw test` /
> `./mvnw verify` is invoked. For faster local iteration, enable container
> reuse by adding the following line to `~/.testcontainers.properties`:
>
> ```properties
> testcontainers.reuse.enable=true
> ```
>
> With reuse on, the same Postgres container survives between `mvn test`
> runs and the per-suite startup cost drops from ~3–5 s to ~200 ms.

**Frontend:**

```bash
cd frontend && pnpm install && pnpm test && pnpm build
```

## Docker Compose & Coolify

The project uses a split Docker Compose setup to support both local development and [Coolify](https://coolify.io/)
deployment:

| File                          | Purpose                                                         |
|-------------------------------|-----------------------------------------------------------------|
| `docker-compose.yml`          | Service definitions without port bindings (used by Coolify)     |
| `docker-compose.override.yml` | Host port bindings for local development (merged automatically) |

This split is necessary because Coolify uses Traefik as a reverse proxy to route traffic via FQDNs. Host port bindings
are not needed in Coolify and cause port allocation conflicts during deployment.

For more details, see [DOCKER-COMPOSE-COOLIFY.md](DOCKER-COMPOSE-COOLIFY.md).

### Deploy to Coolify

1. In Coolify, create a new application and connect the GitHub repository
2. Select **Docker Compose** as the build method
3. Set the branch to `main`
4. Configure the FQDNs for the services in Coolify:
    - **frontend** — e.g., `crm.example.com`
    - **backend** — e.g., `crm-backend.example.com`
5. Add the required environment variables in Coolify:
    - `DB_NAME` — PostgreSQL database name (e.g., `opencrm`)
    - `DB_USER` — PostgreSQL user (e.g., `opencrm`)
    - `DB_PASSWORD` — PostgreSQL password
6. Deploy — Coolify builds the Docker images and starts the services
7. Traefik automatically handles TLS certificates and routes traffic to the containers

After deployment:

- Frontend: `https://crm.example.com`
- Backend / Swagger UI: `https://crm-backend.example.com/swagger-ui.html`

## Authentication

Open CRM uses OpenID Connect (OIDC) for authentication. Locally,
a [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) runs automatically via Docker Compose. For
production, you connect a real [Authentik](https://goauthentik.io/) instance.

### Local Development (default)

No configuration needed — `docker compose up` starts the mock-oauth2-server automatically. The default `.env` values
point to the local mock server. The mock server shows an interactive login form where you can enter any username and
claims.

### Configure Authentik for Production

#### 1. Create an OAuth2/OpenID Provider in Authentik

1. In Authentik, go to **Applications → Providers → Create**
2. Select **OAuth2/OpenID Provider**
3. Configure:
    - **Name:** `Open CRM`
    - **Authorization flow:** Select your default authorization flow
    - **Client type:** Confidential
    - **Client ID:** Note this value (e.g., `open-crm`)
    - **Client Secret:** Note this value
    - **Redirect URIs:** `https://crm.example.com/api/auth/callback/oidc`
    - **Logout URI:** `https://crm.example.com` (required for logout redirect back to the app)
    - **Scopes:** `openid`, `profile`, `email`, `offline_access`
4. Save the provider

> **Important:** The `offline_access` scope is required for refresh tokens. Without it, users will be logged out when
> the access token expires (typically after a few minutes).

#### 2. Create an Application in Authentik

1. Go to **Applications → Applications → Create**
2. Configure:
    - **Name:** `Open CRM`
    - **Slug:** `open-crm`
    - **Provider:** Select the provider created above
3. Save the application

#### 3. Set Environment Variables

Set these variables in your deployment environment (e.g., Coolify):

```env
# Frontend OIDC configuration
OIDC_ISSUER_URI=https://auth.example.com/application/o/open-crm/
OIDC_CLIENT_ID=open-crm
OIDC_CLIENT_SECRET=your-client-secret-from-authentik
AUTH_SECRET=generate-a-random-secret-here
AUTH_URL=https://crm.example.com

# Backend JWT validation
OIDC_JWK_SET_URI=https://auth.example.com/application/o/open-crm/jwks/
```

- `OIDC_ISSUER_URI` — The Authentik OpenID issuer URL for your application
- `OIDC_CLIENT_ID` / `OIDC_CLIENT_SECRET` — From the Authentik provider configuration
- `AUTH_SECRET` — A random string for encrypting session cookies (generate with `openssl rand -base64 32`)
- `AUTH_URL` — The public URL of your frontend
- `OIDC_JWK_SET_URI` — The JWKS endpoint for JWT signature validation (usually `{issuer}/jwks/`)

#### 4. Verify

After deploying with the new variables:

1. Open the frontend URL — you should be redirected to the Authentik login page
2. Log in with an Authentik user
3. You should be redirected back to the CRM with your name displayed in the sidebar

## API Keys

API keys allow external services to access the CRM REST API without an interactive OIDC login. Keys grant **read-only
access** (GET endpoints only) — write operations (POST, PUT, DELETE) are rejected with 403 Forbidden.

### Managing API Keys

API keys can be managed via the frontend (sidebar: "API Keys") or the REST API:

```bash
# Create an API key (requires JWT auth — use the Bearer token from the Admin page)
curl -X POST https://crm-backend.example.com/api/api-keys \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "CI Pipeline"}'
```

The response includes the raw key (`crm_...`) — **copy it immediately**, it is shown only once.

```bash
# List API keys
curl https://crm-backend.example.com/api/api-keys \
  -H "Authorization: Bearer <your-jwt-token>"

# Delete an API key
curl -X DELETE https://crm-backend.example.com/api/api-keys/<key-id> \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Using API Keys

Pass the key in the `X-API-Key` header:

```bash
# List companies
curl https://crm-backend.example.com/api/companies \
  -H "X-API-Key: crm_your_key_here"

# Get a specific contact
curl https://crm-backend.example.com/api/contacts/<contact-id> \
  -H "X-API-Key: crm_your_key_here"

# Paginated contacts
curl "https://crm-backend.example.com/api/contacts?page=0&size=10" \
  -H "X-API-Key: crm_your_key_here"

# This will be rejected (write access not allowed):
curl -X POST https://crm-backend.example.com/api/companies \
  -H "X-API-Key: crm_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test"}'
# -> 403 Forbidden: "API keys only grant read-only access"
```

## Webhooks

Webhooks notify external services when data changes in the CRM. When a company, contact, task, tag, comment, or user is
created, updated, or deleted, an HTTP POST is sent to all registered webhook URLs with a JSON payload describing the
event.

### Managing Webhooks

Webhooks can be managed via the frontend (sidebar: "Webhooks") or the REST API:

```bash
# Create a webhook
curl -X POST https://crm-backend.example.com/api/webhooks \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://your-service.example.com/webhook"}'

# List webhooks (shows lastStatus and lastCalledAt)
curl https://crm-backend.example.com/api/webhooks \
  -H "Authorization: Bearer <your-jwt-token>"

# Ping a webhook to test connectivity
curl -X POST https://crm-backend.example.com/api/webhooks/<webhook-id>/ping \
  -H "Authorization: Bearer <your-jwt-token>"

# Delete a webhook
curl -X DELETE https://crm-backend.example.com/api/webhooks/<webhook-id> \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Webhook Payload

Every webhook call sends a JSON POST with this structure:

```json
{
    "eventId": "a1b2c3d4-e5f6-...",
    "eventType": "COMPANY_CREATED",
    "timestamp": "2026-04-05T14:30:00Z",
    "entityId": "550e8400-...",
    "data": {
        "id": "550e8400-...",
        "name": "Acme Corp"
    }
}
```

For delete events, `data` is `null`. The `eventId` (UUID) can be used for idempotency.

**Event types:** `COMPANY_CREATED`, `COMPANY_UPDATED`, `COMPANY_DELETED`, `CONTACT_CREATED`, `CONTACT_UPDATED`,
`CONTACT_DELETED`, `COMMENT_CREATED`, `COMMENT_UPDATED`, `COMMENT_DELETED`, `TAG_CREATED`, `TAG_UPDATED`, `TAG_DELETED`,
`TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`, `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`, `PING`

### Testing Webhooks Locally

To test webhooks on your local machine, you need a tool that creates a temporary HTTP endpoint and shows incoming
requests. Here are some options for Mac/Linux:

**Option 1: Python one-liner (no install needed)**

```bash
# Start a simple HTTP server that logs all incoming requests
python3 -c "
from http.server import HTTPServer, BaseHTTPRequestHandler
import json

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(length)
        print('\n--- Webhook received ---')
        print(json.dumps(json.loads(body), indent=2))
        self.send_response(200)
        self.end_headers()

HTTPServer(('0.0.0.0', 9999), Handler).serve_forever()
"
```

Then register `http://host.docker.internal:9999` as the webhook URL (if the CRM runs in Docker) or
`http://localhost:9999` (if running standalone).

**Option 2: netcat (quick & dirty)**

```bash
# Listen for a single webhook call and print the raw request
while true; do nc -l 9999; done
```

**Option 3: webhook.site (no local setup)**

Visit [webhook.site](https://webhook.site) to get a temporary public URL. Register that URL as a webhook and see
incoming requests in the browser.

### Webhook Status Tracking

Each webhook tracks the result of its last call:

| `lastStatus` | Meaning             |
|--------------|---------------------|
| `null`       | Never called        |
| `200`        | OK                  |
| `0`          | Connection error    |
| `-1`         | Timeout (10s limit) |
| `4xx`/`5xx`  | HTTP error          |

Use the PING endpoint or check `lastStatus` in the webhook list to verify connectivity.

## License

See [LICENSE](LICENSE) for details.
