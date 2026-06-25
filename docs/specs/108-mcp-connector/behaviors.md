# Behaviors: MCP Server (client-agnostic)

Scenarios are grouped by phase. **Phase 1** (Profile A — API key, Onyx) is the current slice; **Phase 2** (Profile B — per-user OIDC, Claude) scenarios are retained and marked so they are not lost.

---

## Phase 1 — Authentication (Profile A: API key)

### MCP endpoint requires an API key

- **Given** the backend runs with `openelements.mcp.enabled=true` and `auth.api-key.enabled=true`
- **When** a client sends `POST /mcp` without an `X-API-Key` header
- **Then** the response is `401 Unauthorized`
- **And** no audit log entry is created

### A valid API key authenticates a POST to /mcp

- **Given** a valid CRM API key `crm_…` exists in `api_keys`
- **When** a client sends `POST /mcp` with header `X-API-Key: crm_…` and a valid JSON-RPC body
- **Then** the request is processed (the dedicated `/mcp` chain allows POST, unlike the GET-only `/api/external` chain)
- **And** the response is a valid JSON-RPC result

### An invalid or revoked API key is rejected

- **Given** an `X-API-Key` value that does not match any active key (never existed, or was deleted)
- **When** `POST /mcp` is called with that header
- **Then** the response is `401 Unauthorized`
- **And** revocation takes effect immediately (validation happens per request)

### CSRF does not block the machine-to-machine endpoint

- **Given** a valid API key
- **When** `POST /mcp` is called without any CSRF token
- **Then** the request succeeds (CSRF is disabled on the `/mcp/**` chain)

### Master switch disables the endpoint entirely

- **Given** the backend is started with `openelements.mcp.enabled=false` (the default)
- **Then** no MCP-related beans are registered in the application context (no `McpSyncServer`, no `mcpRouterFunction`)
- **And** the `/mcp` security chain is absent, so `/mcp` is not served as an MCP endpoint
- *(Note: with the endpoint absent, a request to `/mcp` is handled by the default JWT chain and is rejected/closed rather than returning a tool response — the original 404 expectation was revised to bean-absence during implementation.)*

## Phase 1 — Tool discovery

### `tools/list` returns the Phase 1 catalog

- **Given** an authenticated MCP session (API key)
- **When** the client sends `{ "method": "tools/list" }`
- **Then** the response contains exactly: `search`, `list_companies`, `get_company`, `list_contacts`, `get_contact`, `list_tags`, `get_tag`, `list_company_comments`, `list_contact_comments`
- **And** no `task`, `user`, API-key, or webhook tool is present
- **And** each entry has a `name`, a non-empty `description`, and an `inputSchema` matching the documented parameters

### Tool descriptions instruct the model to paginate

- **Given** the registered tool set
- **When** a collection tool's description is inspected
- **Then** it states that results are paginated and that `hasMore=true` means further pages must be fetched

### Tool list is stable across calls

- **Given** the registered tool set
- **When** `tools/list` is called twice
- **Then** names, descriptions, and input schemas are identical between calls

## Phase 1 — Read tools, happy paths

### `list_contacts` returns a paginated envelope with completeness signal

- **Given** 200 contacts match a filter
- **When** the client calls `list_contacts` with `page=0`, `size=20`
- **Then** the response envelope has `items.length == 20`, `page == 0`, `size == 20`, `totalCount == 200`, `hasMore == true`

### Last page reports `hasMore=false`

- **Given** 35 companies exist
- **When** the client calls `list_companies` with `page=1`, `size=20`
- **Then** `items.length == 15`, `totalCount == 35`, `hasMore == false`

### `get_company` returns a single company by id

- **Given** a company with id `c1`
- **When** the client calls `get_company` with `id=c1`
- **Then** the response is the matching `CompanyDto` including finance fields when present

### `get_contact` for an unknown id returns a not-found error

- **Given** no contact with id `x` exists
- **When** the client calls `get_contact` with `id=x`
- **Then** the JSON-RPC response is an error whose message identifies the missing entity
- **And** a failure audit entry is recorded (see audit section)

### `list_contacts` applies compound filters

- **Given** contacts with mixed languages and Brevo origins
- **When** the client calls `list_contacts` with `search="anna"`, `language="DE"`, `brevo=false`
- **Then** only contacts matching all three filters are returned

