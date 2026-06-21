# Implementation Steps: Company Financial Fields

> **Note:** Spec 080 (External Links Table) is blocked. Readonly protection for externally-managed fields is excluded from this implementation. It can be added when Spec 080 is unblocked.

---

## Step 1: Database Migration

- [x] Create `V23__add_company_finance_fields.sql` with four new nullable columns: `bank_name VARCHAR(255)`, `bic VARCHAR(11)`, `iban VARCHAR(34)`, `vat_id VARCHAR(20)`

**Acceptance criteria:**
- [x] Backend starts without errors
- [x] Migration applies successfully

**Related behaviors:** None directly (infrastructure step)

---

## Step 2: Entity and DTO Fields

- [x] Add `bankName`, `bic`, `iban`, `vatId` fields to `CompanyEntity.java` with `@Column` annotations and correct max lengths
- [x] Add `bankName`, `bic`, `iban`, `vatId` to `CompanyDto.java` record with `@Schema` annotations
- [x] Update `CompanyDto.fromEntity()` to map the four new fields
- [x] Add `bankName`, `bic`, `iban`, `vatId` to `CompanyCreateDto.java` with `@Size` constraints
- [x] Add `bankName`, `bic`, `iban`, `vatId` to `CompanyUpdateDto.java` with `@Size` constraints

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] Existing tests still pass

**Related behaviors:** GET company returns financial fields, POST company with financial fields

---

## Step 3: Service Validation and Field Mapping

- [x] Add IBAN validation in `CompanyService`: strip whitespace, 15–34 chars, first 2 uppercase letters, rest alphanumeric (only when non-null/non-blank; blank → null)
- [x] Add BIC validation in `CompanyService`: exactly 8 or 11 chars, all uppercase alphanumeric (only when non-null/non-blank; blank ��� null)
- [x] Map the four fields in `create()` and `update()` methods
- [x] Trim `vatId` on save; store blank as null

**Acceptance criteria:**
- [x] Project compiles and existing tests pass
- [x] Valid IBAN/BIC values are accepted, invalid ones throw 400

**Related behaviors:** All IBAN Validation, all BIC Validation, VAT ID stored as-is, VAT ID with any format accepted, Empty IBAN/BIC accepted

---

## Step 4: CSV Export Columns

- [x] Add `BANK_NAME`, `BIC`, `IBAN`, `VAT_ID` enum values to `CompanyExportColumn.java`

**Acceptance criteria:**
- [x] Project compiles successfully

**Related behaviors:** Financial fields included in export, Empty financial fields exported as empty

---

## Step 5: Frontend Types and i18n

- [x] Add `bankName`, `bic`, `iban`, `vatId` to `CompanyDto` interface in `types.ts`
- [x] Add same fields as optional to `CompanyCreateDto` interface
- [x] Add finance section translation keys to `en.ts` and `de.ts` (title, field labels, placeholders)
- [x] Add CSV export column labels for the four fields to both language files

**Acceptance criteria:**
- [x] Frontend compiles without type errors

**Related behaviors:** None directly (infrastructure step)

---

## Step 6: Frontend Detail View — Finanzen Section

- [x] Add "Finanzen" section to `company-detail.tsx` below existing fields
- [x] Show section only when at least one financial field is non-null
- [x] Display each non-null field with copy action using existing `DetailField` pattern
- [x] Use i18n keys for section title and field labels

**Acceptance criteria:**
- [x] Section visible when financial fields are present
- [x] Section hidden when all four fields are null
- [x] Copy action works on each field

**Related behaviors:** Finanzen section displays when fields present, Finanzen section hidden when all fields null, Copy action on financial fields, Create company with partial financial fields (detail view part)

---

## Step 7: Frontend Form — Finanzen Section

- [x] Add "Finanzen" section to `company-form.tsx` with four text input fields
- [x] Use correct placeholders from i18n
- [x] All fields optional
- [x] Map fields to/from API calls (create and update)

**Acceptance criteria:**
- [x] Form shows financial fields section
- [x] Create and edit both send financial fields to the API
- [x] Fields are pre-populated in edit mode

**Related behaviors:** Create company with all financial fields, Create company without financial fields, Update financial fields, Clear financial fields

---

## Step 8: Backend Tests — DTO Conversion

- [x] Add tests in `CompanyDtoTest.java` for `fromEntity()` mapping of all four financial fields
- [x] Test that null entity fields map to null DTO fields
- [x] Test that populated entity fields map correctly

**Acceptance criteria:**
- [x] All DTO conversion tests pass

**Related behaviors:** GET company returns financial fields

