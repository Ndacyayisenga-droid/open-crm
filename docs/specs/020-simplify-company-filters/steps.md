# Implementation Steps: Simplify Company Filters

## Step 1: Remove city/country filters and sorting from backend and frontend

- [x] Backend: Remove `city` and `country` params from `CompanyController.list()` and `CompanyService.list()`
- [x] Backend: Remove Specification clauses for city and country
- [x] Backend tests: Remove city, country, and sort-related tests; update remaining `list()` calls
- [x] Frontend: Remove `sort`, `city`, `country` from `CompanyListParams` and API query construction
- [x] Frontend: Remove `getCompaniesForSelect()` sort param (backend default handles it)
- [x] Frontend: Remove sort/city/country state, UI elements, and imports from `company-list.tsx`
- [x] Frontend: Remove unused i18n keys (filter.city, filter.country, sort block) from de.ts and en.ts
- [x] Frontend tests: Remove city, country, and sorting tests from `company-list.test.tsx`

**Acceptance criteria:**
- [x] Backend: 227 tests pass, 0 failures
- [x] Frontend: 150 tests pass, TypeScript clean
- [x] Company list shows only name filter and archive toggle
- [x] Default sort by name ascending preserved via `@PageableDefault`
