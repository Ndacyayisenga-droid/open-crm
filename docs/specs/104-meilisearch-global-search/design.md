# Design: Global search via Meilisearch

## Prerequisite

**Spec 103 (tests-postgres-testcontainers) must be merged first.** Meilisearch's bootstrap and sync logic depend on a realistic Postgres-backed test setup; the integration tests in this spec assume `AbstractDbTest` is available.

## Reference

The full architectural rationale (why Meilisearch over pg_trgm, FTS, OpenSearch, etc.; alternatives matrix; trade-offs) lives in `meilisearch.md` at the repository root and is **not** duplicated here. This spec is the implementation contract for v1; `meilisearch.md` is the decision record.

## Summary

Add a global, typo-tolerant search across companies, contacts, tags, and comments. Realized by a separate **Meilisearch** sidecar in `docker-compose.yml`, accessed by the backend via a single `GET /api/search` endpoint that fans out to four Meilisearch indexes and returns results grouped per entity type. The frontend gets a dedicated `/search` route reachable from a new sidebar menu entry placed directly below "Updates".

Postgres remains source of truth. Meilisearch is a derived index, kept in sync by listeners on the existing `GenericDataEvent` (`OnObjectCreate`/`Update`/`Delete`) infrastructure plus a full auto-bootstrap on every backend startup.

This is the **third of three currently planned spec initiatives** (image formats → test migration → Meilisearch).

## Goals

- Typo-tolerant search across Companies, Contacts, Tags, Comments — "hendirk" finds "Hendrik".
- Single `GET /api/search?q=…&limit=…` endpoint returning grouped sections.
- Dedicated `/search` view in the frontend with a sidebar menu entry below "Updates".
- Sync is real-time (event-based) with full bootstrap on backend startup.
- Comments link back to their owner entity (Company / Contact / Task).
- Same deploy unit — Meilisearch ships as a `docker-compose` sidecar, in dev and prod.
- Backend never exposes its Meilisearch API key to the browser.

## Non-goals (v1)

The following are explicit non-goals; each has its own follow-up entry in `TODO.md`:

- **Cmd-K / Ctrl-K keyboard shortcut** — separate spec.
- **Extraction to `spring-services` / `@open-elements/ui` / `@open-elements/nextjs-app-layer`** — separate spec.
- **Synchronous fan-out on tag/company rename** — v1 uses defer-to-reindex (next backend restart repairs).
- **Manual admin reindex endpoint, incremental catch-up, index-version migration** — separate spec.
- **RBAC / per-tag visibility on search results** — every authenticated user sees every hit, same as today's list endpoints.
- **Search-as-you-type as a topbar widget** — v1 ships only the `/search` view.
- **Synonyms with domain vocabulary** — Meilisearch defaults; configurable per index later.
- **Vector / hybrid semantic search** — Onyx is the planned integration for that surface; see `meilisearch.md` § 6.

## Technical approach

### 1. Deployment

Meilisearch becomes a sidecar in `docker-compose.yml`, alongside `db`, `backend`, `frontend`, and `db-backup`. Same topology for dev and prod.

**`docker-compose.yml` additions:**

```yaml
services:
  meilisearch:
    # No healthcheck: the official meilisearch image (v1.6+) ships without
    # wget/curl/nc, so the typical HTTP healthcheck patterns cannot run inside
    # the container. The backend uses `condition: service_started` and retries
    # connections on bootstrap (see § 7), which keeps startup robust without a
    # Docker-level healthcheck.
    image: getmeili/meilisearch:v1.10
    environment:
      MEILI_MASTER_KEY: ${MEILI_MASTER_KEY}
      MEILI_ENV: ${MEILI_ENV:-production}
      MEILI_NO_ANALYTICS: "true"
    volumes:
      - meili-data:/meili_data

  backend:
    environment:
      # existing entries +
      MEILI_HOST: http://meilisearch:7700
      MEILI_MASTER_KEY: ${MEILI_MASTER_KEY}
      MEILI_INDEX_PREFIX: ${MEILI_INDEX_PREFIX:-crm_}
    depends_on:
      db:
        condition: service_healthy
      meilisearch:
        condition: service_started

volumes:
  meili-data:
```

