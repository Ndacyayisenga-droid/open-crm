# Implementation Steps: MCP Server — Phase 1 (Onyx / API key)

Scope: the Phase 1 slice of spec 108 (`design.md`). Phase 2 (Claude / per-user OIDC) is out of scope here.
Backend module: `backend/` (Spring Boot 3.5, Java 21). New code under `com.openelements.crm.mcp`.

---

## Step 1: Dependency + configuration properties ✅

- [x] Add `io.modelcontextprotocol.sdk:mcp-spring-webmvc` **pinned to `0.18.3`** to `backend/pom.xml` (version property `mcp-sdk.version`). Pulls `mcp-core:0.18.3` + `reactor-core`.
- [x] Create `McpProperties` (`@ConfigurationProperties("openelements.mcp")`): `enabled` (default `false`), `serverName` (default `"Open CRM"`), `serverVersion`, `maxPageSize` (default `50`), `defaultPageSize` (default `20`), nested `auth.apiKey.enabled` (default `true`), `auth.oidc.enabled` (default `false`). Registered via always-on `McpConfiguration`.
- [x] Register the properties in `application.yml` with documented defaults; keep `enabled: false` as the safe default.

**Acceptance criteria:**
- [x] Project compiles with the new dependency resolved.
- [x] `McpProperties` binds from YAML; `McpPropertiesTest` confirms defaults + override binding.
- [x] Reproducible-builds: version is pinned, no floating range.

**Related behaviors:** Configuration switch; page-size config.

---

## Step 2: `/mcp` security filter chain ✅

- [x] Create `McpSecurityConfig` with a `SecurityFilterChain` bean `@Order(0)` (ahead of spring-services `externalApiFilterChain` `@Order(1)` / `defaultFilterChain` `@Order(2)`), `securityMatcher("/mcp", "/mcp/**")`.
- [x] Reuse the spring-services `ApiKeyAuthenticationFilter` (constructed fresh from the `ApiKeyDataService` bean + the `apiKeyAuthenticationEntryPoint`) — confirmed method-agnostic via bytecode, so POST works; the GET-only rule lives only in the library's external chain.
- [x] Chain config: `anyRequest().authenticated()`; `csrf().disable()`; stateless session; `exceptionHandling` → JSON 401 entry point.
- [x] Gate on `@ConditionalOnProperty("openelements.mcp.enabled")` (class) + `auth.api-key.enabled` (bean, matchIfMissing).
- [x] Precedence verified: `@Order(0)` + `/mcp`-only matcher cannot swallow `/api/**`.

**Acceptance criteria:**
- [x] `POST /mcp` without `X-API-Key` → `401`.
- [x] `POST /mcp` with a valid key passes authentication (not 401/403; 404 until step 6 adds the handler).
- [x] An invalid/revoked key → `401`.
- [x] No CSRF token required. (`McpSecurityIntegrationTest`, 3/3.)

**Related behaviors:** Phase 1 — Authentication (all scenarios).

---

## Step 3: Actor + audit abstraction ✅

- [x] Create `McpActor` (record `{ UserEntity auditUser, String label }`) + `McpActorResolver`: Phase 1 → SYSTEM user + `label="apikey:<keyName>"` from the `ApiKeyEntity` principal. Seam designed so Phase 2 adds a JWT branch without changing callers.
- [x] Create `McpAuditService` writing one `AuditLogEntity` (`entityType="MCP"`, `action=INSERT`, `user=auditUser`, `name="<tool> [<label>]"`, `entityId` = target id or nil sentinel). Success + failure variants.
- [x] INFO logging records only `tool=<name> actor=<label>` (no arguments).

**Acceptance criteria:**
- [x] A tool call writes exactly one `MCP` audit row attributed to SYSTEM + key name.
- [x] A failed tool call writes a failure audit row encoding tool + error.
- [x] Actor resolves API-key principal → SYSTEM user. (`McpAuditIntegrationTest`, 3/3.)
- [ ] (Unauthenticated → no audit row: covered end-to-end in step 7 once the handler exists.)

**Related behaviors:** Phase 1 — Audit logging.

---

## Step 4: Pagination envelope ✅

- [x] Create `McpPage<T>` (record: `items`, `page`, `size`, `totalCount`, `hasMore`) with `from(Page<T>)` mapping `getTotalElements()`/`!isLast()`.
- [x] `McpPaging` helper: `resolveSize` (default → clamp to `maxPageSize`, reject `<=0`), `resolvePage` (default 0, reject negative), `toPageable(...)` (unsorted + sorted).

