# Behaviors: Global search via Meilisearch

## Backend — search endpoint

### Authenticated search returns grouped results

- **Given** an authenticated user (JWT)
- **And** Meilisearch is reachable and the bootstrap has finished
- **And** the database contains at least one Company, one Contact, one Tag, and one Comment whose text contains the substring `"hendrik"`
- **When** the client issues `GET /api/search?q=hendrik`
- **Then** the response status is `200 OK`
- **And** the response body has the shape `{ "query", "companies", "contacts", "tags", "comments" }`
- **And** every section contains an array (possibly empty) of hit objects with at least `id`, `label`, `score`

### Typo tolerance — "hendirk" finds "Hendrik"

- **Given** a Contact with `firstName = "Hendrik"`, `lastName = "Ebbers"`
- **When** the client issues `GET /api/search?q=hendirk`
- **Then** the `contacts` section contains a hit with `id` equal to that Contact
- **And** the hit's `score` is > 0

### Highlighting wraps matched fragments in `<em>`

- **Given** a Company with `name = "Open Elements GmbH"`
- **When** the client issues `GET /api/search?q=open`
- **Then** the `companies` section contains a hit whose `highlight` field includes `<em>Open</em>`

### Comment hit carries owner reference

- **Given** a Comment attached to a specific Contact, with text containing `"meeting"`
- **When** the client issues `GET /api/search?q=meeting`
- **Then** the `comments` section contains a hit
- **And** the hit's `ownerType` is `"contact"`
- **And** the hit's `ownerId` is the ID of the owning Contact
- **And** the hit's `label` derived from `ownerLabel` matches the owning contact's display name

### Minimum query length

- **Given** any database state
- **When** the client issues `GET /api/search?q=a` (length 1)
- **Then** the response status is `200 OK`
- **And** all four sections are empty arrays
- **And** no `multi-search` call was made to Meilisearch (verifiable via mock in unit tests)

### Empty query

- **Given** any database state
- **When** the client issues `GET /api/search?q=` or omits the parameter
- **Then** the response status is `200 OK`
- **And** all four sections are empty arrays

### Default and bounded limit

- **Given** more than 5 matching Contacts
- **When** the client issues `GET /api/search?q=…` without `limit`
- **Then** the `contacts` section contains exactly 5 hits

- **Given** more than 20 matching Contacts
- **When** the client issues `GET /api/search?q=…&limit=50`
- **Then** the `contacts` section contains at most 20 hits (server caps at 20)

### Unauthenticated request is rejected

- **Given** no `Authorization` header
- **When** the client issues `GET /api/search?q=hendrik`
- **Then** the response status is `401 UNAUTHORIZED`

## Backend — bootstrap behavior

### `/api/search` returns 503 while bootstrap is running

- **Given** the backend has just started and `SearchIndexBootstrap` has not yet finished
- **When** the client issues `GET /api/search?q=hendrik`
- **Then** the response status is `503 SERVICE_UNAVAILABLE`
- **And** the response includes header `Retry-After: 30`
- **And** the response body contains a machine-readable hint, e.g. `{"error":"search index is initializing"}`
- **And** no `multi-search` call was made to Meilisearch

### Bootstrap fills all four indexes

- **Given** a Postgres database containing N companies, M contacts, T tags, and C comments
- **When** the backend starts and `SearchIndexBootstrap` completes
- **Then** `crm_companies` contains exactly N documents
- **And** `crm_contacts` contains exactly M documents
- **And** `crm_tags` contains exactly T documents
- **And** `crm_comments` contains exactly C documents (each with a resolved `ownerType` and `ownerId`)

### Bootstrap runs async — HTTP listener is up immediately

