# Design: Company Phone Number

## GitHub Issue

—

## Summary

Add an optional phone number field to the Company data type. The field is a nullable free-text string with no format constraints, matching the existing `phoneNumber` field on Contact. It appears in the detail view (next to email) and the create/edit form (next to email), but not in the list table.

## Goals

- Allow users to store a phone number for companies
- Display and edit the phone number in the detail and form views
- Include the phone number in CSV export (Spec 038) and detail field actions (Spec 040)

## Non-goals

- Phone number format validation
- Adding a phone column to the company list table
- Brevo import of company phone numbers (Brevo does not provide this field)

## Technical Approach

### Backend

#### Database migration

Add a new Flyway migration to add a nullable `phone_number` column to the `company` table:

```sql
ALTER TABLE company ADD COLUMN phone_number VARCHAR(255);
```

#### Entity

Add `phoneNumber` field to `CompanyEntity`:

```java
@Column(name = "phone_number")
private String phoneNumber;
```

#### DTOs

- `CompanyDto` — add `phoneNumber: String | null`
- `CompanyCreateDto` — add `phoneNumber: String | null`
- `CompanyUpdateDto` — add `phoneNumber: String | null`

#### Service

Update the DTO mapping methods to include `phoneNumber` in both directions (entity ↔ DTO).

#### CSV Export (Spec 038)

Add `PHONE_NUMBER` to the `CompanyExportColumn` enum.

### Frontend

#### Types

Add `phoneNumber: string | null` to the `CompanyDto` TypeScript interface.

#### Detail view

Add a `DetailField` for phone number next to the email field. With Spec 040 implemented, the phone field gets `copyable` and `callable` props (copy-to-clipboard + `tel:` link).

#### Form (Create/Edit)

Add a phone number input field next to the email field, following the same pattern as the existing Contact phone field. Optional, no validation.

#### i18n

Add translations for the phone number label:
- DE: "Telefon"
- EN: "Phone"

(These keys may already exist in the contact translations — reuse if shared, or add to company-specific translations.)

### Files Affected

**Backend (new):**
- Flyway migration file — `ALTER TABLE company ADD COLUMN phone_number`

**Backend (modified):**
- `CompanyEntity.java` — add `phoneNumber` field
- `CompanyDto.java` — add `phoneNumber`
- `CompanyCreateDto.java` — add `phoneNumber`
- `CompanyUpdateDto.java` — add `phoneNumber`
- `CompanyService.java` — update DTO mapping
- `CompanyExportColumn.java` (Spec 038) — add `PHONE_NUMBER`

**Frontend (modified):**
- `frontend/src/lib/types.ts` — add `phoneNumber` to `CompanyDto`
- `frontend/src/components/company-detail.tsx` — add phone DetailField next to email
- `frontend/src/components/company-form.tsx` — add phone input next to email
- `frontend/src/lib/i18n/de.ts` — add phone label translation
- `frontend/src/lib/i18n/en.ts` — add phone label translation

## Dependencies

- **Spec 038 (CSV Export):** `PHONE_NUMBER` added to `CompanyExportColumn` enum
- **Spec 040 (Detail Field Actions):** Company phone gets `copyable` + `callable` (tel:) actions

## Open Questions

None — all details resolved during design discussion.
