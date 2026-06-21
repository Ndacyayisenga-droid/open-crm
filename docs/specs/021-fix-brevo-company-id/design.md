# Design: Fix Brevo Company ID Parsing

## GitHub Issue

_To be created_

## Summary

The Brevo import fails to create companies correctly. All 36 Brevo companies are collapsed into a single database entity because the Brevo company ID is a hex string (e.g., `699309fa6f8c46a643a7b922`) but the code parses it as `long` via `JsonNode.asLong()`, which silently returns `0` for non-numeric strings. Every company gets `brevoCompanyId = 0`, so `findByBrevoCompanyId(0)` matches the first-created company for all subsequent ones — overwriting its name and domain each time.

The user-visible impact: the import dialog reports "36 companies imported/updated" but only 1 company actually exists in the database. All contacts that should be distributed across 36 companies are either assigned to that single company or have no company at all.

## Root Cause Analysis

### The parsing bug

`BrevoApiClient.fetchAllCompanies()` (line 86):

```java
final long id = item.get("id").asLong();
```

The Brevo `/v3/companies` API returns company IDs as **hex strings** (e.g., `"699309fa6f8c46a643a7b922"`), not as numbers. `JsonNode.asLong()` cannot parse hex strings and returns the default value `0`.

**Note:** Brevo contact IDs (`/v3/contacts`) are numeric integers (e.g., `130`). This inconsistency in the Brevo API is why the contact import works correctly but the company import does not.

### The cascade

1. All 36 `BrevoCompany` objects get `id = 0`
2. First company: `findByBrevoCompanyId(0)` → no match → **created** with `brevoCompanyId = 0`
3. Companies 2–36: `findByBrevoCompanyId(0)` → **matches the first company** → overwrites name, domain, and counts as "updated"
4. The `contactToCompanyBrevoId` map stores `0` for all contact→company links
5. During contact sync, `findByBrevoCompanyId(0)` resolves all contacts to the same single company
6. Contacts linked to Brevo companies whose data was overwritten lose their correct association

### Why the counters lie

The import correctly counts each `syncCompany()` call as imported or updated — the operations "succeed" within their transactions. But since all operations target the same database row, the net result is 1 company with the last Brevo company's data.

## Fix Approach

### 1. Change `brevo_company_id` from `BIGINT` to `VARCHAR`

The Brevo company ID is a 24-character hex string. The database column, entity field, repository queries, and sync logic must all use `String` instead of `Long`.

**Migration** (`V8__change_brevo_company_id_to_varchar.sql`):

```sql
-- Drop the existing unique index
DROP INDEX IF EXISTS idx_companies_brevo_company_id;

-- Change column type from BIGINT to VARCHAR(50)
ALTER TABLE companies ALTER COLUMN brevo_company_id TYPE VARCHAR(50);

-- Recreate unique index
CREATE UNIQUE INDEX idx_companies_brevo_company_id
    ON companies(brevo_company_id) WHERE brevo_company_id IS NOT NULL;

-- Clean up bad data: reset the single company with brevoCompanyId='0'
UPDATE companies SET brevo_company_id = NULL WHERE brevo_company_id = '0';
```

**Rationale:** Using `VARCHAR(50)` instead of `CHAR(24)` provides tolerance for potential future ID format changes in the Brevo API.

### 2. Update `BrevoCompany` record

Change from `long id` to `String id`.

### 3. Update `BrevoApiClient.fetchAllCompanies()`

Change `item.get("id").asLong()` to `item.get("id").asText()`.

### 4. Update `CompanyEntity`

Change `brevoCompanyId` field from `Long` to `String`. Update the column annotation length.

### 5. Update `CompanyRepository`

Change `findByBrevoCompanyId(Long)` to `findByBrevoCompanyId(String)`.

### 6. Update `BrevoSyncService`

- `contactToCompanyBrevoId` map: change from `Map<Long, Long>` to `Map<Long, String>` (contact ID stays `long`, company ID becomes `String`)
- `syncCompany()`: no logic change needed, just type propagation
- `resolveCompany()`: parameter type changes from `Long` to `String`

### 7. Add logging

Add `LOG.info`/`LOG.debug` statements to make future debugging easier:

- Log each company fetched from Brevo (ID, name) at DEBUG level
- Log each company sync result (created/updated, DB ID, Brevo ID) at INFO level
- Log each contact company resolution result (which company was matched, or null) at DEBUG level
- Log summary counts at INFO level (already exists)

**Rationale:** The silent failure was hard to diagnose because there was no per-entity logging. Even after the ID fix, logging is essential for verifying correct behavior and diagnosing future issues.

### 8. Update tests

Update all test code that uses `Long` for Brevo company IDs to use `String`.

## Files Affected

| File | Change |
|------|--------|
| `backend/src/main/resources/db/migration/V8__change_brevo_company_id_to_varchar.sql` | **New** — migrate column type, clean bad data |
| `backend/src/main/java/.../brevo/BrevoCompany.java` | `long id` → `String id` |
| `backend/src/main/java/.../brevo/BrevoApiClient.java` | `asLong()` → `asText()` |
| `backend/src/main/java/.../brevo/BrevoSyncService.java` | Map types, logging |
| `backend/src/main/java/.../company/CompanyEntity.java` | `Long brevoCompanyId` → `String brevoCompanyId` |
| `backend/src/main/java/.../company/CompanyRepository.java` | Query parameter type |
| `backend/src/test/java/.../brevo/BrevoSyncServiceTest.java` | Update ID types and assertions |
| `backend/src/test/java/.../brevo/BrevoSyncControllerTest.java` | Update if affected |
| `backend/src/test/java/.../company/CompanyRepositoryTest.java` | Update if affected |
| `backend/src/test/java/.../company/CompanyServiceTest.java` | Update if affected |

## Regression Risk

- **Low**: The change is type-only (`Long` → `String`) with a straightforward DB migration. No new behavior is introduced.
- **Contact import**: Unaffected — contact IDs remain numeric `long`.
- **Existing data**: The migration resets the single bad `brevoCompanyId = '0'` entry to `NULL`. A re-import will correctly assign all 36 companies.
- **Re-import required**: After deploying the fix, the user must run the Brevo sync again to import the 36 companies correctly.

## Open Questions

None.
