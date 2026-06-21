# Implementation Steps: Optional Contact Language

## Step 1: Database Migration

- [x] Create `backend/src/main/resources/db/migration/V6__make_language_nullable.sql`
  - `ALTER TABLE contacts ALTER COLUMN language DROP NOT NULL;`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Existing contacts retain their language values

**Related behaviors:** Existing contacts unchanged after migration

---

## Step 2: Backend Entity and DTO Changes

- [x] In `ContactEntity.java`: change `@Column(name = "language", nullable = false, length = 5)` to `nullable = true`
- [x] In `ContactEntity.java`: remove `Objects.requireNonNull` from `setLanguage()`, allow null
- [x] In `ContactCreateDto.java`: remove `@NotNull` annotation from `language`, change schema to `NOT_REQUIRED`
- [x] In `ContactUpdateDto.java`: remove `@NotNull` annotation from `language`, change schema to `NOT_REQUIRED`
- [x] In `ContactDto.java`: change schema from `REQUIRED` to `NOT_REQUIRED` for `language`
- [x] Update Javadoc in DTOs to reflect nullable language

**Acceptance criteria:**
- [x] Project builds successfully
- [ ] Existing backend tests still pass (after adjusting for nullable language)

**Related behaviors:** Create contact via API with null language, Create contact via API without language field, Update contact via API to null language

---

## Step 3: Backend List Filter for NULL Language

- [x] In `ContactController.java`: change `language` param type from `Language` to `String` to support a "UNKNOWN" filter value
- [x] In `ContactService.java`: update list method to accept `String` for language, handle "UNKNOWN" → `IS NULL` filter, parse DE/EN as before

**Acceptance criteria:**
- [x] Project builds successfully
- [ ] Filtering by "UNKNOWN" returns contacts with null language
- [ ] Filtering by "DE"/"EN" still works as before

**Related behaviors:** Filter by unknown language, Filter by specific language excludes unknown, Filter by all languages includes unknown

---

## Step 4: Backend Tests

- [x] Add tests to `ContactControllerTest`:
  - Create contact with null language → 201
  - Create contact without language field → 201
  - Update contact to null language → 200
  - GET contact with null language → language field is null in response
  - Filter by "UNKNOWN" → returns only null-language contacts
  - Filter by "DE" → excludes null-language contacts
  - No filter → includes null-language contacts
- [x] Fix Swagger schema tests in `HealthControllerTest` (language no longer required)

**Acceptance criteria:**
- [x] All new backend tests pass
- [x] All existing backend tests still pass

**Related behaviors:** Create contact via API with null language, Create contact via API without language field, Update contact via API to null language, Existing contacts unchanged after migration, Filter by unknown language, Filter by specific language excludes unknown, Filter by all languages includes unknown

---

## Step 5: Frontend Types and i18n

- [x] In `types.ts`: change `language: "DE" | "EN"` to `language: "DE" | "EN" | null` in `ContactDto`
- [x] In `types.ts`: change `language: "DE" | "EN"` to `language?: "DE" | "EN" | null` in `ContactCreateDto`
- [x] In `de.ts`: add `contacts.form.languageUnknown: "Unbekannt"` and `contacts.filter.unknownLanguage: "Unbekannt"`, remove `languageRequired`
- [x] In `en.ts`: add `contacts.form.languageUnknown: "Unknown"` and `contacts.filter.unknownLanguage: "Unknown"`, remove `languageRequired`

**Acceptance criteria:**
- [ ] Frontend builds successfully (after form updates in Step 6)

**Related behaviors:** (foundation for all frontend scenarios)

---

## Step 6: Frontend Contact Form — Optional Language

- [x] In `contact-form.tsx`: remove required asterisk from language label
- [x] Remove language validation error logic (no longer required)
- [x] Add "Unbekannt"/"Unknown" option to language Select that maps to null
- [x] In edit mode, show "Unbekannt"/"Unknown" when contact.language is null
- [x] In create mode, no pre-selected value

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Language field is optional in create/edit forms

**Related behaviors:** Create contact without language, Form does not pre-select a language, Change language to unknown, Change language from unknown to a value, Edit form shows current unknown language

---

## Step 7: Frontend Contact Detail — Display Unknown Language

- [x] In `contact-detail.tsx`: when `contact.language` is null, display "Unbekannt" (DE) or "Unknown" (EN) instead of raw value

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Null language shown as translated "Unknown"

**Related behaviors:** Display unknown language, Display known language

---

## Step 8: Frontend Contact List Filter — Unknown Option

- [x] In `contact-list.tsx`: add "Unbekannt"/"Unknown" option to language filter dropdown
- [x] Map the "Unknown" filter value to "UNKNOWN" query parameter that the backend interprets as IS NULL

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Unknown language filter option available

**Related behaviors:** Filter by unknown language, Filter by specific language excludes unknown, Filter by all languages includes unknown

---

## Step 9: Frontend Tests — Form, Detail, and List

- [x] Update `contact-form.test.tsx`:
  - Removed language-required validation test
  - "should show Unknown as default language selection in create mode"
  - "should show Unknown as selected when editing contact with null language"
- [x] Update `contact-detail.test.tsx`:
  - "should display Unbekannt when language is null"
- [x] Update `contact-list.test.tsx`:
  - "should show language filter with placeholder text"

**Acceptance criteria:**
- [x] All new frontend tests pass
- [x] All existing frontend tests still pass

**Related behaviors:** Create contact without language, Form does not pre-select a language, Edit form shows current unknown language, Display unknown language, Display known language, Filter by unknown language

---

## Step 10: Update Project Documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — note optional language field
- [x] Update `specs/INDEX.md` — set status to "done"

**Acceptance criteria:**
- [x] Documentation reflects the changes

**Related behaviors:** (none — documentation step)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create contact without language | Both | 4 (BE), 9 (FE) |
| Create contact with language | Backend | 4 (existing behavior) |
| Form does not pre-select a language | Frontend | 6, 9 |
| Change language to unknown | Both | 4 (BE), 9 (FE) |
| Change language from unknown to a value | Both | 4 (BE), 6 (FE) |
| Edit form shows current unknown language | Frontend | 6, 9 |
| Display unknown language | Frontend | 7, 9 |
| Display known language | Frontend | 7, 9 |
| Filter by unknown language | Both | 3, 4 (BE), 8, 9 (FE) |
| Filter by specific language excludes unknown | Both | 3, 4 (BE), 8 (FE) |
| Filter by all languages includes unknown | Both | 3, 4 (BE), 8 (FE) |
| Create contact via API with null language | Backend | 4 |
| Create contact via API without language field | Backend | 4 |
| Update contact via API to null language | Backend | 4 |
| Existing contacts unchanged after migration | Backend | 4 |
