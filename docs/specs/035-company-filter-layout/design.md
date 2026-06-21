# Design: Move Archive Toggle into Company Filter Row

## GitHub Issue

_To be created_

## Summary

The "Show Archived" toggle button in the company list is on a separate line below the filter row. This wastes vertical space. The toggle should be moved into the same row as the name filter and Brevo dropdown.

## Goals

- Move the archive toggle into the filter row (same line as other filters)

## Non-goals

- Changing the archive toggle behavior or styling

## Technical Approach

### Frontend — Company list (`company-list.tsx`)

Current layout:
```
[Name filter] [Brevo dropdown]
[Archive toggle]                    ← separate row (mb-4)
```

New layout:
```
[Name filter] [Brevo dropdown] [Archive toggle]    ← single row
```

Move the `<Button>` for the archive toggle from its own `<div className="mb-4">` container into the existing filter `<div className="mb-4 flex flex-col gap-3 sm:flex-row">` container. Remove the now-empty wrapper div.

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Move archive toggle into filter row |

## Open Questions

None.
