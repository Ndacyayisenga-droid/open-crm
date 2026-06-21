# Design: Brevo Origin Filter for Company and Contact Lists

## GitHub Issue

_To be created_

## Summary

Add a filter to both the company list and contact list tables that allows filtering records by their Brevo origin: "All" (default), "From Brevo" (records with a Brevo ID), or "Not from Brevo" (records without a Brevo ID). This helps users distinguish between manually created records and those imported from Brevo.

## Prerequisites

This spec depends on the following specs being implemented first:

| Spec | Dependency |
|------|-----------|
| 020 — Simplify company filters | Company list has only name filter and archive toggle |
| 021 — Fix Brevo company ID | `brevoCompanyId` is `String`/`VARCHAR(50)` on companies |
| 022 — Remove double opt-in | `doubleOptIn` removed from contacts |
| 023 — Contact brevo cleanup | `syncedToBrevo` removed, `brevoId` is `String`/`VARCHAR(50)` on contacts |

**Do not implement this spec before the above are done.**

## Goals

- Add a three-way Brevo origin filter to the company list table
- Add the same filter to the contact list table
- Identical UI pattern (Select dropdown) for both tables

## Non-goals

- Refactoring the contact list filter UI (too many filters) — deferred to a future spec
- Showing the actual Brevo ID in the table columns

## Technical Approach

### Backend — Company list

**`CompanyController.java`**: Add a `brevo` query parameter of type `Boolean` (nullable):

```java
@RequestParam(required = false) final Boolean brevo
```

- `brevo=true` → only companies where `brevoCompanyId IS NOT NULL`
- `brevo=false` → only companies where `brevoCompanyId IS NULL`
- Parameter omitted → no filter (all companies)

**`CompanyService.java`**: Add a `brevo` parameter to the `list()` method. Add a new `Specification` clause:

```java
if (brevo != null) {
    if (brevo) {
        spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoCompanyId")));
    } else {
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoCompanyId")));
    }
}
```

**Rationale:** Using `Boolean` (nullable wrapper) allows three states: `true`, `false`, and absent. This is consistent with how `includeDeleted` works as a query parameter.

### Backend — Contact list

**`ContactController.java`**: Add the same `brevo` query parameter of type `Boolean`:

```java
@RequestParam(required = false) final Boolean brevo
```

**`ContactService.java`**: Add a `brevo` parameter to the `list()` method with the same `Specification` pattern, checking `brevoId` instead of `brevoCompanyId`.

### Frontend — API client (`api.ts`)

**`CompanyListParams`**: Add `readonly brevo?: boolean` (after Spec 020 has removed `sort`, `city`, `country`).

**`ContactListParams`**: Add `readonly brevo?: boolean`.

Add query parameter construction for both:

```typescript
if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
```

### Frontend — Company list (`company-list.tsx`)

Add state: `const [brevoFilter, setBrevoFilter] = useState("all")`

Add a Select dropdown alongside the name filter:

```
[Name filter input] [Brevo dropdown: All | From Brevo | Not from Brevo] [Archive toggle]
```

Map the dropdown value to the API parameter:
- `"all"` → `brevo: undefined`
- `"true"` → `brevo: true`
- `"false"` → `brevo: false`

Add `brevoFilter` to the page-reset effect dependencies.

### Frontend — Contact list (`contact-list.tsx`)

Same pattern: add `brevoFilter` state, Select dropdown, and API parameter mapping. The dropdown goes after the existing filters.

### Frontend — Translations (`en.ts`, `de.ts`)

Add shared filter labels (usable by both tables):

**English:**
```
brevoFilter: {
  label: "Brevo",
  all: "All",
  fromBrevo: "From Brevo",
  notFromBrevo: "Not from Brevo",
}
```

**German:**
```
brevoFilter: {
  label: "Brevo",
  all: "Alle",
  fromBrevo: "Aus Brevo",
  notFromBrevo: "Nicht aus Brevo",
}
```

**Rationale:** Placing the translations at a shared level (or duplicating in both `companies` and `contacts` sections) keeps them close to usage. Since both tables use identical labels, a shared key avoids duplication.

### Tests

| File | Change |
|------|--------|
| `CompanyControllerTest.java` | Add tests for `?brevo=true` and `?brevo=false` |
| `CompanyServiceTest.java` | Test specification filtering on `brevoCompanyId` |
| `ContactControllerTest.java` | Add tests for `?brevo=true` and `?brevo=false` |
| `ContactServiceTest.java` | Test specification filtering on `brevoId` |
| `company-list.test.tsx` | Test dropdown rendering and API call with brevo parameter |
| `contact-list.test.tsx` | Test dropdown rendering and API call with brevo parameter |

## Files Affected

| File | Change |
|------|--------|
| `backend/.../company/CompanyController.java` | Add `brevo` query parameter |
| `backend/.../company/CompanyService.java` | Add `brevo` parameter and specification clause |
| `backend/.../contact/ContactController.java` | Add `brevo` query parameter |
| `backend/.../contact/ContactService.java` | Add `brevo` parameter and specification clause |
| `frontend/src/lib/api.ts` | Add `brevo` to both list params interfaces |
| `frontend/src/components/company-list.tsx` | Add brevo filter dropdown and state |
| `frontend/src/components/contact-list.tsx` | Add brevo filter dropdown and state |
| `frontend/src/lib/i18n/en.ts` | Add brevo filter translations |
| `frontend/src/lib/i18n/de.ts` | Add brevo filter translations |
| Backend test files (2-4) | Add filter tests |
| Frontend test files (2) | Add dropdown and API call tests |

## Open Questions

None.
