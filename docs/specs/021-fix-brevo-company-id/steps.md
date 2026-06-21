# Implementation Steps: Fix Brevo Company ID Parsing

## Step 1: Migration + Entity + Repository + API Client + Sync Service + Tests

- [x] Create `V8__change_brevo_company_id_to_varchar.sql`
- [x] Update `BrevoCompany.java`: `long id` → `String id`
- [x] Update `BrevoApiClient.java`: `asLong()` → `asText()` for company ID
- [x] Update `CompanyEntity.java`: `Long brevoCompanyId` → `String brevoCompanyId`, `@Column(length = 50)`
- [x] Update `CompanyRepository.java`: `findByBrevoCompanyId(Long)` → `findByBrevoCompanyId(String)`
- [x] Update `BrevoSyncService.java`: map types `Map<Long, Long>` → `Map<Long, String>`, add SLF4J logging
- [x] Update all affected tests (BrevoSyncServiceTest, BrevoSyncControllerTest, CompanyRepositoryTest, CompanyDtoTest)
- [x] Update `specs/INDEX.md` status to "done"

**Acceptance criteria:**
- [x] All backend tests pass
- [x] Hex string company IDs are correctly stored and matched