**Why no healthcheck on meilisearch:** the upstream Docker image strips utility binaries (`wget`, `curl`, `nc`) to keep the image small. A Docker `HEALTHCHECK` therefore cannot HTTP-probe `/health` from inside the container. Adding one anyway makes the container permanently unhealthy and breaks any dependent service that uses `condition: service_healthy`. The two robust alternatives — building a custom image with a probe utility, or running a sidecar probe container — are out of proportion for the actual risk (Meilisearch is ready within seconds and the backend handles transient unavailability via bootstrap retries). A re-evaluation TODO is tracked in `TODO.md` for when upstream ships a probe-friendly utility.

**`docker-compose.override.yml` (dev only):**

```yaml
services:
  meilisearch:
    ports:
      - "${MEILI_PORT:-7700}:7700"
    environment:
      MEILI_ENV: development
```

In prod, port 7700 is **not** exposed externally — Meilisearch is reachable only through the compose network.

**Backup:** Meilisearch snapshots are written to `/meili_data/snapshots`. Re-using or extending the existing `db-backup` sidecar to also push the latest snapshot to S3 is in scope for this spec (small change to `scripts/`); a dedicated `meili-backup` service is **not** in scope.

### 2. Configuration

`backend/src/main/resources/application.yml`:

```yaml
openelements:
  search:
    meilisearch:
      host: ${MEILI_HOST:http://localhost:7700}
      master-key: ${MEILI_MASTER_KEY:local-dev-master-key}
      index-prefix: ${MEILI_INDEX_PREFIX:crm_}
      request-timeout: 5s
      sync:
        enabled: true
        retry-attempts: 3
```

### 3. Backend module

New package `com.openelements.crm.search`:

| Class | Responsibility |
| --- | --- |
| `MeilisearchProperties` | `@ConfigurationProperties("openelements.search.meilisearch")`. Host, master key, index prefix, timeout. |
| `MeilisearchClient` | Thin HTTP wrapper on Spring `RestClient`. Endpoints: `GET /health`, `POST /multi-search`, `POST /indexes/{u}/documents`, `DELETE /indexes/{u}/documents/{id}`, `POST /indexes/{u}/settings`, `GET /tasks/{id}`. No external Java SDK — the community one lags. |
| `MeilisearchKeyDeriver` | At startup, exchanges the master key for a scoped API key restricted to `crm_*` indexes. The scoped key is used for all runtime calls; the master key is held only in env. |
| `SearchIndexSettingsConfigurer` | On startup, writes `searchableAttributes`, `filterableAttributes`, `sortableAttributes`, `synonyms`, `stopWords` for each of the four indexes. Idempotent. |
| `SearchIndexBootstrap` | On startup, reads all rows from Postgres and pushes them to Meilisearch in batches of 500. Runs in an `@Async` executor so the backend is HTTP-ready immediately. Sets a `bootstrapping` flag for the duration. |
| `SearchIndexEventListener` | `@EventListener` on `OnObjectCreate<T>`, `OnObjectUpdate<T>`, `OnObjectDelete<T>` for `CompanyDto`, `ContactDto`, `TagDto`, `CommentDto`. Delegates to `SearchIndexService`. `@Async`. |
| `SearchIndexService` | Per entity type: `upsert(dto)`, `delete(id)`. Maps DTO → Meilisearch document. Resolves comment owner (Company / Contact / Task) via the existing join tables. |
| `SearchController` | `GET /api/search`. Returns `503 SERVICE_UNAVAILABLE` with `Retry-After: 30` while `SearchIndexBootstrap` is still running. Otherwise issues one `POST /multi-search`. |
| `GlobalSearchResultDto` | Record: `query`, `companies`, `contacts`, `tags`, `comments`. |
| `SearchHitDto` | Record: `id`, `label`, `snippet`, `highlight`, `score`. Comments additionally carry `ownerType` and `ownerId`. |

### 4. Index design

Four indexes with the prefix `crm_` (configurable).

**`crm_companies`** documents:

```json
{
  "id": "8f3a…",
  "name": "Open Elements GmbH",
  "email": "info@open-elements.com",
  "website": "https://open-elements.com",
  "address": "Bonnstr. 12 53111 Bonn DE",
  "phoneNumber": "+49 …",
  "description": "…",
  "bankName": "…",
  "vatId": "DE…",
  "brevo": true,
  "tagNames": ["partner", "kunde"]
}
```

- `searchableAttributes` (priority order): `name`, `email`, `website`, `address`, `phoneNumber`, `description`, `bankName`, `vatId`, `tagNames`.
- `filterableAttributes`: `brevo`, `tagNames`.

