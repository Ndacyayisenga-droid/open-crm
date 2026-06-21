# Behaviors: Fix Image Column Type Mismatch

## Application Startup

### Backend starts successfully with PostgreSQL

- **Given** the PostgreSQL database has migration V5 applied (columns `logo` and `photo` exist as `BYTEA`)
- **When** the backend application starts with `ddl-auto: validate`
- **Then** Hibernate schema validation passes and the application starts without errors

### Backend starts successfully with H2

- **Given** the test profile is active with H2 in-memory database and `ddl-auto: create-drop`
- **When** the backend application starts
- **Then** Hibernate creates the schema successfully including `logo` and `photo` columns

## Company Logo Operations

### Upload logo to company

- **Given** a company exists without a logo
- **When** a PNG file (< 2 MB) is uploaded via `POST /api/companies/{id}/logo`
- **Then** the logo is stored in the `logo` column and the response status is 200

### Retrieve company logo

- **Given** a company exists with an uploaded logo
- **When** `GET /api/companies/{id}/logo` is called
- **Then** the binary image data is returned with the correct `Content-Type` header

### Delete company logo

- **Given** a company exists with an uploaded logo
- **When** `DELETE /api/companies/{id}/logo` is called
- **Then** the logo is removed (set to `NULL`) and the response status is 204

### Company list shows hasLogo flag correctly

- **Given** a company exists without a logo
- **When** the company list is fetched via `GET /api/companies`
- **Then** the company DTO contains `hasLogo: false`

## Contact Photo Operations

### Upload photo to contact

- **Given** a contact exists without a photo
- **When** a JPEG file (< 2 MB) is uploaded via `POST /api/contacts/{id}/photo`
- **Then** the photo is stored in the `photo` column and the response status is 200

### Retrieve contact photo

- **Given** a contact exists with an uploaded photo
- **When** `GET /api/contacts/{id}/photo` is called
- **Then** the binary image data is returned with the correct `Content-Type` header

### Delete contact photo

- **Given** a contact exists with an uploaded photo
- **When** `DELETE /api/contacts/{id}/photo` is called
- **Then** the photo is removed (set to `NULL`) and the response status is 204

### Contact list shows hasPhoto flag correctly

- **Given** a contact exists without a photo
- **When** the contact list is fetched via `GET /api/contacts`
- **Then** the contact DTO contains `hasPhoto: false`

## Edge Cases

### Large file at exact size limit

- **Given** a company exists
- **When** a file of exactly 2 MB is uploaded as logo
- **Then** the upload succeeds

### Company and contact CRUD unaffected

- **Given** the annotation fix is applied
- **When** companies and contacts are created, read, updated, and deleted via the existing API
- **Then** all CRUD operations work as before — no regression in existing functionality

### Existing NULL image values remain valid

- **Given** companies and contacts exist with `logo`/`photo` set to `NULL`
- **When** the application starts after the fix
- **Then** all existing records are accessible and the `hasLogo`/`hasPhoto` flags are `false`