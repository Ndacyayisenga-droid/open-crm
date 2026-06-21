# Implementation Steps: Brevo Import

## Step 1: Database Migration — Brevo ID columns and settings table

- [x] Create `V7__add_brevo_columns_and_settings.sql`:
  - `ALTER TABLE companies ADD COLUMN brevo_company_id BIGINT;`
  - `CREATE UNIQUE INDEX idx_companies_brevo_company_id ON companies(brevo_company_id) WHERE brevo_company_id IS NOT NULL;`
  - `ALTER TABLE contacts ADD COLUMN brevo_id BIGINT;`
  - `CREATE UNIQUE INDEX idx_contacts_brevo_id ON contacts(brevo_id) WHERE brevo_id IS NOT NULL;`
  - `CREATE TABLE settings (key VARCHAR(100) PRIMARY KEY, value TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());`

**Acceptance criteria:**
- [x] Backend compiles successfully

**Related behaviors:** (foundation for all behaviors)

---

## Step 2: Entity Updates — brevoCompanyId, brevoId, Settings

- [x] Add `Long brevoCompanyId` to `CompanyEntity` with `@Column(name = "brevo_company_id")`
- [x] Add `Long brevoId` to `ContactEntity` with `@Column(name = "brevo_id")`
- [x] Create `SettingsEntity` (key, value, createdAt, updatedAt)
- [x] Create `SettingsRepository extends JpaRepository<SettingsEntity, String>`
- [x] Create `SettingsService` with get/set/delete methods
- [x] Add `findByBrevoCompanyId`, `findByNameIgnoreCase` to `CompanyRepository`
- [x] Add `findByBrevoId`, `findByEmailIgnoreCase` to `ContactRepository`

**Acceptance criteria:**
- [x] Backend compiles and Hibernate validation passes
- [x] Existing tests still pass

**Related behaviors:** (foundation for settings and sync)

---

## Step 3: Brevo API Client

- [x] Create `BrevoCompany` record (id, name, domain, linkedContactsIds)
- [x] Create `BrevoContact` record (id, email, attributes)
- [x] Create `BrevoApiClient` component using `RestClient`
  - `fetchAllCompanies()` — paginated GET /companies
  - `fetchAllContacts()` — paginated GET /contacts
  - `validateApiKey(String apiKey)` — GET /account
  - Rate limiting: 100ms between requests
  - Retry on 429/5xx with exponential backoff

**Acceptance criteria:**
- [x] Backend compiles successfully

**Related behaviors:** Brevo API rate limit hit, Brevo API server error

---

## Step 4: Brevo DTOs and Sync Controller

- [x] Create `BrevoSettingsDto` (apiKeyConfigured: boolean)
- [x] Create `BrevoSettingsUpdateDto` (apiKey: String, @NotBlank)
- [x] Create `BrevoSyncResultDto` (companiesImported/Updated/Failed, contactsImported/Updated/Failed, errors)
- [x] Create `BrevoSyncController` with endpoints:
  - GET /api/brevo/settings → BrevoSettingsDto
  - PUT /api/brevo/settings → BrevoSettingsDto (validates key via Brevo API)
  - DELETE /api/brevo/settings → 204
  - POST /api/brevo/sync → BrevoSyncResultDto

**Acceptance criteria:**
- [x] Backend compiles, endpoints accessible via Swagger UI

**Related behaviors:** Save API key, Save invalid API key, Save blank API key, Update existing API key, Remove API key, Check settings configured/unconfigured

---

## Step 5: Brevo Sync Service

- [x] Create `BrevoSyncService` with `syncAll()` method:
  - Concurrency guard (AtomicBoolean)
  - Phase 1: Import companies (match by brevoCompanyId, then name)
  - Build reverse map: contactId → companyId
  - Phase 2: Import contacts (match by brevoId, then email)
  - Company resolution: CRM link > FIRMA_MANUELL > none
  - Per-record transactions via TransactionTemplate
  - Field mapping as specified
- [x] Wire into controller POST /api/brevo/sync

**Acceptance criteria:**
- [x] Backend compiles successfully

**Related behaviors:** All company import, contact import, company-contact association, error handling, and result summary behaviors

---

## Step 6: Backend Tests — Settings and Sync

- [x] Create `SettingsServiceTest` — get/set/delete, upsert
- [x] Create `BrevoSyncControllerTest` — settings CRUD endpoints, sync endpoint (400 without key, 409 concurrent)
- [x] Create `BrevoSyncServiceTest` — test sync logic with mocked BrevoApiClient:
  - Company import (new, update by ID, match by name, case-insensitive)
  - Contact import (new, update by ID, match by email, case-insensitive)
  - Field mapping (SPRACHE 1/2/3/null, DOUBLE_OPT-IN, syncedToBrevo)
  - Company-contact association (CRM link, FIRMA_MANUELL, priority, empty, none)
  - Error handling (missing VORNAME+NACHNAME, partial failure)
  - Result counts (imported, updated, failed)
  - CRM-only records untouched

**Acceptance criteria:**
- [x] All new backend tests pass
- [x] All existing backend tests still pass

**Related behaviors:** All backend behavior scenarios

---

## Step 7: Frontend Types, API, and i18n

- [x] Add `BrevoSettingsDto` and `BrevoSyncResultDto` to `types.ts`
- [x] Add API functions: getBrevoSettings, updateBrevoSettings, deleteBrevoSettings, startBrevoSync
- [x] Add i18n keys for nav entry and all Brevo sync UI strings (DE and EN)

**Acceptance criteria:**
- [x] Frontend builds successfully

**Related behaviors:** (foundation for frontend behaviors)

---

## Step 8: Frontend — Brevo Sync Page and Sidebar

- [x] Create `/brevo-sync` page with Settings Card and Sync Card
- [x] Add sidebar navigation entry with RefreshCw icon
- [x] Settings Card: configured/unconfigured states, save/change/remove
- [x] Sync Card: Start Import button, loading spinner, result summary with error list

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Page accessible and functional

**Related behaviors:** Settings card states, Sync button disabled, Sync in progress, Sync result displayed

---

## Step 9: Frontend Tests

- [x] Test settings card configured/unconfigured states
- [x] Test sync button disabled without API key
- [x] Test sync loading state
- [x] Test result display with counts and errors

**Acceptance criteria:**
- [x] All frontend tests pass

**Related behaviors:** All frontend UI behaviors

---

## Step 10: Update Project Documentation

- [x] Update project-features.md — add Brevo import feature
- [x] Update project-tech.md — add RestClient, Brevo API
- [x] Update project-structure.md — add brevo and settings packages
- [x] Update project-architecture.md — add Brevo integration
- [x] Update README.md — add Brevo import to features
- [x] Update specs/INDEX.md — set status to "done"

**Acceptance criteria:**
- [x] All documentation up to date
