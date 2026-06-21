# Design: Replace Separate Contact Filters with Unified Search Field

## GitHub Issue

_To be created_

## Summary

The contact list table has three separate text filter inputs (firstName, lastName, email). These are replaced by a single search field that performs a multi-word, multi-field search across firstName, lastName, email, and associated company name. Each word must match in at least one of the four fields (AND across words, OR across fields). The existing dropdown filters (company, language, brevo) remain unchanged.

## Goals

- Replace three text inputs with a single search field
- Search across firstName, lastName, email, and company name
- Multi-word search: each word must match in at least one field
- Remove old `firstName`, `lastName`, `email` API parameters
- Add new `search` API parameter

## Non-goals

- Removing the company or language dropdown filters
- Full-text search or fuzzy matching
- Searching across additional fields (phone, position, etc.)

## Technical Approach

### Search semantics

Input: `"Anna Schmidt"`

1. Split by whitespace → `["Anna", "Schmidt"]`
2. For each word, create an OR specification across four fields (case-insensitive contains):
   - `firstName ILIKE '%anna%' OR lastName ILIKE '%anna%' OR email ILIKE '%anna%' OR company.name ILIKE '%anna%'`
3. AND all word-specifications together:
   - `(word1 matches any field) AND (word2 matches any field)`
4. AND with remaining filters (company dropdown, language dropdown, brevo filter)

This finds "Anna Schmidt" at company "Acme" but also finds "Anna Müller" at company "Schmidt GmbH" — because "Anna" matches firstName and "Schmidt" matches company name.

### Backend — Controller (`ContactController.java`)

Replace the three `@RequestParam` parameters with a single one:

```java
// Remove:
@RequestParam(required = false) final String firstName,
@RequestParam(required = false) final String lastName,
@RequestParam(required = false) final String email,

// Add:
@RequestParam(required = false) final String search,
```

Update the `list()` call to pass `search` instead of the three individual parameters.

### Backend — Service (`ContactService.java`)

Replace the three individual specification clauses with the new search logic:

```java
if (search != null && !search.isBlank()) {
    final String[] words = search.trim().split("\\s+");
    for (final String word : words) {
        final String pattern = "%" + word.toLowerCase() + "%";
        spec = spec.and((root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("firstName")), pattern),
            cb.like(cb.lower(root.get("lastName")), pattern),
            cb.like(cb.lower(root.get("email")), pattern),
            cb.like(cb.lower(root.get("company").get("name")), pattern)
        ));
    }
}
```

**Note on company JOIN:** `root.get("company").get("name")` creates an implicit inner join. Contacts without a company would be excluded. To include them, use a left join:

```java
final Join<ContactEntity, CompanyEntity> companyJoin = root.join("company", JoinType.LEFT);
// then: cb.like(cb.lower(companyJoin.get("name")), pattern)
```

**Rationale:** A LEFT JOIN ensures contacts without a company are still found when searching by name or email. The join must be created once outside the word loop and reused.

### Frontend — Contact list (`contact-list.tsx`)

1. **Remove state**: `firstNameFilter`, `lastNameFilter`, `emailFilter`
2. **Add state**: `searchFilter` (single string)
3. **Remove UI**: Three `<Input>` elements for firstName, lastName, email
4. **Add UI**: One `<Input>` with placeholder "Search..." / "Suche..."
5. **Update API call**: Pass `search: searchFilter || undefined` instead of the three individual params
6. **Update page-reset effect**: Replace the three filter dependencies with `searchFilter`

### Frontend — API client (`api.ts`)

**`ContactListParams`**: Remove `firstName`, `lastName`, `email`. Add `search`:

```typescript
export interface ContactListParams {
  readonly page?: number;
  readonly size?: number;
  readonly search?: string;
  readonly companyId?: string;
  readonly language?: string;
  readonly brevo?: boolean;
}
```

Remove the three old query parameter lines. Add:

```typescript
if (params.search) searchParams.set("search", params.search);
```

### Frontend — Translations (`en.ts`, `de.ts`)

**Remove:**
- `contacts.filter.firstName`
- `contacts.filter.lastName`
- `contacts.filter.email`

**Add:**
- EN: `contacts.filter.search: "Search..."`
- DE: `contacts.filter.search: "Suche..."`

### Tests

| File | Change |
|------|--------|
| `ContactControllerTest.java` | Replace firstName/lastName/email filter tests with `search` param tests |
| `ContactServiceTest.java` | Test multi-word search logic, company name matching, contacts without company |
| `contact-list.test.tsx` | Replace three filter assertions with single search input test |

## Files Affected

| File | Change |
|------|--------|
| `backend/.../contact/ContactController.java` | Replace three params with `search` |
| `backend/.../contact/ContactService.java` | Multi-word, multi-field search specification with LEFT JOIN |
| `frontend/src/components/contact-list.tsx` | Single search input replacing three inputs |
| `frontend/src/lib/api.ts` | Update `ContactListParams` and query construction |
| `frontend/src/lib/i18n/en.ts` | Replace filter keys, add search placeholder |
| `frontend/src/lib/i18n/de.ts` | Replace filter keys, add search placeholder |
| Backend test files (2) | Update filter tests |
| Frontend test files (1) | Update filter assertions |

## Open Questions

None.