**`crm_contacts`** documents:

```json
{
  "id": "…",
  "title": "Dr.",
  "firstName": "Hendrik",
  "lastName": "Ebbers",
  "email": "…",
  "position": "Founder",
  "phoneNumber": "…",
  "description": "…",
  "socialLinkValues": ["hendrikebbers", "…"],
  "companyId": "…",
  "companyName": "Open Elements GmbH",
  "brevo": true,
  "tagNames": ["…"]
}
```

- `searchableAttributes`: `firstName`, `lastName`, `email`, `position`, `phoneNumber`, `description`, `socialLinkValues`, `companyName`, `tagNames`, `title`.
- `filterableAttributes`: `companyId`, `brevo`, `tagNames`.
- **Denormalization:** `companyName` and `tagNames` are copies. v1 uses **defer-to-reindex** for renames (next backend restart repairs); see `meilisearch.md` § 6.3 and the `TODO.md` follow-up for the synchronous fan-out v2 hardening.

**`crm_tags`** documents:

```json
{ "id": "…", "name": "partner", "description": "…", "color": "#…" }
```

- `searchableAttributes`: `name`, `description`.

**`crm_comments`** documents:

```json
{
  "id": "…",
  "text": "…",
  "ownerType": "contact",
  "ownerId": "…",
  "ownerLabel": "Hendrik Ebbers"
}
```

- `searchableAttributes`: `text`, `ownerLabel`.
- `filterableAttributes`: `ownerType`.

`ownerType` is one of `company` | `contact` | `task`, resolved by the existing join tables (`company_comments`, `contact_comments`, `task_comments`).

### 5. Search request shape

`GET /api/search?q=<query>&limit=<n>`:

- Authenticated; same auth as existing endpoints (Spring Security resource server JWT).
- `q` minimum length 2 characters; below that, returns an empty result without contacting Meilisearch.
- `limit` default 5, max 20 per section.
- Typo thresholds: Meilisearch defaults (0 typos for 1–4 chars, 1 for 5–8, 2 for ≥ 9 chars).
- During bootstrap: `503 SERVICE_UNAVAILABLE` with `Retry-After: 30` header, JSON body `{"error":"search index is initializing"}`.

Backend issues exactly one `POST /multi-search` per request:

```json
{
  "queries": [
    { "indexUid": "crm_companies", "q": "<q>", "limit": <n>, "attributesToHighlight": ["name","email","address"] },
    { "indexUid": "crm_contacts",  "q": "<q>", "limit": <n>, "attributesToHighlight": ["firstName","lastName","email","companyName"] },
    { "indexUid": "crm_tags",      "q": "<q>", "limit": <n>, "attributesToHighlight": ["name","description"] },
    { "indexUid": "crm_comments",  "q": "<q>", "limit": <n>, "attributesToHighlight": ["text","ownerLabel"] }
  ]
}
```

Response is mapped to:

```json
{
  "query": "hendirk",
  "companies": [
    { "id": "…", "label": "Open Elements GmbH",
      "snippet": "…", "highlight": "<em>Open</em> Elements …", "score": 0.42 }
  ],
  "contacts": [
    { "id": "…", "label": "Hendrik Ebbers",
      "snippet": "hendrik.ebbers@…", "highlight": "<em>Hendrik</em> Ebbers", "score": 0.91 }
  ],
  "tags": [],
  "comments": [
    { "id": "…", "label": "…hendrik schickt morgen…",
      "ownerType": "contact", "ownerId": "…", "score": 0.31 }
  ]
}
```

### 6. Sync via events

`SearchIndexEventListener` listens to events from `com.openelements.spring.base.events`:

```java
@EventListener
@Async("searchIndexExecutor")
public void onContactChanged(OnObjectCreate<ContactDto> event) {
    searchIndexService.upsertContact(event.getData());
}
// + symmetric handlers for Update/Delete and for Company, Tag, Comment
```

The async executor is small (e.g. core size 2, max 8) to bound concurrent writes to Meilisearch. Failures are logged with the failing document ID; the next bootstrap repairs.

**Comment events** additionally resolve the owner. Since `CommentEntity` itself doesn't carry an owner reference (the owner lives in the join tables), the listener queries the three join tables to find which Company / Contact / Task currently links to the comment and writes `ownerType` + `ownerId` + `ownerLabel` into the document.

