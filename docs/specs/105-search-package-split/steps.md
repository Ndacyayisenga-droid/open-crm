# Implementation Steps: Search Package Split

## Step 1: Create the `.lib` reusable building blocks

- [x] `MeilisearchProperties` (move to `.lib`, prefix → `openelements.meilisearch`, drop the four `*Index()` accessors, add `resolveIndex(suffix)`)
- [x] `MeilisearchException` (top-level, lifted from nested)
- [x] `TaskOutcome` (top-level enum, lifted from nested)
- [x] `MeilisearchClient` (move to `.lib`, `@Component`, return/use top-level `TaskOutcome`/`MeilisearchException`)
- [x] `SearchReadinessState` (move to `.lib`, renamed from `SearchIndexState`)
- [x] `ScopedKeySpec` record, `IndexSettings` record
- [x] `SearchIndexBootstrapStep` SPI interface
- [x] `Highlighter` (move `safeHighlight` + `PRE_MARK`/`POST_MARK` from `SearchController`)
- [x] `BatchWriter` helper (stream → batches of 500 → `addDocuments` + `waitForTask`)
- [x] `BootstrapInvoker` (`@Async("searchIndexExecutor")` indirection, top-level)
- [x] `MeilisearchScopedKeyInitializer` `@Order(10)` (parametrized by `Optional<ScopedKeySpec>`)
- [x] `MeilisearchIndexSettingsInitializer` `@Order(20)` (parametrized by `List<IndexSettings>`)
- [x] `MeilisearchBootstrapRunner` `@Order(30)` (drives `List<SearchIndexBootstrapStep>`, per-step isolation)
- [x] `MeilisearchConfiguration` (`@Configuration @ComponentScan(".lib") @EnableConfigurationProperties`)

**Acceptance criteria:**
- [x] No `.lib` file imports `com.openelements.crm.*` (outside `.search.lib`)
- [x] Backend compiles

---

## Step 2: Rework CRM-side glue

- [x] `CrmIndexNames` `@Component` (four index-name accessors via `MeilisearchProperties.resolveIndex`)
- [x] `Companies/Contacts/Tags/CommentsBootstrapStep` `@Order(10/20/30/40)` implementing the SPI (entity→DTO mappers move here)
- [x] `SearchConfiguration` `@Import(MeilisearchConfiguration.class) @EnableAsync`: `searchIndexExecutor`, `crmScopedKey` `ScopedKeySpec`, four `IndexSettings` beans
- [x] `SearchController`: use `Highlighter` + `CrmIndexNames` + `SearchReadinessState`
- [x] `SearchIndexService`: use `CrmIndexNames` instead of `MeilisearchProperties` index methods
- [x] `SearchIndexEventListener`: unchanged logic, package stays
- [x] Delete `SearchIndexBootstrap`, `MeilisearchKeyDeriver`, `SearchIndexSettingsConfigurer`, `SearchIndexState` (replaced)

**Acceptance criteria:**
- [x] Backend compiles

---

## Step 3: Migrate property prefix + existing tests

- [x] `application.yml`: `openelements.search.meilisearch:` → `openelements.meilisearch:`
- [x] `AbstractSearchTest`: property keys + `CrmIndexNames` for index-name lookups
- [x] `MeilisearchClientTest`: `TaskOutcome` import
- [x] `SearchIndexServiceTest`: construct via `CrmIndexNames`
- [x] `SearchIntegrationTest`: `SearchReadinessState` rename
- [x] Rename `SearchControllerSafeHighlightTest` → `HighlighterTest`

**Acceptance criteria:**
- [x] All Spec 104 search tests pass

---

## Step 4: Add unit tests for the new lib behavior

- [x] `MeilisearchBootstrapRunnerTest` (batch 500/partial/empty, per-step isolation, stream close, unreachable)
- [x] `MeilisearchScopedKeyInitializerTest` (minted when bean present, no-op when absent)
- [x] `MeilisearchIndexSettingsInitializerTest` (applied per bean, empty no-op)
- [x] `CrmIndexNamesTest` (default + custom prefix)
- [x] `MeilisearchPropertiesBindingTest` (renamed honored, legacy ignored)

**Acceptance criteria:**
- [x] Full backend test suite passes
