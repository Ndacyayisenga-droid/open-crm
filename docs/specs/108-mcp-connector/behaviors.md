# Behaviors: MCP Connector

## Connector setup (OAuth handshake)

### Protected resource metadata is publicly reachable

- **Given** the backend is running with `openelements.mcp.enabled=true`
- **When** an unauthenticated client requests `GET /.well-known/oauth-protected-resource`
- **Then** the response is `200 OK`
- **And** the JSON body contains an `authorization_servers` array with the configured Authentik issuer URL
- **And** the response is reachable without an `Authorization` header

### MCP endpoint requires authentication

- **Given** the backend is running
- **When** a client sends `POST /mcp` without an `Authorization` header
- **Then** the response is `401 Unauthorized`
- **And** the response carries a `WWW-Authenticate: Bearer` header pointing to the resource-metadata URL

### Successful OAuth handshake produces a usable token

- **Given** the `open-crm-mcp` OAuth client is registered in Authentik
- **And** a user has pasted the matching Client ID and Client Secret into Claude's "Add Custom Connector" dialog
- **When** Claude completes the Authorization Code flow against Authentik
- **Then** Claude receives an access token (JWT) signed by the same JWKS the backend trusts
- **And** subsequent `POST /mcp` calls with `Authorization: Bearer <token>` succeed

### Token signed by an unknown issuer is rejected

- **Given** the backend trusts only the configured Authentik JWKS
- **When** `POST /mcp` is called with a JWT signed by a different issuer
- **Then** the response is `401 Unauthorized`
- **And** no audit log entry is created

### Token from the existing `open-crm` web client is technically accepted

- **Given** strict audience validation is deferred (see TODO.md)
- **And** a valid JWT issued for the `open-crm` web frontend client is presented
- **When** `POST /mcp` is called with that token
- **Then** the request is processed normally
- **And** the deferred audience check is the documented hardening path

## Tool discovery

### `tools/list` returns the full v1 catalog

- **Given** an authenticated MCP session
- **When** Claude sends `{ "method": "tools/list" }`
- **Then** the response contains exactly the following tools: `search`, `list_companies`, `get_company`, `list_contacts`, `get_contact`, `list_tasks`, `get_task`, `list_tags`, `get_tag`, `list_company_comments`, `list_contact_comments`, `list_task_comments`, `list_users`
- **And** each tool entry has a `name`, a non-empty `description`, and an `inputSchema` matching the documented parameters

### Tool descriptions are stable identifiers

- **Given** the registered tool set
- **When** `tools/list` is called twice in a row
- **Then** the names, descriptions, and input schemas are byte-identical between calls

## Read tools — happy paths

### `list_companies` returns a paginated page

- **Given** 35 companies exist in the database
- **And** an authenticated MCP session
- **When** Claude calls `list_companies` with `page=0`, `size=20`
- **Then** the response contains 20 `CompanyDto` entries
- **And** the response carries the total element count (`35`)
- **And** each entry includes finance fields (`bankName`, `bic`, `iban`, `vatId`) when present

### `get_company` returns a single company by id

- **Given** a company with id `c1`
- **When** Claude calls `get_company` with `id=c1`
- **Then** the response is the matching `CompanyDto`

### `get_company` for an unknown id returns a not-found error

- **Given** no company with id `c-missing` exists
- **When** Claude calls `get_company` with `id=c-missing`
- **Then** the JSON-RPC response is an error with code `-32602` (Invalid params) or the SDK's "not found" mapping
- **And** the human-readable message identifies the missing entity

### `search` proxies the global search and returns grouped results

- **Given** Meilisearch is up and indices are populated
- **When** Claude calls `search` with `q="Maier"`, `limit=10`
- **Then** the response is a `GlobalSearchResultDto` with `companies`, `contacts`, `tags`, and `comments` sections
- **And** each hit carries `id`, `label`, `snippet`, `score`, `ownerType`, `ownerId`

### `search` while Meilisearch is bootstrapping returns 503-equivalent

- **Given** Meilisearch is reachable but the initial index bootstrap (spec 104) is still in progress
- **When** Claude calls `search`
- **Then** the JSON-RPC response is an error indicating the service is temporarily unavailable
- **And** the error message references "search index is bootstrapping"

### `list_contacts` accepts compound filters

- **Given** contacts exist with mixed languages and Brevo origins
- **When** Claude calls `list_contacts` with `search="anna"`, `language="de"`, `brevo=false`, `page=0`, `size=20`
- **Then** the response only contains contacts matching all three filters

### `list_tasks` defaults to all statuses

- **Given** tasks exist with statuses `OPEN`, `IN_PROGRESS`, and `DONE`
- **When** Claude calls `list_tasks` without a `status` parameter
- **Then** the response contains tasks of all three statuses
- **And** when `status=OPEN` is supplied, the response is restricted accordingly

### `list_company_comments` returns the latest comments capped at 50

- **Given** a company with 120 comments
- **When** Claude calls `list_company_comments` with that company id
- **Then** the response contains the 50 most recent comments
- **And** the response indicates the cap was applied (e.g. via a count vs returned-count mismatch)

## Reduced users projection

### `list_users` returns only id and displayName

- **Given** the existing `UserDto` carries `id`, `displayName`, `email`, and `avatar` fields
- **When** Claude calls `list_users` with `page=0`, `size=20`
- **Then** each entry in the response contains exactly the fields `id` and `displayName`
- **And** no entry contains `email` or `avatar`
- **And** the response is identical for callers with role `user`, `ADMIN`, or `IT-ADMIN`

### `list_users` ignores attempts to request more fields

