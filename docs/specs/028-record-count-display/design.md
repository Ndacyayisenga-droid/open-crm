# Design: Show Total Record Count in Company and Contact List Tables

## GitHub Issue

_To be created_

## Summary

The company and contact list tables show pagination info ("Page 1 of 3") but not the total number of filtered records. Users have no way to see at a glance how many records match their current filters. The total count (`totalElements`) is already returned by the backend API — this is a frontend-only display change.

## Goals

- Show total record count alongside existing pagination info in both tables
- Count reflects current filter state
- Proper singular/plural handling in both languages

## Non-goals

- Backend changes (data already available in API response)
- Showing count in the empty state (empty state message is sufficient)

## Technical Approach

### Display format

The existing pagination text (left-aligned below the table) changes from:

```
Seite 1 von 3                    [Zurück] [Weiter]
```

to:

```
42 Firmen · Seite 1 von 3        [Zurück] [Weiter]
```

The record count is prepended to the existing page info, separated by ` · ` (middle dot with spaces).

### Frontend — Company list (`company-list.tsx`)

The pagination section already renders `data.totalPages` and `data.number`. Add `data.totalElements` to the display string.

Current (approximate):
```tsx
{S.pagination.page.replace("{current}", ...).replace("{total}", ...)}
```

New:
```tsx
{S.pagination.total.replace("{count}", String(data.totalElements))} · {S.pagination.page.replace("{current}", ...).replace("{total}", ...)}
```

Use a conditional translation key for singular vs. plural based on `data.totalElements`.

### Frontend — Contact list (`contact-list.tsx`)

Same pattern as company list.

### Frontend — Translations (`en.ts`, `de.ts`)

**English:**
```
companies.pagination.totalOne: "{count} Company"
companies.pagination.totalOther: "{count} Companies"

contacts.pagination.totalOne: "{count} Contact"
contacts.pagination.totalOther: "{count} Contacts"
```

**German:**
```
companies.pagination.totalOne: "{count} Firma"
companies.pagination.totalOther: "{count} Firmen"

contacts.pagination.totalOne: "{count} Kontakt"
contacts.pagination.totalOther: "{count} Kontakte"
```

**Rationale:** Separate keys for singular/plural instead of a runtime pluralization library — keeps it simple, the CRM only supports two languages, and there are only two entity types.

### Visibility

The record count is only shown when the table is visible (i.e., `data.content.length > 0`). When the empty state is shown (0 results), neither the pagination nor the count is displayed — the empty state message is sufficient.

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Add totalElements to pagination display |
| `frontend/src/components/contact-list.tsx` | Add totalElements to pagination display |
| `frontend/src/lib/i18n/en.ts` | Add singular/plural total keys |
| `frontend/src/lib/i18n/de.ts` | Add singular/plural total keys |
| `frontend/src/components/__tests__/company-list.test.tsx` | Assert count is displayed |
| `frontend/src/components/__tests__/contact-list.test.tsx` | Assert count is displayed |

## Open Questions

None.