### `search` returns grouped results across all four entity types

- **Given** Meilisearch is up and indices are populated
- **When** the client calls `search` with `q="Maier"`, `limit=10`
- **Then** the response is a `GlobalSearchResultDto` with `companies`, `contacts`, `tags`, and `comments` sections
- **And** each hit carries `id`, `label`, `snippet`, `score`, `ownerType`, `ownerId`

### `search` while Meilisearch is bootstrapping signals unavailability

- **Given** the initial index bootstrap (spec 104) is still in progress
- **When** the client calls `search`
- **Then** the JSON-RPC response is an error indicating the search index is temporarily bootstrapping

### Comment tools return full text in a paginated envelope

- **Given** a company with 120 comments
- **When** the client calls `list_company_comments` with that company id, `page=0`, `size=50`
- **Then** the envelope contains 50 `CommentDto` entries with **full comment text**, `totalCount == 120`, `hasMore == true`

## Phase 1 — Pagination bounds

### Default page size applies when omitted

- **Given** `default-page-size=20` and more than 20 records
- **When** a list tool is called with no `size`
- **Then** at most 20 items are returned

### `size` above the cap is clamped

- **Given** `max-page-size=50`
- **When** a list tool is called with `size=200`
- **Then** at most 50 items are returned, identically for `size=51`, `100`, or `10000`

### Negative or zero `size` is rejected

- **When** a list tool is called with `size=0` or `size=-5`
- **Then** the JSON-RPC response is an "invalid parameter" error
- **And** a failure audit entry is recorded

### `page` past the last page returns an empty envelope

- **Given** 35 companies exist
- **When** the client calls `list_companies` with `page=10`, `size=20`
- **Then** `items` is empty, `totalCount == 35`, `hasMore == false`

## Phase 1 — Authorization & exposure

### Any valid API key can call all Phase 1 tools

- **Given** any active API key
- **When** the key is used to call any Phase 1 tool
- **Then** the call succeeds (no per-key scoping exists yet)

### Unknown tool names are not callable

- **When** the client attempts `delete_company` or any name not in the catalog
- **Then** the JSON-RPC response is an "unknown tool" error

### Administrative and deferred entities are not exposed

- **When** any caller runs `tools/list`
- **Then** no tool references API keys, webhooks, or the audit log
- **And** no `task` or `user` tool is present in Phase 1

## Phase 1 — Audit logging

### Every successful tool call records an audit entry attributed to the key

- **Given** a request authenticated with API key named `onyx-prod`
- **When** the client calls `list_contacts`
- **Then** exactly one `AuditLogEntity` is persisted with `entityType="MCP"`, `action=INSERT`, `user=<SYSTEM user>`, and `name` encoding the tool and key, e.g. `"list_contacts [apikey:onyx-prod]"`
- **And** `createdAt` is the time of the call

### Failed tool calls still record an audit entry

- **Given** a request authenticated with API key named `onyx-prod`
- **When** the client calls `get_contact` with a malformed id
- **Then** an `AuditLogEntity` is persisted referencing the tool, the failure, and the key name
- **And** the response remains the JSON-RPC error

### Unauthenticated requests are not audited

- **When** `POST /mcp` is called without a valid key
- **Then** the response is `401 Unauthorized` and no `AuditLogEntity` is created

## Phase 1 — Configuration

### Page-size config is honored

- **Given** `default-page-size=10` and `max-page-size=30`
- **When** a list tool is called without `size`
- **Then** at most 10 items are returned
- **And** a call with `size=100` returns at most 30 items

## Phase 1 — Production-readiness check (manual, GDPR)

Not executable; reproduced so the review checklist lives with the spec. Applies to the self-hosted-EU Onyx + EU-hosted LLM profile.

### AVV with the LLM provider is signed before rollout

- **Given** the implementation is complete
- **When** an operator prepares to enable `/mcp` in a real environment
- **Then** a signed AVV/DPA with the EU-hosted LLM provider covering CRM-category data must be on file (or rollout is blocked)

### Legal basis and privacy notice are in place