---

## Step 9: Backend Tests — Service Validation

- [x] Add tests in `CompanyServiceTest.java` for IBAN validation (valid, with spaces, too short, invalid country code, exceeding max length, empty/null)
- [x] Add tests for BIC validation (valid 8-char, valid 11-char, wrong length, non-alphanumeric, empty/null)
- [x] Add tests for VAT ID (stored as-is, any format accepted, exceeding max length)
- [x] Add tests for create/update with financial fields
- [x] Add tests for clearing financial fields

**Acceptance criteria:**
- [x] All service tests pass
- [x] Every IBAN/BIC/VAT validation scenario from behaviors.md is covered

**Related behaviors:** All IBAN Validation scenarios, all BIC Validation scenarios, all VAT ID scenarios, Create company with all financial fields, Create company without financial fields, Update financial fields, Clear financial fields

---

## Step 10: Backend Tests — Controller

- [x] Add tests in `CompanyControllerTest.java` for POST with financial fields (201)
- [x] Add test for PUT with invalid IBAN (400)
- [x] Add test for GET returning financial fields

**Acceptance criteria:**
- [x] All controller tests pass

**Related behaviors:** POST company with financial fields, PUT company rejects invalid IBAN, GET company returns financial fields

---

## Step 11: Frontend Tests — Detail View

- [x] Add tests in company-detail test file for Finanzen section visibility
- [x] Test section hidden when all fields null
- [x] Test section shows only non-null fields
- [x] Test copy action on financial fields

**Acceptance criteria:**
- [x] All frontend detail tests pass

**Related behaviors:** Finanzen section displays when fields present, Finanzen section hidden when all fields null, Copy action on financial fields, Create company with partial financial fields (detail view part)

---

## Step 12: Frontend Tests — Form

- [x] Add tests in company-form test file for financial fields presence
- [x] Test that fields are pre-populated in edit mode
- [x] Test that empty fields are submitted as null

**Acceptance criteria:**
- [x] All frontend form tests pass

**Related behaviors:** Create company with all financial fields (form part), Create company without financial fields (form part), Update financial fields (form part), Clear financial fields (form part)

---

## Step 13: Update Project Documentation

- [x] Update `project-features.md` with financial fields feature
- [x] Update `project-structure.md` if new files were added
- [x] Update `project-architecture.md` if data flows changed

**Acceptance criteria:**
- [x] Documentation reflects the current state of the project

**Related behaviors:** None

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create company with all financial fields | Both | Step 7 (frontend), Step 9 (backend) |
| Create company without financial fields | Both | Step 7 (frontend), Step 9 (backend) |
| Create company with partial financial fields | Both | Step 6 (frontend detail), Step 9 (backend) |
| Valid IBAN is accepted | Backend | Step 9 |
| Valid IBAN with spaces is accepted | Backend | Step 9 |
| IBAN too short is rejected | Backend | Step 9 |
| IBAN with invalid country code is rejected | Backend | Step 9 |
| IBAN exceeding max length is rejected | Backend | Step 9 |
| Empty IBAN is accepted | Backend | Step 9 |
| Valid 8-character BIC is accepted | Backend | Step 9 |
| Valid 11-character BIC is accepted | Backend | Step 9 |
| BIC with wrong length is rejected | Backend | Step 9 |
| BIC with non-alphanumeric characters is rejected | Backend | Step 9 |
| Empty BIC is accepted | Backend | Step 9 |
| VAT ID stored as-is | Backend | Step 9 |
| VAT ID with any format is accepted | Backend | Step 9 |
| VAT ID exceeding max length is rejected | Backend | Step 9 |
| Update financial fields | Both | Step 7 (frontend), Step 9 (backend) |
| Clear financial fields | Both | Step 7 (frontend), Step 9 (backend) |
| Finanzen section displays when fields present | Frontend | Step 6, Step 11 |
| Finanzen section hidden when all fields null | Frontend | Step 6, Step 11 |
| Copy action on financial fields | Frontend | Step 6, Step 11 |
| Financial fields readonly when externally managed | — | Skipped (Spec 080 blocked) |
| Financial fields editable for Brevo companies | — | Skipped (Spec 080 blocked) |
| Financial fields editable for unlinked companies | — | Skipped (Spec 080 blocked) |
| Financial fields included in export | Backend | Step 4 |
| Empty financial fields exported as empty | Backend | Step 4 |
| GET company returns financial fields | Backend | Step 8, Step 10 |
| POST company with financial fields | Backend | Step 10 |
| PUT company rejects invalid IBAN | Backend | Step 10 |
