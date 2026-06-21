# Design: Brevo Origin Badge in Company and Contact Detail Views

## GitHub Issue

_To be created_

## Prerequisites

This spec depends on the following specs being implemented first:

| Spec | Dependency |
|------|-----------|
| 021 — Fix Brevo company ID | `brevoCompanyId` is `String` on companies |
| 023 — Contact brevo cleanup | Contact API exposes computed `brevo: true/false` field |

## Summary

Display a small tag below the name/title in both company and contact detail views when the record was imported from Brevo. For contacts, this replaces the existing "Synced to Brevo" checkbox which is removed. The layout reserves fixed space for the tag area so the view does not shift between Brevo and non-Brevo records.

## Goals

- Show a "Brevo" tag below the name in company and contact detail views for Brevo-imported records
- Remove the "Synced to Brevo" checkbox from the contact detail view
- Add a computed `brevo` field to the Company API (same pattern as contacts)
- Consistent layout regardless of whether the tag is visible

## Non-goals

- Showing a tag for non-Brevo records (e.g., "Manually created")
- Making the tag clickable or interactive
- Changing the list/table views (covered by Spec 024)

## Technical Approach

### Backend — Company DTO

**`CompanyDto.java`**: Add a `boolean brevo` record component, computed from `brevoCompanyId != null` in the `fromEntity()` factory method.

```java
entity.getBrevoCompanyId() != null  // → brevo field
```

**Rationale:** Same pattern as the contact DTO after Spec 023. The `brevoCompanyId` itself is not exposed — only the derived boolean.

### Frontend — Types (`types.ts`)

Add `readonly brevo: boolean` to the `CompanyDto` interface.

### Frontend — Tag Component

The tag is a small, subtle label rendered below the name heading. It should be visually distinct from the "Archived" badge pattern — it's metadata, not a status warning.

**Design:**
- Text: "Brevo" (same in both languages — it's a product name)
- Style: Small text (`text-xs`), muted color (`text-oe-gray-mid`), with a subtle border or background to distinguish it as a tag
- Position: Directly below the `<h1>` name heading, left-aligned
- Spacing: The container for the tag area always takes up the same vertical space (e.g., fixed `min-h` or `h` on the tag row), whether the tag is visible or not. This prevents the layout from jumping.

**Implementation:** When `brevo` is `false`, render an empty container with the same height. When `brevo` is `true`, render the tag inside that container.

### Frontend — Company Detail (`company-detail.tsx`)

Current structure (lines 53-65):
```
[Logo] [Company Name h1]
```

New structure:
```
[Logo] [Company Name h1]
       [Brevo tag or empty space]
```

Add the tag row below the `<h1>` heading, inside the same flex container. The tag is conditionally rendered but the container space is always reserved.

### Frontend — Contact Detail (`contact-detail.tsx`)

Current structure (lines 93-125):
```
[Photo] [Contact Name h1]
```

New structure:
```
[Photo] [Contact Name h1]
        [Brevo tag or empty space]
```

Same pattern as company detail.

**Remove:** The `<CheckboxField label={S.detail.brevo} checked={contact.brevo} />` section (currently around line 169-171).

### Frontend — Translations (`en.ts`, `de.ts`)

No new translation keys needed — "Brevo" is a product name and not translated. The removed `brevo` / `syncedToBrevo` keys under `contacts.detail` can be cleaned up.

### Tests

| File | Change |
|------|--------|
| `CompanyDtoTest.java` | Assert `brevo` field is computed correctly |
| `CompanyControllerTest.java` | Assert `brevo` in API response |
| `company-detail.test.tsx` | Test tag renders for Brevo companies, hidden for non-Brevo |
| `contact-detail.test.tsx` | Test tag renders, checkbox removed |
| `contact-form.test.tsx` | Update test data if `brevo` field changes |

## Files Affected

| File | Change |
|------|--------|
| `backend/.../company/CompanyDto.java` | Add computed `brevo` field |
| `frontend/src/lib/types.ts` | Add `brevo` to `CompanyDto` interface |
| `frontend/src/components/company-detail.tsx` | Add Brevo tag below name |
| `frontend/src/components/contact-detail.tsx` | Add Brevo tag below name, remove checkbox |
| `frontend/src/lib/i18n/en.ts` | Clean up removed keys |
| `frontend/src/lib/i18n/de.ts` | Clean up removed keys |
| Backend test files (2) | Assert brevo field |
| Frontend test files (2-3) | Assert tag rendering |

## Open Questions

None.
