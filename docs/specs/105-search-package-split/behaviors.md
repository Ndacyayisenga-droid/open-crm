# Behaviors: Search Package Split

This refactor is structural — most behavior is unchanged. The scenarios below verify that the split preserves the contract from Spec 104, and pin down the two intentional behavior changes (per-step error isolation, property prefix rename).

## Package boundary

### Lib package has no CRM imports

- **Given** the production source tree at `backend/src/main/java/com/openelements/crm/search/lib/`
- **When** `grep -r "import com.openelements.crm\." backend/src/main/java/com/openelements/crm/search/lib/` is run
- **Then** zero matches are returned, except for imports from `com.openelements.crm.search.lib` itself

### Lib package has no domain-type usage

- **Given** the production source tree at `backend/src/main/java/com/openelements/crm/search/lib/`
- **When** any file under it is inspected
- **Then** no reference to `CompanyDto`, `ContactDto`, `TagDto`, `CommentDto`, `CompanyEntity`, `ContactEntity`, or `TagEntity` exists

### CRM-side `SearchConfiguration` imports the lib config

- **Given** `com.openelements.crm.search.SearchConfiguration`
- **When** its annotations are inspected
- **Then** it carries `@Import(MeilisearchConfiguration.class)` referencing `com.openelements.crm.search.lib.MeilisearchConfiguration`

## Configuration properties

### Renamed prefix is honored

- **Given** a Spring Boot context starting with `openelements.meilisearch.host=http://localhost:7700`
- **When** the context is up
- **Then** `MeilisearchProperties#host()` returns `http://localhost:7700`

### Legacy prefix is no longer honored

- **Given** a Spring Boot context with only `openelements.search.meilisearch.host=http://example:7700` set (no `openelements.meilisearch.host`)
- **When** the context is up
- **Then** `MeilisearchProperties#host()` returns the default `http://localhost:7700`, not the legacy-prefix value

### `application.yml` is migrated

- **Given** `backend/src/main/resources/application.yml`
- **When** the file is inspected
- **Then** it contains an `openelements.meilisearch:` block with `host`, `master-key`, `index-prefix`, `request-timeout` keys, and contains **no** `openelements.search.meilisearch:` block

### Operator env-var contract is unchanged

- **Given** the environment variables `MEILI_HOST`, `MEILI_MASTER_KEY`, `MEILI_INDEX_PREFIX` are set
- **When** the Spring Boot application starts
- **Then** `MeilisearchProperties#host()`, `#masterKey()`, `#indexPrefix()` reflect those values — the env-var → property mapping in `application.yml` continues to work

## `CrmIndexNames`

### Resolves the four CRM index names with the configured prefix

- **Given** `openelements.meilisearch.index-prefix=crm_`
- **When** `CrmIndexNames#companies()`, `#contacts()`, `#tags()`, `#comments()` are called
- **Then** they return `crm_companies`, `crm_contacts`, `crm_tags`, `crm_comments`

### Honors a custom prefix

- **Given** `openelements.meilisearch.index-prefix=test_`
- **When** `CrmIndexNames#companies()` is called
- **Then** it returns `test_companies`

## Bootstrap SPI

### Lib drives the four CRM steps in `@Order` sequence

- **Given** four `SearchIndexBootstrapStep` beans annotated `@Order(10)`, `@Order(20)`, `@Order(30)`, `@Order(40)` for companies, contacts, tags, comments respectively
- **When** `MeilisearchBootstrapRunner` runs
- **Then** the steps are invoked in that order (verifiable via log output or a test spy)

### Lib closes the document stream

- **Given** a `SearchIndexBootstrapStep` whose `documents()` returns a `Stream` that records `close()` calls
- **When** `MeilisearchBootstrapRunner` processes that step
- **Then** the stream's `close()` is invoked exactly once

### Lib batches at 500 documents

- **Given** a `SearchIndexBootstrapStep` that produces 1,500 documents
- **When** `MeilisearchBootstrapRunner` processes that step
- **Then** `MeilisearchClient#addDocuments` is invoked exactly three times, each with 500 documents

### Lib batches partial final batch

- **Given** a `SearchIndexBootstrapStep` that produces 750 documents
- **When** `MeilisearchBootstrapRunner` processes that step
- **Then** `MeilisearchClient#addDocuments` is invoked twice — once with 500, once with 250 documents

### Empty step is a no-op

- **Given** a `SearchIndexBootstrapStep` that produces zero documents
- **When** `MeilisearchBootstrapRunner` processes that step
- **Then** `MeilisearchClient#addDocuments` is not invoked for that step's `indexUid`, and the step counts as succeeded

## Per-step error isolation

### Failure in one step does not stop the others

