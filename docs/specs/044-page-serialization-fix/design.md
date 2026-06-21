# Design: Page Serialization Fix

## GitHub Issue

—

## Summary

The backend logs a warning on every paginated API call because `PageImpl` instances are serialized directly to JSON, which Spring Data considers unstable. The fix is to enable Spring Data's `VIA_DTO` page serialization mode, which produces a stable JSON structure. This changes the response shape, requiring frontend adjustments.

## Reproduction

1. Start the backend
2. Open the Company or Contact list in the frontend
3. Observe the backend log:
   ```
   WARN Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
   ```

## Root Cause Analysis

The Company and Contact controllers return `Page<T>` directly. Spring Boot's Jackson serialization converts this to JSON, but the `PageImpl` class is not designed as a stable serialization target. Spring Data warns about this and recommends using `PagedModel` or the `VIA_DTO` serialization mode.

## Fix Approach

### Backend

Add `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to the Spring Boot application class (or a configuration class). This globally changes the JSON serialization of all `Page<T>` return types.

**JSON structure change:**

Before (current, unstable):
```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

After (`VIA_DTO`):
```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 42,
    "totalPages": 3
  }
}
```

Key differences:
- Pagination metadata moves into a nested `page` object
- `first` and `last` boolean fields are removed
- `content` remains at the top level

**Rationale:** The `@EnableSpringDataWebSupport(VIA_DTO)` annotation is the simplest and Spring-recommended approach. It requires no changes to controller return types or service layer code. The alternative (manually wrapping in `PagedModel`) would require changing every paginated endpoint.

### Frontend

#### TypeScript type

Replace the current `Page<T>` interface:

```typescript
export interface Page<T> {
  readonly content: readonly T[];
  readonly page: {
    readonly size: number;
    readonly number: number;
    readonly totalElements: number;
    readonly totalPages: number;
  };
}
```

#### Derived properties

Where `last` was used (print views, CSV export paginated fetch loops), compute it:

```typescript
const isLast = result.page.number >= result.page.totalPages - 1;
```

Where `first` was used (if anywhere), compute it:

```typescript
const isFirst = result.page.number === 0;
```

#### Field access updates

All accesses to `result.totalElements`, `result.totalPages`, `result.number`, `result.size` change to `result.page.totalElements`, `result.page.totalPages`, etc.

### Files Affected

**Backend (modified):**
- `CrmApplication.java` (or config class) — add `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`

**Frontend (modified):**
- `frontend/src/lib/types.ts` — update `Page<T>` interface
- `frontend/src/components/company-list.tsx` — update pagination metadata access
- `frontend/src/components/contact-list.tsx` — update pagination metadata access
- `frontend/src/app/companies/print/page.tsx` — replace `result.last` with computed check
- `frontend/src/app/contacts/print/page.tsx` — replace `result.last` with computed check

## Regression Risk

- The JSON structure change is a breaking change for any external API consumer. Since this is an internal CRM with only the Next.js frontend as consumer, the risk is contained — both sides are updated together.
- All paginated responses change simultaneously (companies, contacts, comments). Every frontend component that reads pagination metadata must be updated.

## Open Questions

None — all details resolved during design discussion.
