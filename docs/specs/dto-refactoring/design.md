# Design: DTO Refactoring

## GitHub Issue

_To be created._

## Summary

Rename all DTO records from `*Response`/`*CreateRequest`/`*UpdateRequest` to `*Dto`/`*CreateDto`/`*UpdateDto` for a consistent naming convention. Additionally, improve OpenAPI schema annotations by adding `requiredMode = Schema.RequiredMode.REQUIRED` on all mandatory properties so that Swagger UI clearly indicates which fields are required.

This is a pure refactoring — no behavior changes, no new features, no database changes.

## Goals

- Consistent DTO naming convention using `*Dto` suffix
- OpenAPI schema accurately reflects required vs. optional properties
- All existing tests continue to pass without behavior changes

## Non-goals

- Changing any business logic or validation rules
- Adding new fields or endpoints
- Changing the API contract (JSON field names stay the same)

## Technical Approach

### Rename DTOs

| Current Name | New Name |
|---|---|
| `CompanyCreateRequest` | `CompanyCreateDto` |
| `CompanyUpdateRequest` | `CompanyUpdateDto` |
| `CompanyResponse` | `CompanyDto` |
| `ContactCreateRequest` | `ContactCreateDto` |
| `ContactUpdateRequest` | `ContactUpdateDto` |
| `ContactResponse` | `ContactDto` |
| `CommentCreateRequest` | `CommentCreateDto` |
| `CommentUpdateRequest` | `CommentUpdateDto` |
| `CommentResponse` | `CommentDto` |
| `HealthResponse` | `HealthDto` |

**Rationale:** `*Dto` is shorter and more consistent. The direction (request vs. response) is already clear from `Create`/`Update` prefix (input) vs. no prefix (output).

### Add `requiredMode` to `@Schema` Annotations

For every property annotated with `@NotBlank`, `@NotNull`, or that is a non-nullable primitive (like `boolean`), add `requiredMode = Schema.RequiredMode.REQUIRED` to the `@Schema` annotation.

For response DTOs (`*Dto`), properties that are always present (id, name, timestamps, booleans) should also be marked `requiredMode = REQUIRED`.

Optional/nullable properties keep the default (`NOT_REQUIRED`).

### Affected Files

**Rename + update schema:**
- `company/CompanyCreateRequest.java` → `CompanyCreateDto.java`
- `company/CompanyUpdateRequest.java` → `CompanyUpdateDto.java`
- `company/CompanyResponse.java` → `CompanyDto.java`
- `contact/ContactCreateRequest.java` → `ContactCreateDto.java`
- `contact/ContactUpdateRequest.java` → `ContactUpdateDto.java`
- `contact/ContactResponse.java` → `ContactDto.java`
- `comment/CommentCreateRequest.java` → `CommentCreateDto.java`
- `comment/CommentUpdateRequest.java` → `CommentUpdateDto.java`
- `comment/CommentResponse.java` → `CommentDto.java`
- `health/HealthResponse.java` → `HealthDto.java`

**Update references:**
- `company/CompanyService.java`
- `company/CompanyController.java`
- `contact/ContactService.java`
- `contact/ContactController.java`
- `comment/CommentService.java`
- `comment/CommentController.java`
- `health/HealthController.java`

**Update test references:**
- All test files referencing old DTO names

## Open Questions

None — this is a straightforward mechanical refactoring.