### 7. Bootstrap

`SearchIndexBootstrap` runs as a Spring `ApplicationRunner` on backend startup:

1. Set `bootstrapping = true` (volatile flag on a bean shared with `SearchController`).
2. **Connect-retry loop:** call `GET /health` on Meilisearch until it returns 200, with 1 s backoff between attempts and a 60 s overall budget. Required because there is no Docker healthcheck (see § 1) — Meilisearch is typically ready within ≤ 3 s of container start, but the backend may race ahead.
3. For each of the four entities, stream the rows from Postgres in batches of 500, map to documents, push via `POST /indexes/{u}/documents` (which is upsert by primary key).
4. Wait for the corresponding Meilisearch `tasks` to reach `succeeded` (poll `GET /tasks/{id}` with backoff).
5. Set `bootstrapping = false`.

Steps 1–5 run in an `@Async` background executor so the backend's HTTP listener is up immediately. While `bootstrapping == true`, `/api/search` returns 503. If step 2 exhausts its 60 s budget without reaching Meilisearch, the bootstrap logs `ERROR`, leaves `bootstrapping = true` (search remains 503), and emits a one-shot metric — the backend itself stays up so non-search endpoints continue to work.

This is the **full reindex on every startup** chosen in `meilisearch.md` — guarantees consistency after every deploy and absorbs Meilisearch snapshot loss / version upgrades without ceremony.

### 8. Auth

- `MEILI_MASTER_KEY` set on the Meilisearch container.
- `MeilisearchKeyDeriver` runs once at startup, calls Meilisearch's `POST /keys` to mint a scoped key with read/write rights on `crm_*` indexes only. The scoped key is held in the bean for the JVM lifetime; the master key is never used after this exchange.
- The browser never sees any Meilisearch key. Every search query goes through `GET /api/search`, which requires the standard JWT and reuses the scoped key server-side.

### 9. Frontend

**New sidebar menu entry "Suche" / "Search"** (with search icon), placed directly below the existing "Updates" entry. Existing menu structure is in `frontend/src/components/sidebar.tsx` (or wherever the sidebar component lives in the current code — implementation reads the actual file).

**New route `/search`** in the Next.js frontend. The page:

- Auto-focuses a large search input at the top.
- Debounces input by 300 ms; queries `GET /api/search` once `q.length >= 2`.
- Renders four sections (Companies, Contacts, Tags, Comments) in fixed order. Each section header shows the section name; if the section is non-empty, also the count and an "Alle anzeigen" link to the corresponding list view with the query pre-filled.
- Per hit row: entity-typical visual (company logo / contact avatar / tag color dot / comment icon), label with `<em>` highlight markup from `highlight`, snippet line, click → detail page. Comment hits navigate to the owner's detail page.
- Empty states for: no input (instructive copy), no results, error.
- During 503: a friendly "Suchindex wird initialisiert, einen Moment …" banner with auto-retry every 5 s.
- DE/EN i18n analogous to existing views.

All components live **locally in the open-crm frontend** for v1 (no extraction to `@open-elements/ui` — separate spec).

### 10. Tests

Functional tests run against a real Meilisearch instance via Testcontainers (`org.testcontainers:meilisearch`). Builds on the Postgres-Testcontainers infrastructure from Spec 103.

**Test infrastructure additions:**

A new `AbstractSearchTest extends AbstractDbTest` adds a static `MeilisearchContainer` (the same singleton-static-field pattern), an `@AfterEach` hook that drops the four `crm_*` indexes for test isolation, and a `waitForIndex()` helper that polls until the latest async indexing task succeeds (Meilisearch sync is asynchronous).

| Layer | Strategy |
| --- | --- |
| `MeilisearchClient` | Unit-test against `MockWebServer` — verifies request shape, headers, auth. |
| `SearchIndexService` mapping | Unit-test: DTO → JSON document, no network. |
| `SearchIndexEventListener` | Unit-test with a mocked `SearchIndexService`, verifies that `OnObjectCreate/Update/Delete` are delegated correctly. |
| `SearchIndexBootstrap` | Integration-test extending `AbstractSearchTest`: seed DB → start backend → assert all four indexes are filled. |
| End-to-end search | Integration-test extending `AbstractSearchTest`: seed → bootstrap → real search queries with typos → assert ranking, highlighting, section grouping. |
| Sync behavior | Integration-test: create / update / delete entity → `waitForIndex()` → search reflects the change. |
| Bootstrap 503 | Integration-test: while bootstrap is artificially blocked, `/api/search` returns 503 with `Retry-After`. |

