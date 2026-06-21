# Design: No Company Filter

## GitHub Issue

—

## Summary

The Contact list's company filter dropdown currently supports "All companies" (no filter) or selecting a specific company by UUID. There is no way to filter for contacts that are **not assigned to any company** (`companyId IS NULL`). A new "No company" option is added to the dropdown, backed by a new `noCompany` boolean query parameter on the backend.

## Goals

- Allow users to filter the contact list for contacts without a company association
- Carry the filter through to print view and CSV export
- Maintain backward compatibility with the existing `companyId` filter

## Non-goals

- Changing the data model (contacts can already have `companyId = null`)
- Adding a similar filter to the company list (companies don't have parent entities)

## Technical Approach

### Backend

#### New query parameter

Add a `noCompany` boolean parameter to `GET /api/contacts`:

```
GET /api/contacts?noCompany=true
```

- `noCompany` is `required = false`, defaults to `false`
- When `true`, the Specification adds `WHERE company IS NULL`
- **Conflict rule:** If both `companyId` and `noCompany=true` are provided, return `400 Bad Request`

**Rationale:** A separate boolean parameter is cleaner than overloading the existing `companyId` UUID parameter with a sentinel string value. The `companyId` parameter is typed as `UUID` and Spring would throw a parsing error on non-UUID values.

#### Service layer

Add the `noCompany` condition to `ContactService.list()`:

```java
public Page<ContactDto> list(String search, UUID companyId, boolean noCompany,
                             String language, Boolean brevo, Pageable pageable) {
    if (companyId != null && noCompany) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Cannot combine companyId and noCompany filters");
    }
    // ... existing spec building ...
    if (noCompany) {
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("company")));
    }
}
```

#### Export endpoint

The same `noCompany` parameter is added to `GET /api/contacts/export` (Spec 038) for consistency.

### Frontend

#### Dropdown option

Add a "No company" option to the company filter `<Select>` in `contact-list.tsx`, positioned between "All companies" and the company list:

```
All companies       → value=""  (or "all")
No company          → value="none"
---
Company A           → value="<uuid>"
Company B           → value="<uuid>"
```

The label "No company" / "Keine Firma" is translated via i18n.

#### API call mapping

When `companyIdFilter === "none"`:
- Send `noCompany=true` to the API (no `companyId` parameter)

When `companyIdFilter` is a UUID:
- Send `companyId=<uuid>` to the API (no `noCompany` parameter)

When `companyIdFilter` is empty/all:
- Send neither parameter

#### Print view

Pass `noCompany=true` as a URL parameter to the print view when the "No company" filter is active. The print page reads and forwards it to the API.

### Files Affected

**Backend (modified):**
- `ContactRestController.java` — add `noCompany` parameter to list and export endpoints
- `ContactService.java` — add `noCompany` filter logic with conflict validation

**Frontend (modified):**
- `frontend/src/components/contact-list.tsx` — add "No company" dropdown option, map to API parameter
- `frontend/src/app/contacts/print/page.tsx` — read and forward `noCompany` parameter
- `frontend/src/lib/api.ts` — add `noCompany` parameter to `getContacts()` function
- `frontend/src/lib/i18n/de.ts` — add "Keine Firma" translation
- `frontend/src/lib/i18n/en.ts` — add "No company" translation

## Open Questions

None — all details resolved during design discussion.
