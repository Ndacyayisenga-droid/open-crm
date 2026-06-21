# Design: Page Size Selector

## GitHub Issue

—

## Summary

The company, contact, and tag list tables currently use a hardcoded page size of 20 records. Users have no way to control how many records are displayed per page. This feature adds a page size selector dropdown to each list view, allowing users to choose between 10, 20, 50, 100, and 200 records per page. The selected size is persisted per list in `localStorage` so it survives page navigation and browser refreshes.

## Goals

- Let users choose the number of records displayed per page in list views
- Persist the choice per list across page reloads
- Provide sensible options: 10, 20 (default), 50, 100, 200

## Non-goals

- Changing page size for comment lists (they use a "load more" pattern, not pagination)
- Affecting print views (they fetch all records independently)
- Affecting CSV export (backend returns all matching records without pagination)
- Backend changes (Spring Data's `Pageable` already accepts `size` from query parameters)
- URL-based persistence (the page size is a user preference, not a shareable state)

## Technical Approach

### Backend

**No changes required.** The Spring Data controllers already use `@PageableDefault(size = 20)` which serves as a fallback when no `size` parameter is provided. When the frontend sends `?size=50`, Spring Data automatically applies it.

**Rationale:** The backend already supports dynamic page sizes through Spring's `Pageable` abstraction. Adding backend-side validation for allowed sizes is unnecessary — the fixed dropdown options on the frontend ensure only valid sizes are sent.

### Frontend

**Page size selector UI:**
- A `<Select>` dropdown (shadcn/ui) with options: 10, 20, 50, 100, 200
- Placed in the pagination bar, to the left of the record count display
- Compact styling consistent with existing pagination controls

**State management:**
- Each list component gets a `pageSize` state variable initialized from `localStorage`
- On page size change: update state, persist to `localStorage`, reset page to 0
- The `pageSize` is passed to the API call instead of the hardcoded `20`

**localStorage keys (per list, separate persistence):**
- `pageSize.companies`
- `pageSize.contacts`
- `pageSize.tags`

**Rationale for localStorage over cookies:** The codebase already uses `localStorage` for user preferences (language setting in `language-context.tsx`). Using the same mechanism keeps the approach consistent. Cookies would be sent to the server on every request unnecessarily.

**Tag list range calculation fix:** The tag list currently hardcodes `20` in its "Showing X–Y of Z" range calculation. This must be replaced with the dynamic `pageSize` value.

### i18n

Add translation key for the page size label:

| Key | EN | DE |
|-----|----|----|
| `pagination.perPage` | "per page" | "pro Seite" |

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Add pageSize state, Select dropdown, pass to API |
| `frontend/src/components/contact-list.tsx` | Add pageSize state, Select dropdown, pass to API |
| `frontend/src/components/tag-list.tsx` | Add pageSize state, Select dropdown, fix range calc |
| `frontend/src/lib/i18n/en.ts` | Add `pagination.perPage` |
| `frontend/src/lib/i18n/de.ts` | Add `pagination.perPage` |

## Security Considerations

- The backend's `@PageableDefault` annotation provides a safe default
- Spring Data caps maximum page size via `spring.data.web.pageable.max-page-size` (defaults to 2000) — the largest selectable value (200) is well within this limit
- No user input is directly passed to queries — Spring Data handles parameterization
