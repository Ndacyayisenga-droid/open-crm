# Design: Merge Name Columns and Add Email Column to Contact List Table

## GitHub Issue

_To be created_

## Summary

The contact list table currently has separate "First Name" and "Last Name" columns. These should be merged into a single "Name" column displaying "firstName lastName". A new "Email" column is added between Name and Company. This reduces visual clutter and surfaces the email — a frequently needed piece of information — directly in the table.

## Goals

- Merge first name and last name into a single "Name" column
- Add an "Email" column between Name and Company
- Handle edge cases (empty first name) gracefully

## Non-goals

- Merging the firstName/lastName filter inputs into a single name filter (separate spec)
- Backend changes (API already returns all needed fields)
- Changing the default sort order (remains `lastName,asc`)

## Technical Approach

### Column order change

**Current:** Photo | First Name | Last Name | Company | Comments | Actions

**New:** Photo | Name | Email | Company | Comments | Actions

### Frontend — Contact list (`contact-list.tsx`)

**Table header:** Replace the two `<TableHead>` elements for firstName and lastName with a single "Name" head. Add an "Email" head after it.

**Table cells:** Replace the two `<TableCell>` elements with a single cell that renders:

```tsx
`${contact.firstName} ${contact.lastName}`.trim()
```

Using `.trim()` ensures no leading space when `firstName` is empty.

Add a new cell for email:

```tsx
<TableCell className="text-oe-gray-mid">
  {contact.email ?? "—"}
</TableCell>
```

**Rationale:** Same pattern as the company table's website column — show the value or "—" if null.

### Frontend — Translations (`en.ts`, `de.ts`)

**Replace** the separate `firstName` and `lastName` column keys with a single `name` key:

**English:**
```
contacts.columns.name: "Name"        // replaces firstName + lastName
contacts.columns.email: "Email"       // new
```

**German:**
```
contacts.columns.name: "Name"        // replaces firstName + lastName
contacts.columns.email: "E-Mail"      // new
```

Keep the existing `firstName` and `lastName` keys under `contacts.columns` if they are referenced elsewhere (e.g., filter labels). Remove them only if unused after this change.

### Sorting

The default sort remains `lastName,asc`. This is a backend sort parameter — the merged Name column in the frontend does not affect sorting behavior. Users see names sorted alphabetically by last name, which is correct for a combined "Name" column.

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/contact-list.tsx` | Merge name columns, add email column |
| `frontend/src/lib/i18n/en.ts` | Add `name` and `email` column keys |
| `frontend/src/lib/i18n/de.ts` | Add `name` and `email` column keys |
| `frontend/src/components/__tests__/contact-list.test.tsx` | Update column assertions |

## Open Questions

None.