- **Given** four bootstrap steps where the contacts step's `documents()` throws `RuntimeException` mid-stream
- **When** `MeilisearchBootstrapRunner` runs
- **Then** the companies, tags, and comments steps still run to completion
- **And** an `ERROR`-level log entry identifies the failing step by its `indexUid`
- **And** an `INFO`-level summary at the end reports `3 step(s) succeeded, 1 failed`

### Readiness flips to ready even on partial failure

- **Given** any bootstrap run in which one or more steps throw
- **When** the bootstrap finishes
- **Then** `SearchReadinessState#isBootstrapping()` returns `false`
- **And** `GET /api/search` no longer returns `503`

### All steps failing still flips readiness

- **Given** four bootstrap steps that all throw `RuntimeException`
- **When** the bootstrap finishes
- **Then** `SearchReadinessState#isBootstrapping()` returns `false`
- **And** the summary log reports `0 step(s) succeeded, 4 failed`

## Meilisearch unreachable at startup

### Bootstrap is skipped, readiness marked finished

- **Given** Meilisearch is unreachable at backend startup (`client.isHealthy()` returns `false`)
- **When** `MeilisearchBootstrapRunner` runs
- **Then** no step's `documents()` is invoked
- **And** `SearchReadinessState#isBootstrapping()` returns `false` immediately
- **And** a `WARN`-level log entry indicates that Meilisearch was unreachable

## Settings & ScopedKey

### Scoped key is minted when `ScopedKeySpec` bean is present

- **Given** a `ScopedKeySpec` bean exists with non-empty `indexes` and `actions`
- **And** Meilisearch is reachable
- **When** `MeilisearchScopedKeyInitializer` runs
- **Then** `POST /keys` is called with the bean's contents
- **And** the returned key is set on `MeilisearchClient` via `useApiKey(...)`

### No `ScopedKeySpec` bean means no key derivation

- **Given** no `ScopedKeySpec` bean is registered
- **When** `MeilisearchScopedKeyInitializer` runs
- **Then** `POST /keys` is not called
- **And** `MeilisearchClient` continues to use the master key

### All `IndexSettings` beans are applied at startup

- **Given** four `IndexSettings` beans for the CRM indexes
- **When** `MeilisearchIndexSettingsInitializer` runs against a reachable Meilisearch
- **Then** for each bean, `ensureIndex(uid, primaryKey)` is called once, followed by `updateSettings(uid, ...)` once with searchable, filterable, and sortable attributes

### Empty `IndexSettings` list is a no-op

- **Given** zero `IndexSettings` beans are registered
- **When** `MeilisearchIndexSettingsInitializer` runs
- **Then** no `ensureIndex` and no `updateSettings` calls are made

## Highlighter

### `safeHighlight` is XSS-safe

- **Given** the input string `<script>alert('xss')</script>` containing the boundary marks `foo`
- **When** `Highlighter.safeHighlight(input)` is called
- **Then** the result has HTML special characters escaped (`&lt;`, `&gt;`, `&#39;`)
- **And** the marker characters are replaced with literal `<em>` / `</em>` tags
- **And** the result is safe to render via `dangerouslySetInnerHTML`

### Null input

- **Given** `null` is passed to `Highlighter.safeHighlight`
- **When** the method is called
- **Then** it returns the empty string

## Search endpoint regression (Spec 104 contract)

### Global search still returns grouped results

- **Given** the bootstrap has finished and three contacts, two companies, one tag, and four comments have been indexed
- **When** `GET /api/search?q=acme` is called with credentials
- **Then** the response is `200 OK` with the `GlobalSearchResultDto` envelope and four sections (`companies`, `contacts`, `tags`, `comments`)

### 503 while bootstrapping

- **Given** `SearchReadinessState#isBootstrapping()` returns `true`
- **When** `GET /api/search?q=acme` is called
- **Then** the response is `503` with `Retry-After: 30`

### Query under minimum length

- **Given** the bootstrap has finished
- **When** `GET /api/search?q=a` is called
- **Then** the response is `200 OK` with empty sections and `query: "a"`

### Owner-aware comment hit

- **Given** a comment indexed with `ownerType: "company"`, `ownerId: <uuid>`, `ownerLabel: "Acme"`
- **And** the comment text matches the query `q=hello`
- **When** `GET /api/search?q=hello` is called
- **Then** the response's `comments` section contains a hit with `ownerType: "company"` and the matching `ownerId`

## Tests

### Existing search tests pass without scenario changes

- **Given** the test suite in `backend/src/test/java/com/openelements/crm/search/`
- **When** the suite is run after the refactor
- **Then** all tests pass
- **And** the only modifications applied to test source files are: package/class imports, property-key strings in `AbstractSearchTest.registerMeilisearchProperties`, and bean-injection points (`MeilisearchProperties` → `CrmIndexNames` for index-name lookups)
