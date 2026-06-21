# Design: Remove Double Opt-In Flag from Contacts

## GitHub Issue

_To be created_

## Summary

The `doubleOptIn` boolean field on contacts is unused. It was only populated by the Brevo import (mapping the `DOUBLE_OPT-IN` attribute) and displayed as a read-only checkbox in the contact detail view. It is not used in any business logic, filtering, or export. The field should be removed from the entire stack.

## Goals

- Remove the `doubleOptIn` field from the database, backend, and frontend
- Clean removal with no traces of dead code

## Non-goals

- Removing the `syncedToBrevo` field (different use case, stays)
- Archiving or exporting existing `doubleOptIn` data before deletion (confirmed unnecessary)

## Technical Approach

### 1. Database migration (`V9__remove_double_opt_in.sql`)

```sql
ALTER TABLE contacts DROP COLUMN double_opt_in;
```

**Rationale:** The column has `NOT NULL DEFAULT false`, so dropping it is safe — no data dependencies.

Note: The next migration number depends on whether Spec 021 (Brevo company ID fix) has been implemented. If V8 is already taken, this becomes V9. Adjust accordingly.

### 2. Backend — Entity

**`ContactEntity.java`**: Remove the field declaration, `@Column` annotation, getter (`isDoubleOptIn`), and setter (`setDoubleOptIn`).

### 3. Backend — DTO

**`ContactDto.java`**: Remove the `doubleOptIn` record component and its `@Schema` annotation. Remove the mapping from entity in the static factory method.

### 4. Backend — Service

**`ContactService.java`**: Remove the comment mentioning `doubleOptIn` as a Brevo-managed field.

### 5. Backend — Brevo sync

**`BrevoSyncService.java`**: Remove the two lines that read `DOUBLE_OPT-IN` from Brevo attributes and set it on the entity (lines 231–232).

### 6. Frontend — Types

**`types.ts`**: Remove `readonly doubleOptIn: boolean` from the `ContactDto` interface.

### 7. Frontend — Contact detail view

**`contact-detail.tsx`**: Remove the `<CheckboxField>` that displays `doubleOptIn`.

### 8. Frontend — Translations

**`en.ts`** and **`de.ts`**: Remove the `doubleOptIn` key from `contacts.detail`.

### 9. Tests

Update all test files that reference `doubleOptIn`:

| File | Change |
|------|--------|
| `ContactDtoTest.java` | Remove `setDoubleOptIn` and assertion |
| `ContactControllerTest.java` | Remove from test data if present |
| `BrevoSyncServiceTest.java` | Remove assertions about doubleOptIn mapping |
| `contact-detail.test.tsx` | Remove from test data factory, remove checkbox assertion |
| `contact-form.test.tsx` | Remove from test data factory |
| `contact-list.test.tsx` | Remove from test data factory |

### 10. Specs and documentation

Update existing spec files and architecture docs that reference `doubleOptIn` to reflect the removal. Affected files:

- `specs/core-data-model/` (design.md, behaviors.md, steps.md)
- `specs/dto-refactoring/` (behaviors.md, steps.md)
- `specs/016-brevo-import/` (design.md, behaviors.md, steps.md)
- `specs/007-contact-frontend/` (design.md, behaviors.md, steps.md)
- `specs/018-component-tests/` (behaviors.md)
- `.claude/conventions/project-specific/project-architecture.md`

## Files Affected

| File | Change |
|------|--------|
| `backend/src/main/resources/db/migration/V9__remove_double_opt_in.sql` | **New** — drop column |
| `backend/src/main/java/.../contact/ContactEntity.java` | Remove field, getter, setter |
| `backend/src/main/java/.../contact/ContactDto.java` | Remove record component |
| `backend/src/main/java/.../contact/ContactService.java` | Remove comment |
| `backend/src/main/java/.../brevo/BrevoSyncService.java` | Remove DOUBLE_OPT-IN mapping |
| `frontend/src/lib/types.ts` | Remove from interface |
| `frontend/src/components/contact-detail.tsx` | Remove checkbox |
| `frontend/src/lib/i18n/en.ts` | Remove translation key |
| `frontend/src/lib/i18n/de.ts` | Remove translation key |
| Backend + frontend test files (6 files) | Remove from test data and assertions |
| Spec and doc files (~10 files) | Update references |

## Regression Risk

- **Very low**: Pure removal of an unused read-only field. No business logic depends on it.
- **API breaking change**: The `doubleOptIn` field will disappear from `GET /api/contacts` and `GET /api/contacts/{id}` responses. Since there are no external API consumers, this is acceptable.

## Open Questions

None.
