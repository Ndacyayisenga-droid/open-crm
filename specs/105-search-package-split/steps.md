# Implementation Steps: Search Package Split

## Step 1: Create the `.lib` reusable building blocks

- [ ] `MeilisearchProperties` (move to `.lib`, prefix → `openelements.meilisearch`, drop the four `*Index()` accessors, add `resolveIndex(suffix)`)
- [ ] `MeilisearchException` (top-level, lifted from nested)
- [ ] `TaskOutcome` (top-level enum, lifted from nested)
- [ ] `MeilisearchClient` (move to `.lib`, `@Component`, return/use top-level `TaskOutcome`/`MeilisearchException`)
- [ ] `SearchReadinessState` (move to `.lib`, renamed from `SearchIndexState`)
- [ ] `ScopedKeySpec` record, `IndexSettings` record
- [ ] `SearchIndexBootstrapStep` SPI interface
- [ ] `Highlighter` (move `safeHighlight` + `PRE_MARK`/`POST_MARK` from `SearchController`)
- [ ] `BatchWriter` helper (stream → batches of 500 → `addDocuments` + `waitForTask`)
- [ ] `BootstrapInvoker` (`@Async("searchIndexExecutor")` indirection, top-level)
- [ ] `MeilisearchScopedKeyInitializer` `@Order(10)` (parametrized by `Optional<ScopedKeySpec>`)
- [ ] `MeilisearchIndexSettingsInitializer` `@Order(20)` (parametrized by `List<IndexSettings>`)
- [ ] `MeilisearchBootstrapRunner` `@Order(30)` (drives `List<SearchIndexBootstrapStep>`, per-step isolation)
- [ ] `MeilisearchConfiguration` (`@Configuration @ComponentScan(".lib") @EnableConfigurationProperties`)

**Acceptance criteria:**
- [ ] No `.lib` file imports `com.openelements.crm.*` (outside `.search.lib`)
- [ ] Backend compiles

---

## Step 2: Rework CRM-side glue

- [ ] `CrmIndexNames` `@Component` (four index-name accessors via `MeilisearchProperties.resolveIndex`)
- [ ] `Companies/Contacts/Tags/CommentsBootstrapStep` `@Order(10/20/30/40)` implementing the SPI (entity→DTO mappers move here)
- [ ] `SearchConfiguration` `@Import(MeilisearchConfiguration.class) @EnableAsync`: `searchIndexExecutor`, `crmScopedKey` `ScopedKeySpec`, four `IndexSettings` beans
- [ ] `SearchController`: use `Highlighter` + `CrmIndexNames` + `SearchReadinessState`
- [ ] `SearchIndexService`: use `CrmIndexNames` instead of `MeilisearchProperties` index methods
- [ ] `SearchIndexEventListener`: unchanged logic, package stays
- [ ] Delete `SearchIndexBootstrap`, `MeilisearchKeyDeriver`, `SearchIndexSettingsConfigurer`, `SearchIndexState` (replaced)

**Acceptance criteria:**
- [ ] Backend compiles

---

## Step 3: Migrate property prefix + existing tests

- [ ] `application.yml`: `openelements.search.meilisearch:` → `openelements.meilisearch:`
- [ ] `AbstractSearchTest`: property keys + `CrmIndexNames` for index-name lookups
- [ ] `MeilisearchClientTest`: `TaskOutcome` import
- [ ] `SearchIndexServiceTest`: construct via `CrmIndexNames`
- [ ] `SearchIntegrationTest`: `SearchReadinessState` rename
- [ ] Rename `SearchControllerSafeHighlightTest` → `HighlighterTest`

**Acceptance criteria:**
- [ ] All Spec 104 search tests pass

---

## Step 4: Add unit tests for the new lib behavior

- [ ] `MeilisearchBootstrapRunnerTest` (batch 500/partial/empty, per-step isolation, stream close, unreachable)
- [ ] `MeilisearchScopedKeyInitializerTest` (minted when bean present, no-op when absent)
- [ ] `MeilisearchIndexSettingsInitializerTest` (applied per bean, empty no-op)
- [ ] `CrmIndexNamesTest` (default + custom prefix)
- [ ] `MeilisearchPropertiesBindingTest` (renamed honored, legacy ignored)

**Acceptance criteria:**
- [ ] Full backend test suite passes