- **When** an operator prepares rollout
- **Then** the documented legal basis (Art. 6 (1) f, with balancing) exists
- **And** the public privacy policy and internal employee notice mention transmission of CRM data to the LLM provider on request

### Onyx chat-log retention is defined

- **When** an operator prepares rollout
- **Then** a retention/deletion policy for Onyx's stored chat history (which contains retrieved CRM data) exists, reachable by data-subject erasure requests

### Works-council agreement where applicable

- **Given** a works council exists
- **When** an operator prepares rollout
- **Then** an agreement covering Onyx-side logging of employee queries is in place

### Privilege-expansion review before enabling

- **Given** existing API keys have no scopes
- **When** an operator prepares to enable MCP
- **Then** existing keys are reviewed/rotated, acknowledging that enabling MCP grants every active key full read access to personal CRM data

## Phase 1 — End-to-end smoke test (manual)

### Adding the MCP server in Onyx works end to end

- **Given** the backend is deployed with `openelements.mcp.enabled=true`, `auth.api-key.enabled=true`, and a CRM API key exists
- **And** a self-hosted EU Onyx with an EU-hosted GDPR-compliant LLM
- **When** an admin adds the MCP server in Onyx (URL `https://<crm-host>/mcp`, HTTP transport, Shared-Key sending `X-API-Key`)
- **Then** Onyx validates the connection and lists the Phase 1 tools
- **And** asking Onyx "Find the contact named Maier" triggers a `search` call and a human-readable answer
- **And** an audit entry for `search [apikey:<name>]` is visible at `/admin/audit-log`

---

## Phase 2 — Authentication (Profile B: per-user OIDC, Claude)

### Protected resource metadata is publicly reachable

- **Given** the backend runs with `openelements.mcp.enabled=true` and `auth.oidc.enabled=true`
- **When** an unauthenticated client requests `GET /.well-known/oauth-protected-resource`
- **Then** the response is `200 OK` and the JSON body contains an `authorization_servers` array with the configured Authentik issuer URL
- **And** the response is reachable without an `Authorization` header

### MCP endpoint accepts a Bearer JWT

- **Given** the `open-crm-mcp` OAuth client is registered in Authentik and Claude has completed the Authorization Code flow
- **When** `POST /mcp` is called with `Authorization: Bearer <jwt>` signed by the trusted JWKS
- **Then** the request succeeds
- **And** a JWT signed by an unknown issuer yields `401 Unauthorized` with no audit entry

### Both auth profiles coexist on the `/mcp` chain

- **Given** both `auth.api-key.enabled=true` and `auth.oidc.enabled=true`
- **When** a request presents either a valid `X-API-Key` or a valid Bearer JWT
- **Then** either is accepted

## Phase 2 — Per-user actor & audit

### First MCP call for an unknown sub auto-provisions a user

- **Given** a valid JWT whose `sub` has no `UserEntity`
- **When** the user calls any tool for the first time
- **Then** a `UserEntity` is created (same path as frontend login, spec 065)
- **And** the audit entry references the newly created real user (not the SYSTEM user)

### Subsequent calls attribute audit to the real user

- **Given** an auto-provisioned user `u1`
- **When** `u1` calls a tool
- **Then** the `AuditLogEntity` has `entityType="MCP"`, `name="<tool>"`, `user=u1`

## Phase 2 — Deferred tools (ship with or after the OIDC profile)

### Task tools

- **Given** tasks exist with statuses `OPEN`, `IN_PROGRESS`, `DONE`
- **When** the client calls `list_tasks` without `status`
- **Then** tasks of all statuses are returned in a paginated envelope; `status=OPEN` restricts accordingly

### Reduced `list_users` projection

- **When** the client calls `list_users`
- **Then** each entry contains exactly `id` and `displayName`
- **And** no entry contains `email` or `avatar`, identically for all roles

## Phase 2 — Production-readiness check (manual, GDPR — Anthropic / USA)

### DPA, Zero-Data-Retention, DPIA, privacy notice, works council

- **When** an operator prepares to enable the Claude/OIDC profile in production
- **Then** a signed DPA with Anthropic, written Zero-Data-Retention confirmation, a DPIA covering the third-country transfer (EU-US DPF or SCCs), an updated privacy notice, and (where applicable) a works-council agreement must all be on file (or rollout is blocked)
