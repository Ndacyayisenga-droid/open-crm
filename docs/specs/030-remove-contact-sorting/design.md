# Design: Remove Sorting Dropdown from Contact List Table

## GitHub Issue

_To be created_

## Summary

The contact list table has a sorting dropdown with six options (lastName A-Z/Z-A, firstName A-Z/Z-A, newest/oldest first), but only lastName A-Z is used. A dropdown with one useful option adds UI clutter. Same situation as Spec 020 for the company list. The dropdown is removed and contacts are always sorted by lastName ascending via the backend default.

## Goals

- Remove the sorting dropdown from the contact list UI
- Remove the `sort` query parameter from the frontend API client for contacts
- Remove the `sort` state variable and related dependencies from the contact list component
- Clean up unused i18n keys

## Non-goals

- Adding new sort options (deferred to future spec when needed)
- Changing the company list (already done in Spec 020)

## Technical Approach

### Frontend — Contact list (`contact-list.tsx`)

1. **Remove state**: `sort` state variable (currently `useState("lastName,asc")`)
2. **Remove UI**: The `<Select>` dropdown for sorting
3. **Remove from API call**: `sort` parameter from the `getContacts()` call
4. **Remove from page-reset effect**: `sort` from the dependency array of the `useEffect` that resets page to 0
5. **Remove unused imports**: `Select`, `SelectContent`, `SelectItem`, `SelectTrigger`, `SelectValue` (if no longer used after Spec 024's Brevo filter also uses Select — check before removing)

### Frontend — API client (`api.ts`)

1. **Remove `sort` from `ContactListParams`**: Remove the `readonly sort?: string` field
2. **Remove query parameter construction**: Remove the line that appends `sort` to `URLSearchParams`

### Frontend — Translations (`en.ts`, `de.ts`)

Remove the entire `contacts.sort` block:
- `contacts.sort.label`
- `contacts.sort.lastNameAsc`
- `contacts.sort.lastNameDesc`
- `contacts.sort.firstNameAsc`
- `contacts.sort.firstNameDesc`
- `contacts.sort.createdAtDesc`
- `contacts.sort.createdAtAsc`

### Backend — No change needed

The `@PageableDefault(size = 20, sort = "lastName")` on the contact list endpoint already provides the correct default sort. Spring Data's `Pageable` still technically accepts a `sort` query parameter, but without frontend usage this is acceptable — same decision as Spec 020.

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/contact-list.tsx` | Remove sort state, dropdown, API param, effect dependency |
| `frontend/src/lib/api.ts` | Remove `sort` from `ContactListParams` and query construction |
| `frontend/src/lib/i18n/en.ts` | Remove `contacts.sort.*` keys |
| `frontend/src/lib/i18n/de.ts` | Remove `contacts.sort.*` keys |
| `frontend/src/components/__tests__/contact-list.test.tsx` | Remove sort-related assertions |

## Open Questions

None.
