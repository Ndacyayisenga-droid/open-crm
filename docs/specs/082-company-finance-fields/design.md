# Design: Company Financial Fields

## GitHub Issue

_To be created by the user._

## Summary

Companies in the CRM need financial information for invoicing context — bank name, BIC, IBAN, and VAT ID (Umsatzsteuer-ID). These four optional fields are added to the Company entity, displayed in a dedicated "Finanzen" section in the detail view and form, included in CSV export, and protected as readonly when the company is managed by an external system.

## Goals

- Add four optional financial fields to the Company entity
- Display them in a grouped "Finanzen" section in detail view and form
- Include them in CSV export
- Validate IBAN and BIC format server-side
- Protect fields as readonly when externally managed (via Spec 080)

## Non-goals

- Displaying financial fields in the company list table
- Country-specific VAT ID format validation (no global standard)
- Importing these fields from Brevo (Brevo does not have bank data)
- SevDesk field mapping (will be added to Spec 081)

## Dependencies

- **Spec 080 (External Links Table)** — readonly protection uses the generic external links model

## Data Model

### Migration V23: Add financial fields to companies

```sql
ALTER TABLE companies ADD COLUMN bank_name VARCHAR(255);
ALTER TABLE companies ADD COLUMN bic VARCHAR(11);
ALTER TABLE companies ADD COLUMN iban VARCHAR(34);
ALTER TABLE companies ADD COLUMN vat_id VARCHAR(20);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `bank_name` | `VARCHAR(255)` | nullable | Bank name (free text) |
| `bic` | `VARCHAR(11)` | nullable | Bank Identifier Code (8 or 11 chars) |
| `iban` | `VARCHAR(34)` | nullable | International Bank Account Number (max 34 chars per ISO 13616) |
| `vat_id` | `VARCHAR(20)` | nullable | VAT identification number (free text, no format validation) |

**Rationale:** IBAN max length is 34 characters per ISO 13616. BIC is either 8 or 11 characters per ISO 9362. VAT IDs vary by country with no global standard, so 20 characters as free text is sufficient.

## Technical Approach

### Backend: Entity

**File:** `CompanyEntity.java`

Add four new nullable fields:
- `bankName` (String, max 255)
- `bic` (String, max 11)
- `iban` (String, max 34)
- `vatId` (String, max 20)

Standard getters/setters with null-safe pattern.

### Backend: Validation

**IBAN validation** (in `CompanyService` on create/update):
- Remove all whitespace
- Length: 15–34 characters
- First 2 characters: uppercase letters (country code)
- Remaining characters: alphanumeric
- Validation only when field is non-null and non-blank

**BIC validation** (in `CompanyService` on create/update):
- Length: exactly 8 or 11 characters
- All uppercase alphanumeric
- Validation only when field is non-null and non-blank

**VAT ID:** No format validation — stored as-is (trimmed).

**Rationale:** Full IBAN checksum validation (mod-97) or BIC registry lookup would add complexity with marginal benefit for a CRM. Format checks catch obvious typos without over-engineering.

### Backend: DTOs

**`CompanyDto`:** Add `bankName`, `bic`, `iban`, `vatId` fields (all `String`, nullable)

**`CompanyCreateDto` / `CompanyUpdateDto`:** Add the same four optional fields with `@Size` constraints matching the column lengths.

### Backend: CSV Export

**`CompanyExportColumn`:** Add four new enum values:
- `BANK_NAME` — `dto -> dto.bankName()`
- `BIC` — `dto -> dto.bic()`
- `IBAN` — `dto -> dto.iban()`
- `VAT_ID` — `dto -> dto.vatId()`

### Backend: Readonly Protection

When the company is managed by an external system (checked via `ExternalLinkService.isLinked()`), the financial fields are included in the readonly field set for SEVDESK. For BREVO, these fields are **not** readonly (Brevo does not manage bank data).

This means the per-system readonly field mapping (from Spec 080) needs to include:
- **SEVDESK:** name, email, website, phoneNumber, street, houseNumber, zipCode, city, country, bankName, bic, iban, vatId
- **BREVO:** firstName, lastName, email, language (contacts only — no company field protection for Brevo)

### Frontend: Types

**`types.ts`:** Add to `CompanyDto`:
```typescript
readonly bankName: string | null;
readonly bic: string | null;
readonly iban: string | null;
readonly vatId: string | null;
```

Add same fields to `CompanyCreateDto` as optional.

### Frontend: Detail View

**`company-detail.tsx`:** Add a "Finanzen" section below the existing fields:
- Section heading: "Finanzen" / "Finance" (i18n)
- Fields displayed with the standard detail field layout:
  - Bank — with copy action
  - BIC — with copy action
  - IBAN — with copy action
  - Umsatzsteuer-ID — with copy action
- Section only rendered if at least one financial field has a value
- If all four are null, the section is hidden

### Frontend: Form

**`company-form.tsx`:** Add a "Finanzen" section in the create/edit form:
- Section heading: "Finanzen" / "Finance"
- Four text input fields:
  - Bank (placeholder: e.g. "Deutsche Bank")
  - BIC (placeholder: e.g. "DEUTDEDB")
  - IBAN (placeholder: e.g. "DE89370400440532013000")
  - Umsatzsteuer-ID (placeholder: e.g. "DE123456789")
- All fields optional
- When externally managed: disabled with "Managed by {source}" hint

### Frontend: i18n

Add to both `en.ts` and `de.ts`:

```
companies.finance.title: "Finance" / "Finanzen"
companies.finance.bankName: "Bank" / "Bank"
companies.finance.bic: "BIC" / "BIC"
companies.finance.iban: "IBAN" / "IBAN"
companies.finance.vatId: "VAT ID" / "Umsatzsteuer-ID"
companies.finance.bankNamePlaceholder: "e.g. Deutsche Bank" / "z.B. Deutsche Bank"
companies.finance.bicPlaceholder: "e.g. DEUTDEDB" / "z.B. DEUTDEDB"
companies.finance.ibanPlaceholder: "e.g. DE89370400440532013000" / "z.B. DE89370400440532013000"
companies.finance.vatIdPlaceholder: "e.g. DE123456789" / "z.B. DE123456789"
```

CSV export column labels:
```
csvExport.companyColumns.bankName: "Bank" / "Bank"
csvExport.companyColumns.bic: "BIC" / "BIC"
csvExport.companyColumns.iban: "IBAN" / "IBAN"
csvExport.companyColumns.vatId: "VAT ID" / "Umsatzsteuer-ID"
```

## Open Questions

None.