- **Given** the backend is starting
- **When** the HTTP listener becomes ready (Spring's `WebServerInitializedEvent` fires)
- **Then** the bootstrap may still be running in the background
- **And** non-search endpoints respond normally (e.g. `GET /api/companies` returns 200)
- **And** only `/api/search` returns 503 until bootstrap finishes

### Bootstrap runs on every restart

- **Given** the backend has previously completed a bootstrap and Meilisearch already contains documents
- **When** the backend is restarted
- **Then** `SearchIndexBootstrap` runs again and re-pushes all documents
- **And** the resulting indexes are byte-equivalent to a from-scratch bootstrap

## Backend — sync via events

### Creating a Contact appears in search

- **Given** the bootstrap is finished and `crm_contacts` does not contain a contact with `email = "new@example.com"`
- **When** the application creates a new Contact with `firstName = "New"`, `lastName = "Person"`, `email = "new@example.com"`
- **And** the test waits for the indexer to settle (`waitForIndex()` returns)
- **Then** `GET /api/search?q=new@example.com` returns a `contacts` hit with the new contact's id

### Updating a Contact updates the search hit

- **Given** an indexed Contact with `firstName = "Old"`
- **When** the application updates the Contact's `firstName` to `"Different"`
- **And** the test waits for the indexer
- **Then** `GET /api/search?q=different` returns the contact
- **And** `GET /api/search?q=old` does not return that contact (assuming nothing else matches "old")

### Deleting a Contact removes the search hit

- **Given** an indexed Contact
- **When** the application deletes the Contact
- **And** the test waits for the indexer
- **Then** `GET /api/search?q=…` no longer returns that contact in any section

### Creating a Comment indexes it with resolved owner

- **Given** an existing Contact and the bootstrap has finished
- **When** a Comment is added to that Contact with text `"renewal in March"`
- **And** the test waits for the indexer
- **Then** `GET /api/search?q=renewal` returns a `comments` hit
- **And** the hit's `ownerType` is `"contact"` and `ownerId` is the Contact's id

### Deleting a Company removes its document

- **Given** an indexed Company
- **When** the application deletes the Company
- **Then** after the indexer settles, the company no longer appears in search

## Backend — defer-to-reindex on rename

### Renaming a Company does NOT immediately update the denormalized companyName on contacts (v1)

- **Given** a Company `C` with `name = "Foo GmbH"` and two associated Contacts indexed with `companyName = "Foo GmbH"`
- **When** the Company is renamed to `"Foo AG"`
- **And** the test waits for the indexer
- **Then** `crm_companies` shows the new name for `C`
- **And** `crm_contacts` still shows the **old** name `"Foo GmbH"` for the two contacts (this is the documented v1 staleness)
- **And** `GET /api/search?q=AG` returns the company but not the contacts via the companyName field

### Backend restart repairs the staleness

- **Given** the staleness scenario above
- **When** the backend is restarted and the bootstrap completes
- **Then** the two contact documents now show `companyName = "Foo AG"`
- **And** `GET /api/search?q=AG` returns both the company and the two contacts

### Same defer-to-reindex behavior for Tag renames

- **Given** a Tag `T` with `name = "partner"`, denormalized into 3 contacts' `tagNames`
- **When** the Tag is renamed to `"strategic-partner"`
- **And** the test waits for the indexer
- **Then** `crm_tags` shows the new name
- **And** `crm_contacts` still shows the old name in `tagNames` until the next backend restart

## Backend — auth & key handling

### Master key is exchanged for a scoped key at startup

- **Given** the backend boots with `MEILI_MASTER_KEY` set
- **When** `MeilisearchKeyDeriver` runs
- **Then** it issues exactly one `POST /keys` to Meilisearch with `indexes: ["crm_*"]` and `actions` limited to read + document write + settings
- **And** the returned scoped key is used for all subsequent calls (verifiable in MockWebServer-based unit tests by inspecting the `Authorization` header)

### Browser never sees a Meilisearch key

- **Given** a fully running backend and frontend
- **When** the user performs a search via the `/search` page
- **Then** the browser DevTools network panel shows only requests to the backend's `/api/search`
- **And** no request goes to Meilisearch directly
- **And** the `Authorization` header on `/api/search` uses the user's JWT, not a Meilisearch key

## Frontend — sidebar menu

### "Suche" / "Search" entry appears directly below "Updates"

- **Given** the user is logged in
- **When** the sidebar renders
- **Then** an entry labelled "Suche" (DE) / "Search" (EN) with a search icon appears directly below the "Updates" entry
- **And** clicking it navigates to `/search`

## Frontend — `/search` page

### Search input is auto-focused on page load

- **Given** the user navigates to `/search`
- **When** the page mounts
- **Then** the search input has keyboard focus

### Typing under 2 chars does not trigger a request

- **Given** the page is open and idle
- **When** the user types a single character
- **Then** no request to `/api/search` is made
- **And** no empty-state changes (the "type at least 2 characters" hint remains visible)

### Debounced 300 ms request

- **Given** the page is open
- **When** the user types `"hendrik"` over the course of 200 ms
- **Then** exactly one `GET /api/search?q=hendrik` request is fired ~300 ms after the last keystroke

### Each section renders header, hits, "Alle anzeigen"

- **Given** the API returned at least one hit in the `contacts` section
- **When** the page renders the response
- **Then** there is a "Contacts" section header
- **And** the count of hits is shown
- **And** each hit row shows the contact's avatar (or fallback), the highlighted label, and a snippet
- **And** an "Alle anzeigen" / "Show all" link navigates to `/contacts?q=hendrik`

### Comment hit click navigates to the owner

- **Given** a `comments` section hit with `ownerType = "contact"`, `ownerId = "abc-123"`
- **When** the user clicks the hit row
- **Then** the browser navigates to `/contacts/abc-123`

### 503 surface during bootstrap

- **Given** the backend returns 503 for the search request
- **When** the page receives that response
- **Then** the page shows a friendly banner ("Suchindex wird initialisiert, einen Moment …" / "Search index is initializing, one moment …")
- **And** the page auto-retries the same query after the `Retry-After` interval (or every 5 s if absent)
- **And** once the backend returns 200, the banner disappears and results render

### Empty result state

- **Given** the query returns zero hits in every section
- **When** the page renders
- **Then** an explicit "Keine Treffer" / "No results" message is shown
- **And** the section headers are not rendered (no blank "Contacts (0)" headers)

### Error state

- **Given** the search request returns a non-503 5xx
- **When** the page receives the response
- **Then** an inline error message is shown
- **And** the page does not auto-retry indefinitely

## Frontend — i18n

### German labels and copy

- **Given** the German UI is active
- **Then** the sidebar entry reads "Suche"
- **And** the section headers read "Firmen", "Kontakte", "Tags", "Kommentare"
- **And** the empty-state copy and 503 banner are in German

### English labels and copy

- **Given** the English UI is active
- **Then** the sidebar entry reads "Search"
- **And** the section headers read "Companies", "Contacts", "Tags", "Comments"
- **And** all transient copy is in English

## Deployment

### Production: port 7700 is not exposed

- **Given** the production `docker-compose.yml` is in use (no override applied)
- **When** the stack starts
- **Then** the Meilisearch container has no port mapping
- **And** the host cannot reach `localhost:7700`
- **And** only the backend container (via the compose network) can reach `meilisearch:7700`

### Dev: port 7700 is exposed for tooling

- **Given** the dev environment with `docker-compose.override.yml` applied
- **When** the stack starts
- **Then** Meilisearch is reachable at `http://localhost:7700`
- **And** the web dashboard responds (because `MEILI_ENV=development`)

### Backend waits for `db` health and Meilisearch start

- **Given** a fresh stack start
- **When** `docker compose up` runs
- **Then** the backend container does not start until the `db` healthcheck passes
- **And** the backend container starts as soon as the `meilisearch` container has *started* (no Docker-level healthcheck on meilisearch — the upstream image lacks the probe utilities; the backend's bootstrap retries the connection internally instead)

### Backend bootstrap retries the Meilisearch connection

- **Given** Meilisearch is slow to become reachable after container start
- **When** `SearchIndexBootstrap` runs
- **Then** it polls `GET /health` on Meilisearch with 1 s backoff up to a 60 s budget
- **And** as soon as `/health` returns 200, the bootstrap proceeds to push documents
- **And** while the connect-retry loop runs, `/api/search` returns 503

### Backend stays up if Meilisearch never comes online

- **Given** Meilisearch is unreachable for longer than the 60 s connect-retry budget
- **When** `SearchIndexBootstrap` exhausts the budget
- **Then** the bootstrap logs `ERROR` with the connection failure
- **And** `bootstrapping` stays `true`, so `/api/search` continues to return 503
- **And** all other backend endpoints (CRUD, auth, etc.) remain responsive

## Test infrastructure

### `AbstractSearchTest` provides a shared Meilisearch container

- **Given** any integration test extending `AbstractSearchTest`
- **When** the test class boots
- **Then** the same singleton `MeilisearchContainer` instance is used as for other test classes in the same run
- **And** the four `crm_*` indexes are dropped between test methods (`@AfterEach`)

### `waitForIndex()` blocks until the latest async task succeeds

- **Given** a test that triggered a write to Meilisearch
- **When** the test calls `waitForIndex()`
- **Then** the call returns within a bounded timeout once the latest `tasks/{id}` reports `succeeded`
- **And** subsequent search calls see the write