**Acceptance criteria:**
- [x] Envelope reports correct `totalCount`/`hasMore` for first and last pages.
- [x] `size > maxPageSize` clamps; `size <= 0` and negative `page` rejected. (`McpPagingTest`, 5/5.)

**Related behaviors:** Phase 1 — Pagination bounds; completeness signal.

---

## Step 5: Tools

- [ ] Create `McpToolFactory` producing `SyncToolSpecification`s for: `search`, `list_companies`, `get_company`, `list_contacts`, `get_contact`, `list_tags`, `get_tag`, `list_company_comments`, `list_contact_comments`.
- [ ] Each tool: a `McpSchema.Tool` (snake_case name, clear English description incl. pagination hint, JSON `inputSchema` for params) + a handler `(exchange, Map<String,Object> args) -> CallToolResult`.
- [ ] Handlers parse/validate args, call the existing service (`SearchService`, `CompanyService`, `ContactService`, `TagService`, comment list methods), wrap collections in `McpPage`, serialize to JSON text content, and record audit via `McpAuditService`.
- [ ] `search` maps `SearchService.search()`; surface the bootstrap state as a JSON-RPC error (mirrors the 503 from spec 104).
- [ ] `get_*` for unknown id → JSON-RPC error identifying the missing entity.
- [ ] Comment tools return full comment text in the `McpPage` envelope.

**Acceptance criteria:**
- [ ] Each tool returns the documented shape; collection tools return the envelope.
- [ ] Compound filters on `list_contacts` work; unknown id errors; search bootstrap error surfaces.

**Related behaviors:** Phase 1 — Tool discovery; Read tools happy paths.

---

## Step 6: MCP server wiring

- [ ] Create `McpServerConfig` (`@ConditionalOnProperty("openelements.mcp.enabled")`): build `WebMvcStreamableServerTransportProvider` (`mcpEndpoint("/mcp")`, shared `ObjectMapper`), build `McpSyncServer` via `McpServer.sync(provider).serverInfo(serverName, serverVersion).capabilities(tools=true).tools(<all specs>).build()`, and expose `transportProvider.getRouterFunction()` as a `RouterFunction<ServerResponse>` bean.
- [ ] Confirm `tools/list` returns exactly the Phase 1 catalog and nothing else.

**Acceptance criteria:**
- [ ] With `enabled=true`, `tools/list` over `POST /mcp` returns the 9 tools.
- [ ] With `enabled=false`, `/mcp` returns `404` and no MCP beans are registered.

**Related behaviors:** Phase 1 — Tool discovery; master switch.

---

## Step 7: Integration tests

- [ ] `McpAuthIntegrationTest` — 401 without/invalid key, 200 with key, CSRF-free POST, master switch 404.
- [ ] `McpToolsIntegrationTest` — `tools/list` catalog; happy paths for each tool; compound filters; unknown-id error; search grouped + bootstrap error.
- [ ] `McpPaginationIntegrationTest` — default size, clamp, reject `size<=0`, last page, out-of-range page, envelope fields.
- [ ] `McpAuditIntegrationTest` — success + failure audit rows attributed to SYSTEM + key name; no row when unauthenticated.
- [ ] Reuse `AbstractDbTest` (Testcontainers Postgres); seed an API key + sample data; drive `POST /mcp` with JSON-RPC bodies via `MockMvc`.

**Acceptance criteria:**
- [ ] All Phase 1 behavior scenarios have a corresponding test and pass.
- [ ] Backend coverage stays ≥ 80%.

**Related behaviors:** all Phase 1 scenarios.

---

## Step 8: Documentation

- [ ] README: add "MCP Server (Onyx)" section — enabling the master switch, creating an API key, Onyx Shared-Key config sending `X-API-Key`, the tool list, and the **privilege-expansion warning** (enabling MCP grants every existing API key full read access; review/rotate keys first; internal-only until scoped keys exist).
- [ ] Note the Phase-1 GDPR checklist pointer (AVV with the EU LLM provider, etc.) from `design.md`.

**Acceptance criteria:**
- [ ] README documents setup + the privilege-expansion warning.

**Related behaviors:** Phase 1 — production-readiness checks.

---

## Closing (spec-flow)

- [ ] `spec-review` + `quality-review` until clean.
- [ ] Set INDEX status `done`; push `feat/108-mcp-connector`; open PR (closes #40); watch CI.
