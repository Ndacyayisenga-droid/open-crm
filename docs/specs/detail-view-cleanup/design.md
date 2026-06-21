# Design: Detail View Cleanup

## GitHub Issue

—

## Summary

Improve the readability of Company and Contact detail views by removing redundant fields and consolidating related information. Three changes:

1. **Contact:** Remove the separate firstName/lastName detail fields — the name is already displayed in the header.
2. **Contact:** Display language as a human-readable label in the current UI language instead of raw codes ("DE"/"EN").
3. **Company:** Merge the five address fields (street, houseNumber, zipCode, city, country) into a single multi-line address block.

These are display-only changes to the detail views. Edit forms, print views, tables, and the backend remain unchanged.

## Goals

- Reduce visual clutter by removing redundant name fields in the contact detail view
- Make language values immediately understandable without requiring users to interpret ISO codes
- Present company addresses as a natural, readable address block instead of fragmented fields

## Non-goals

- Changing the edit forms (firstName/lastName and address fields remain as separate inputs)
- Modifying the backend data model or API
- Changing print views or list tables
- Adding new address fields or language options

## Technical Approach

### 1. Contact: Remove name detail fields

In `contact-detail.tsx`, remove the two `<DetailField>` entries for `firstName` and `lastName`. The header already renders `fullName` (`${contact.firstName} ${contact.lastName}`), so no information is lost.

**Files affected:** `frontend/src/components/contact-detail.tsx`

### 2. Contact: Language display

Add translated language labels to the i18n files:

| `contact.language` | UI language DE | UI language EN |
|---------------------|----------------|----------------|
| `"DE"` | Deutsch | German |
| `"EN"` | Englisch | English |
| `null` | Unbekannt | Unknown |

Create a helper function (similar to the existing `genderLabel()`) that maps the raw language code to the translated label. Use this in the `<DetailField>` for language.

**Files affected:**
- `frontend/src/lib/i18n/de.ts` — add language display labels
- `frontend/src/lib/i18n/en.ts` — add language display labels
- `frontend/src/components/contact-detail.tsx` — use helper for language display

### 3. Company: Merged address block

Replace the five separate `<DetailField>` entries (street, houseNumber, zipCode, city, country) with a single "Address" field that renders a multi-line formatted block.

**Address format** (when all fields present):
```
Straße Hausnummer
PLZ Stadt
Land
```

**Line composition rules:**
- **Line 1:** `street` + space + `houseNumber`. If `street` is null, skip the entire line (houseNumber alone is meaningless). If `houseNumber` is null, show `street` alone.
- **Line 2:** `zipCode` + space + `city`. Either can appear alone if the other is null. If both null, skip the line.
- **Line 3:** `country`. If null, skip.

If **all** address fields are null, display the standard em-dash "—".

Lines are separated by line breaks within a single detail field. The `DetailField` component or a wrapper must support multi-line rendering for this.

**Rationale:** A custom address rendering (instead of reusing `DetailField` directly) is needed because the current `DetailField` only accepts a single string value. The address block needs line breaks, which requires either extending `DetailField` or rendering the address field with custom markup.

**Files affected:**
- `frontend/src/components/company-detail.tsx` — replace address DetailFields with formatted block
- `frontend/src/lib/i18n/de.ts` — add "Address" label
- `frontend/src/lib/i18n/en.ts` — add "Address" label

## Dependencies

None. All changes are frontend-only and self-contained.

## Open Questions

None — all details resolved during design discussion.
