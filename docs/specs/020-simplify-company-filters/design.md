# Design: Simplify Company List Filters and Sorting

## GitHub Issue

_To be created_

## Summary

The company list table has filter and sorting options that are never used in practice. The city and country filters are irrelevant because the data is often not populated (the team works mostly online) and nobody filters by these fields. The sorting dropdown offers four options, but only "Name A-Z" is ever used — a dropdown with effectively one useful entry adds unnecessary UI clutter.

This spec removes the unused filters and sorting UI to create a cleaner, leaner company list. The fixed default sort (name ascending) remains as the only sort order.

## Goals

- Remove city and country filters from the company list UI
- Remove the sorting dropdown entirely
- Remove the `city`, `country`, and `sort` query parameters from the backend company list API
- Keep the company list always sorted by name ascending (hardcoded default)
- Keep the name filter and archive toggle unchanged

## Non-goals

- Changes to the contact list table (separate scope)
- Adding new filter or sort options (deferred to a future spec when status/funnel fields are introduced)
- Removing city/country fields from the company data model or detail view — those remain

## Technical Approach

### Frontend (`company-list.tsx`)

1. **Remove state variables**: `cityFilter`, `countryFilter`, `sort`
2. **Remove UI elements**:
   - City filter `<Input>` (line 119-124)
   - Country filter `<Input>` (line 125-130)
   - Sort `<Select>` dropdown (lines 131-141)
3. **Remove unused imports**: `Select`, `SelectContent`, `SelectItem`, `SelectTrigger`, `SelectValue`
4. **Simplify `fetchCompanies`**: Remove `city`, `country`, `sort` from the API call. The backend default sort (`name,asc` via `@PageableDefault`) handles the ordering.
5. **Simplify `useEffect` dependencies**: Remove `cityFilter`, `countryFilter`, `sort` from the page-reset effect.

### Frontend API client (`api.ts`)

1. **Remove fields from `CompanyListParams`**: Remove `sort`, `city`, `country`
2. **Remove query parameter construction**: Remove the lines that append `sort`, `city`, `country` to `URLSearchParams`

**Rationale**: Removing the parameters from the TypeScript interface ensures no frontend code accidentally passes unused filters. The backend removal ensures the API contract is clean.

**Note**: `getCompaniesForSelect()` currently passes `sort: "name,asc"` — this must also be updated since `sort` is removed from `CompanyListParams`. The backend default sort handles this automatically.

### Frontend i18n (`en.ts`, `de.ts`)

1. **Remove translation keys**: `companies.filter.city`, `companies.filter.country`, and the entire `companies.sort` block
2. **Remove from type definition** (`index.ts`): Update the `Translations` type if it explicitly lists these keys

### Backend Controller (`CompanyController.java`)

1. **Remove parameters**: Remove `city` and `country` `@RequestParam` from the `list()` method
2. **Keep `@PageableDefault`**: The `sort = "name"` default remains, but clients can no longer override it via query parameter. Since Spring Data still accepts `sort` via `Pageable`, explicitly ignore or document that the sort is fixed.

**Rationale**: Removing the controller parameters is a clean API change. The `Pageable` parameter from Spring Data still technically accepts `sort` in the query string, but without frontend usage this is acceptable. A stricter approach (custom `Pageable` resolver) would be over-engineering for this case.

### Backend Service (`CompanyService.java`)

1. **Remove parameters**: Remove `city` and `country` from the `list()` method signature
2. **Remove specification clauses**: Remove the city and country `Specification` blocks (lines 160-167)

### Backend Tests

1. **Update any tests** that call `companyService.list()` or the controller with `city`/`country` parameters

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Remove city/country filters, sort dropdown, related state |
| `frontend/src/lib/api.ts` | Remove `sort`, `city`, `country` from `CompanyListParams` and query construction |
| `frontend/src/lib/i18n/en.ts` | Remove unused translation keys |
| `frontend/src/lib/i18n/de.ts` | Remove unused translation keys |
| `frontend/src/lib/i18n/index.ts` | Update `Translations` type (if explicit) |
| `frontend/src/components/__tests__/company-list.test.tsx` | Update tests for removed UI elements |
| `backend/src/main/java/.../CompanyController.java` | Remove `city`, `country` params |
| `backend/src/main/java/.../CompanyService.java` | Remove `city`, `country` params and spec clauses |
| Backend test files | Update service/controller test calls |

## Open Questions

None — all decisions resolved during the grill session.
