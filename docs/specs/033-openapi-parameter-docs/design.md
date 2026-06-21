# Design: Add OpenAPI @Parameter Annotations to All REST Controllers

## GitHub Issue

_To be created_

## Summary

REST controller methods have `@Operation` and `@ApiResponse` annotations, and DTOs have `@Schema` annotations, but method parameters (`@RequestParam`, `@PathVariable`, `Pageable`) lack `@Parameter` descriptions. Swagger UI shows parameter names but no descriptions, making the API harder to understand for anyone using it directly.

## Goals

- Add `@Parameter(description = "...")` to all `@RequestParam`, `@PathVariable`, and `Pageable` parameters across all controllers
- Swagger UI shows meaningful descriptions for every parameter

## Non-goals

- Annotating `@RequestBody` parameters (already documented via `@Schema` on DTOs)
- Changing any logic, tests, or behavior
- Adding `@Parameter` to the HealthController (no parameters)

## Technical Approach

Add `io.swagger.v3.oas.annotations.Parameter` annotation to each method parameter. Descriptions should be concise and in English.

### CompanyController — 17 parameters

| Method | Parameter | Description |
|--------|-----------|-------------|
| `list` | `name` | "Partial company name filter (case-insensitive contains)" |
| `list` | `includeDeleted` | "Whether to include soft-deleted companies" |
| `list` | `brevo` | "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all" |
| `list` | `pageable` | "Pagination parameters (page, size)" |
| `getById` | `id` | "The company ID" |
| `create` | — | (skip, @RequestBody) |
| `update` | `id` | "The company ID" |
| `update` | — | (skip, @RequestBody) |
| `delete` | `id` | "The company ID" |
| `restore` | `id` | "The company ID" |
| `uploadLogo` | `id` | "The company ID" |
| `uploadLogo` | `file` | "The logo image file (JPEG, PNG, or SVG; max 2 MB)" |
| `getLogo` | `id` | "The company ID" |
| `deleteLogo` | `id` | "The company ID" |
| `listComments` | `id` | "The company ID" |
| `listComments` | `pageable` | "Pagination parameters (page, size)" |
| `addComment` | `id` | "The company ID" |
| `addComment` | — | (skip, @RequestBody) |

### ContactController — 19 parameters

| Method | Parameter | Description |
|--------|-----------|-------------|
| `list` | `firstName` | "Partial first name filter (case-insensitive contains)" |
| `list` | `lastName` | "Partial last name filter (case-insensitive contains)" |
| `list` | `email` | "Partial email filter (case-insensitive contains)" |
| `list` | `companyId` | "Filter by company ID (exact match)" |
| `list` | `language` | "Filter by language code (e.g. DE, EN)" |
| `list` | `brevo` | "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all" |
| `list` | `pageable` | "Pagination parameters (page, size)" |
| `getById` | `id` | "The contact ID" |
| `create` | — | (skip, @RequestBody) |
| `update` | `id` | "The contact ID" |
| `update` | — | (skip, @RequestBody) |
| `delete` | `id` | "The contact ID" |
| `uploadPhoto` | `id` | "The contact ID" |
| `uploadPhoto` | `file` | "The photo image file (JPEG only; max 2 MB)" |
| `getPhoto` | `id` | "The contact ID" |
| `deletePhoto` | `id` | "The contact ID" |
| `listComments` | `id` | "The contact ID" |
| `listComments` | `pageable` | "Pagination parameters (page, size)" |
| `addComment` | `id` | "The contact ID" |
| `addComment` | — | (skip, @RequestBody) |

### BrevoSyncController — 1 parameter

| Method | Parameter | Description |
|--------|-----------|-------------|
| `updateSettings` | — | (skip, @RequestBody) |

All other methods have no parameters or only `@RequestBody`.

### CommentController — 3 parameters

| Method | Parameter | Description |
|--------|-----------|-------------|
| `update` | `id` | "The comment ID" |
| `update` | — | (skip, @RequestBody) |
| `delete` | `id` | "The comment ID" |

### Example

Before:
```java
public Page<ContactDto> list(
        @RequestParam(required = false) final String firstName,
        ...
```

After:
```java
public Page<ContactDto> list(
        @Parameter(description = "Partial first name filter (case-insensitive contains)")
        @RequestParam(required = false) final String firstName,
        ...
```

### Pageable parameters

For `Pageable` parameters annotated with `@PageableDefault`, add `@Parameter` with the `hidden = true` attribute. Spring Data's springdoc integration already generates the `page`, `size`, and `sort` query parameters from `Pageable`. Adding a visible `@Parameter` would create a duplicate. Instead, the `@ParameterObject` annotation from springdoc can be used if it's not already present, or `@Parameter(hidden = true)` keeps the auto-generated docs clean.

**Rationale:** springdoc-openapi automatically expands `Pageable` into `page`, `size`, `sort` parameters with descriptions. Adding an explicit `@Parameter` on top would conflict. The best approach is to verify that springdoc handles it correctly and only intervene if the auto-generated descriptions are insufficient.

## Files Affected

| File | Change |
|------|--------|
| `backend/.../company/CompanyController.java` | Add ~11 `@Parameter` annotations |
| `backend/.../contact/ContactController.java` | Add ~13 `@Parameter` annotations |
| `backend/.../comment/CommentController.java` | Add ~2 `@Parameter` annotations |

## Regression Risk

- **None**: Pure annotation changes. No logic, no tests, no runtime behavior change.

## Open Questions

None.