`org.testcontainers:meilisearch` is added to `pom.xml` (test scope).

### 11. Dependencies

- `org.testcontainers:meilisearch` (test scope; version from Testcontainers BOM aligned with the BOM already pulled in by Spec 103).
- No production-Java dependency added — `MeilisearchClient` uses Spring `RestClient` already on the classpath.
- Container images: `getmeili/meilisearch:v1.10` (production), same for tests.
- Frontend: no new dependencies; the page uses existing fetch / SWR / debouncing primitives already in the codebase.

### 12. Implementation order

Two phases as in `meilisearch.md` § 8:

**Phase 1 — Backend**

1. Add Meilisearch sidecar to `docker-compose.yml` and `docker-compose.override.yml`.
2. Add `org.testcontainers:meilisearch` dep.
3. Build the `com.openelements.crm.search` module classes in order: `MeilisearchProperties` → `MeilisearchClient` → `MeilisearchKeyDeriver` → `SearchIndexSettingsConfigurer` → `SearchIndexService` (per entity mapping) → `SearchIndexBootstrap` → `SearchIndexEventListener` → `SearchController`.
4. `AbstractSearchTest` + the seven test layers in § 10.
5. Run full backend suite; iterate.

**Phase 2 — Frontend**

6. New sidebar menu entry under "Updates".
7. New `/search` route page with debounced fetch, sectioned results, highlight rendering, 503 banner, empty/error states.
8. DE/EN i18n.
9. Frontend tests (component-level, mock `/api/search`).
10. Manual end-to-end verification: type a typo, see typo-tolerant results; rename a company, verify the contact-section staleness behavior; restart backend, verify the bootstrap-503 banner appears briefly.

A feature flag is **not** required — the search endpoint and `/search` page are new surfaces and shadow nothing.

### 13. Performance

- Meilisearch search latency: typically < 30 ms per multi-search call against the expected dataset size (well under 100k documents).
- Bootstrap time: a few seconds for the existing dataset (single-digit-thousand rows); the `bootstrapping` flag ensures the user-visible delay is communicated rather than appearing as broken search.
- Index size on disk: roughly 2–3× the raw row data. For CRM-sized datasets, negligible.
- RAM: Meilisearch starts at ~100 MB, grows linearly with data. Compose limits not needed at this scale.

## Security considerations

- Master key is held only in env, exchanged once for a scoped key. Scoped key restricted to `crm_*` indexes.
- The browser never receives any Meilisearch key — all queries go through `/api/search` which enforces the existing JWT auth.
- Meilisearch's port 7700 is not exposed outside the compose network in production.
- The search endpoint inherits the same authorization model as the list endpoints (every authenticated user sees every hit). Tighter RBAC is explicitly out of scope for v1.
- No PII flows to a third party; Meilisearch runs entirely inside the deploy.

## GDPR / personal-data note

Meilisearch indexes contain copies of personal data (contact names, emails, comment text). Same legal basis applies as for the underlying tables; the index is a derived store inside the same deploy boundary. Deletion of a Contact/Company/Comment in Postgres triggers a corresponding `OnObjectDelete` event that removes the document from Meilisearch. Bootstrap on restart re-syncs from Postgres, so any persistence drift is bounded by the deploy frequency.

Comment text often contains free-form notes that may name third parties. The same GDPR posture as the existing comment storage applies — no new exposure surface, just a new query interface.

## Open questions

- **Meilisearch version pinning policy** — `v1.10` is the floor in this spec. Whether minor-version bumps are auto-merged or human-reviewed is a deployment-policy question; not blocking v1.
- **Backup retention for Meilisearch snapshots** — snapshot files end up in S3 via the existing/extended `db-backup` flow; retention policy should mirror the Postgres backup retention (`BACKUP_RETENTION_DAYS`).
- **`spring-services` upstream test base classes** — if Spec 103 changes upstream test infrastructure, this spec's `AbstractSearchTest` may need to align with upstream conventions.
- **Truncate-list maintenance for the `@AfterEach` cleanup** — adding indexes later means extending the test cleanup; documented in `AbstractSearchTest` Javadoc.
