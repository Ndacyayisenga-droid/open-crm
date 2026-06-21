# Design: Replace syncedToBrevo Flag with String brevoId on Contacts

## GitHub Issue

_To be created_

## Summary

The `syncedToBrevo` boolean on contacts is redundant — a non-null `brevoId` already indicates that a contact was synced from Brevo. Additionally, `brevoId` is stored as `BIGINT` but should be `VARCHAR(50)` to match the company ID fix (Spec 021) and prevent future issues if Brevo changes contact IDs from numeric to hex strings.

## Goals

- Remove the redundant `syncedToBrevo` boolean from contacts
- Change `brevoId` from `Long`/`BIGINT` to `String`/`VARCHAR(50)`
- Keep the "Synced to Brevo" display in the frontend, derived from `brevoId != null`

## Non-goals

- Exposing `brevoId` in the REST API (internal field, not needed by frontend)
- Validating existing data consistency before migration (confirmed: blind drop is acceptable)

## Technical Approach

### 1. Database migration

```sql
-- Change brevo_id from BIGINT to VARCHAR(50)
DROP INDEX IF EXISTS idx_contacts_brevo_id;
ALTER TABLE contacts ALTER COLUMN brevo_id TYPE VARCHAR(50);
CREATE UNIQUE INDEX idx_contacts_brevo_id
    ON contacts(brevo_id) WHERE brevo_id IS NOT NULL;

-- Drop the redundant synced_to_brevo column
ALTER TABLE contacts DROP COLUMN synced_to_brevo;
```

Existing numeric `brevo_id` values (e.g., `130`) are automatically cast to strings (`'130'`) by PostgreSQL during the type change.

**Rationale:** `VARCHAR(50)` instead of `CHAR` provides tolerance for future ID format changes, matching the company column from Spec 021.

### 2. Backend — Entity (`ContactEntity.java`)

- Change `private Long brevoId` to `private String brevoId`
- Update `@Column` annotation: add `length = 50`
- Update getter/setter types
- Remove `syncedToBrevo` field, `@Column` annotation, getter, and setter

### 3. Backend — Repository (`ContactRepository.java`)

- Change `findByBrevoId(Long brevoId)` to `findByBrevoId(String brevoId)`

### 4. Backend — DTO (`ContactDto.java`)

- Remove the `syncedToBrevo` record component
- Add `boolean brevo` — a computed field derived from `brevoId != null`
- In the `fromEntity()` factory method: `entity.getBrevoId() != null` maps to the `brevo` field

**Rationale:** The API does not expose `brevoId` (internal Brevo detail). Instead it exposes a simple `brevo: true/false` flag, which is what the frontend needs to display the sync status.

### 5. Backend — Service (`ContactService.java`)

- Remove comment mentioning `syncedToBrevo` as a Brevo-managed field

### 6. Backend — Brevo sync (`BrevoSyncService.java`)

- Change `brevoContact.id()` from `long` to — wait, contact IDs from Brevo are still numeric. The `BrevoContact` record stays `long id`. The conversion happens at assignment: `entity.setBrevoId(String.valueOf(brevoContact.id()))`
- Remove `entity.setSyncedToBrevo(true)` — no longer needed

### 7. Frontend — Types (`types.ts`)

- Remove `readonly syncedToBrevo: boolean`
- Add `readonly brevo: boolean`

### 8. Frontend — Contact detail (`contact-detail.tsx`)

- Change `contact.syncedToBrevo` to `contact.brevo`

### 9. Frontend — Translations (`en.ts`, `de.ts`)

- Rename key from `syncedToBrevo` to `brevo` (keep same display text: "Synced to Brevo" / "Mit Brevo synchronisiert")

### 10. Tests

Update all test files:

| File | Change |
|------|--------|
| `ContactDtoTest.java` | Remove `setSyncedToBrevo`, assert `brevo` field instead |
| `ContactControllerTest.java` | Change `syncedToBrevo` expectations to `brevo` |
| `BrevoSyncServiceTest.java` | Change `Long` brevoId to `String`, remove `syncedToBrevo` assertions, add `brevoId != null` checks |
| `contact-detail.test.tsx` | Change `syncedToBrevo` to `brevo` in test data and assertions |
| `contact-form.test.tsx` | Change `syncedToBrevo` to `brevo` in test data |
| `contact-list.test.tsx` | Change `syncedToBrevo` to `brevo` in test data |

## Files Affected

| File | Change |
|------|--------|
| `backend/src/main/resources/db/migration/V*__contact_brevo_cleanup.sql` | **New** — change column type, drop column |
| `backend/src/main/java/.../contact/ContactEntity.java` | `Long` → `String` for brevoId, remove syncedToBrevo |
| `backend/src/main/java/.../contact/ContactRepository.java` | Parameter type `Long` → `String` |
| `backend/src/main/java/.../contact/ContactDto.java` | Remove `syncedToBrevo`, add computed `brevo` |
| `backend/src/main/java/.../contact/ContactService.java` | Remove comment |
| `backend/src/main/java/.../brevo/BrevoSyncService.java` | `String.valueOf()` for brevoId, remove setSyncedToBrevo |
| `frontend/src/lib/types.ts` | `syncedToBrevo` → `brevo` |
| `frontend/src/components/contact-detail.tsx` | Property name change |
| `frontend/src/lib/i18n/en.ts` | Rename key |
| `frontend/src/lib/i18n/de.ts` | Rename key |
| Backend test files (3) | Update types, assertions |
| Frontend test files (3) | Update test data |

## Regression Risk

- **Low**: Type change (`Long` → `String`) is straightforward. PostgreSQL handles the BIGINT→VARCHAR cast automatically.
- **API breaking change**: `syncedToBrevo` disappears, `brevo` appears. No external consumers, only our frontend.
- **Brevo import**: Contact IDs are still numeric — `String.valueOf(long)` is lossless.

## Open Questions

None.
