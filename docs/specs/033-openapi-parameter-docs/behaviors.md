# Behaviors: Add OpenAPI @Parameter Annotations to All REST Controllers

## Swagger UI — Company Endpoints

### Company list parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `GET /api/companies`
- **Then** the `name` parameter shows description "Partial company name filter (case-insensitive contains)"
- **And** the `includeDeleted` parameter shows description "Whether to include soft-deleted companies"
- **And** the `brevo` parameter shows description "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all"

### Company path parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `GET /api/companies/{id}`
- **Then** the `id` parameter shows description "The company ID"

### Company logo upload parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `POST /api/companies/{id}/logo`
- **Then** the `id` parameter shows description "The company ID"
- **And** the `file` parameter shows description "The logo image file (JPEG, PNG, or SVG; max 2 MB)"

## Swagger UI — Contact Endpoints

### Contact list parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `GET /api/contacts`
- **Then** the `firstName` parameter shows description "Partial first name filter (case-insensitive contains)"
- **And** the `lastName` parameter shows description "Partial last name filter (case-insensitive contains)"
- **And** the `email` parameter shows description "Partial email filter (case-insensitive contains)"
- **And** the `companyId` parameter shows description "Filter by company ID (exact match)"
- **And** the `language` parameter shows description "Filter by language code (e.g. DE, EN)"
- **And** the `brevo` parameter shows description "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all"

### Contact path parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `DELETE /api/contacts/{id}`
- **Then** the `id` parameter shows description "The contact ID"

### Contact photo upload parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `POST /api/contacts/{id}/photo`
- **Then** the `file` parameter shows description "The photo image file (JPEG only; max 2 MB)"

## Swagger UI — Comment Endpoints

### Comment path parameters have descriptions

- **Given** the user opens Swagger UI
- **When** they expand `PUT /api/comments/{id}`
- **Then** the `id` parameter shows description "The comment ID"

## No Behavioral Change

### API behavior is unchanged

- **Given** the `@Parameter` annotations are added
- **When** any API endpoint is called with valid parameters
- **Then** the response is identical to before the annotation change
