# Behaviors: DTO Refactoring

## Build Integrity

### Project compiles after rename

- **Given** all DTOs have been renamed from `*Response`/`*Request` to `*Dto`/`*CreateDto`/`*UpdateDto`
- **When** `./mvnw clean compile` is executed
- **Then** the build succeeds without errors

### All existing tests pass after rename

- **Given** all DTO references in services, controllers, and tests are updated
- **When** `./mvnw clean verify` is executed
- **Then** all tests pass without failures

## OpenAPI Schema

### Required properties are marked in company create schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `CompanyCreateDto`
- **Then** `name` is listed as required
- **And** `email`, `website`, `street`, `houseNumber`, `zipCode`, `city`, `country` are not required

### Required properties are marked in contact create schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `ContactCreateDto`
- **Then** `firstName`, `lastName`, `language` are listed as required
- **And** `email`, `position`, `gender`, `linkedInUrl`, `phoneNumber`, `companyId` are not required

### Required properties are marked in comment create schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `CommentCreateDto`
- **Then** `text` and `author` are listed as required

### Required properties are marked in company response schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `CompanyDto`
- **Then** `id`, `name`, `deleted`, `createdAt`, `updatedAt` are listed as required

### Required properties are marked in contact response schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `ContactDto`
- **Then** `id`, `firstName`, `lastName`, `syncedToBrevo`, `doubleOptIn`, `language`, `createdAt`, `updatedAt` are listed as required

### Required properties are marked in comment response schema

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `CommentDto`
- **Then** `id`, `text`, `author`, `createdAt`, `updatedAt` are listed as required

### Health DTO schema is correct

- **Given** the application is running
- **When** the OpenAPI spec at `/v3/api-docs` is inspected for `HealthDto`
- **Then** `status` is listed as required

## API Contract Unchanged

### JSON field names are unchanged

- **Given** the DTOs have been renamed
- **When** any API endpoint is called
- **Then** the JSON field names in request and response bodies are identical to before the refactoring