- **Given** a malicious client tries to extend the parameter set with `includeEmail=true`
- **When** Claude calls `list_users` with extra parameters
- **Then** the unknown parameter is rejected by the input schema or ignored
- **And** the response still contains only `id` and `displayName`

## Pagination

### Default page size is applied when omitted

- **Given** more than 20 records exist for some list tool
- **When** Claude calls the tool with no `size` parameter
- **Then** the response contains exactly 20 records (or fewer if not enough data exists)

### `size` above the hard cap is clamped to 50

- **Given** an authenticated MCP session
- **When** Claude calls `list_companies` with `size=200`
- **Then** the response contains at most 50 records
- **And** the behavior is identical whether `size=51`, `size=100`, or `size=10000` is requested

### Negative or zero `size` is rejected

- **When** Claude calls a list tool with `size=0` or `size=-5`
- **Then** the JSON-RPC response is an error with an "invalid parameter" code
- **And** no audit log entry is recorded for a successful list (a failure entry is recorded — see audit section)

### `page` past the last page returns an empty list

- **Given** 35 companies exist
- **When** Claude calls `list_companies` with `page=10`, `size=20`
- **Then** the response is a successful empty page
- **And** the total element count is still `35`

## Authorization (RBAC)

### Any authenticated user can call all v1 tools

- **Given** an authenticated user with only the default authority (no `ADMIN`, no `IT-ADMIN`)
- **When** that user calls any tool from the v1 catalog
- **Then** the call succeeds (subject to the entity actually being readable)

### Tools that are not in the catalog are not callable

- **Given** an authenticated user
- **When** Claude attempts to call `delete_company` or any name not in the catalog
- **Then** the JSON-RPC response is an error indicating the tool is unknown

### Administrative entities are not exposed

- **Given** the v1 tool catalog
- **When** any user calls `tools/list`
- **Then** no tool referencing API keys, webhooks, or audit log entries is present
- **And** no tool exposes the full Users view (email, avatar)

## Audit logging

### Every successful tool call records an audit entry

- **Given** an authenticated user `u1`
- **When** the user calls `list_companies`
- **Then** exactly one new `AuditLogEntity` is persisted with `entityType="MCP"`, `name="list_companies"`, `user=u1`, `action=INSERT`
- **And** the entry's `createdAt` is the time of the call

### Failed tool calls still record an audit entry

- **Given** an authenticated user `u1`
- **When** the user calls `get_company` with a malformed id
- **Then** an `AuditLogEntity` is persisted with `entityType="MCP"`, `name` mentioning the tool and the failure (e.g. `"get_company: invalid argument"`), `user=u1`
- **And** the response remains the JSON-RPC error

### Unauthenticated requests are not audited

- **Given** no token is presented
- **When** `POST /mcp` is called
- **Then** the response is `401 Unauthorized`
- **And** no `AuditLogEntity` is created

## User auto-provisioning

### First MCP call for an unknown sub creates a UserEntity

- **Given** a valid JWT whose `sub` does not correspond to any existing `UserEntity`
- **When** the user calls any MCP tool for the first time
- **Then** a new `UserEntity` is created with the claims from the JWT (same as the frontend login path in spec 065)
- **And** the audit log entry references the newly created user

### Repeated calls reuse the existing UserEntity

- **Given** a user has already triggered auto-provisioning
- **When** the same user calls another MCP tool
- **Then** no new `UserEntity` is created
- **And** the audit log entry references the existing user id

## Configuration switch

### `openelements.mcp.enabled=false` disables the endpoint entirely

- **Given** the backend is started with `openelements.mcp.enabled=false`
- **When** any client requests `POST /mcp` or `GET /.well-known/oauth-protected-resource`
- **Then** the response is `404 Not Found`
- **And** no MCP-related beans are registered in the application context

### Default-page-size and max-page-size are honored from config

- **Given** the backend is started with `openelements.mcp.default-page-size=10` and `max-page-size=30`
- **When** Claude calls `list_companies` without `size`
- **Then** the response contains at most 10 records
- **And** a call with `size=100` returns at most 30 records

## Production-readiness check (manual)

These are not executable scenarios but are reproduced here so the review checklist is captured alongside the spec.

### DPA with Anthropic is signed before production rollout

- **Given** the technical implementation is complete
- **When** the operator prepares to enable the MCP endpoint in a production environment
- **Then** the DPA with Anthropic must be on file (or the deployment is blocked)

### Privacy notice is updated before production rollout

- **Given** the technical implementation is complete
- **When** the operator prepares to enable the MCP endpoint in a production environment
- **Then** the public privacy policy and the internal employee notice must reflect the new transfer to Anthropic

### DPIA is on file before production rollout

- **Given** the technical implementation is complete
- **When** the operator prepares to enable the MCP endpoint in a production environment
- **Then** a documented DPIA covering the third-country transfer (USA / EU-US DPF) must exist

### Works council agreement is in place where applicable

- **Given** a works council exists in the operating organisation
- **When** the operator prepares to enable the MCP endpoint
- **Then** an agreement covering MCP-driven monitoring of employee activity must be in place, analogous to the pending "Updates view" agreement in TODO.md

## End-to-end smoke test (manual)

### Adding the connector in Claude works end to end

- **Given** the backend is deployed at `https://crm-backend.example.com` with `openelements.mcp.enabled=true`
- **And** the `open-crm-mcp` OAuth client is registered in Authentik
- **When** an employee adds a Custom Connector in Claude with URL `https://crm-backend.example.com/mcp`, Client ID, and Client Secret
- **Then** Claude completes the OAuth dance, registers the connector, and lists the v1 tools
- **And** asking Claude "Find me the contact named Maier" results in a `search` call and a human-readable answer in the chat
- **And** an audit log entry for `search` is visible at `/admin/audit-log` in the web frontend
