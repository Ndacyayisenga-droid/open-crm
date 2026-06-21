# Behaviors: Read-Only API Key Authentication

## API Key Creation

### Create API key with valid name

- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `POST /api/api-keys` with body `{"name": "CI Pipeline"}`
- **Then** the response status is 201 Created
- **Then** the response contains `id`, `name`, `keyPrefix`, `key`, `createdBy`, `createdAt`
- **Then** `name` equals `"CI Pipeline"`
- **Then** `key` starts with `crm_` and has 52 characters total
- **Then** `keyPrefix` shows the first 8 and last 4 characters of the key (e.g. `crm_a1B2...w3X4`)
- **Then** `createdBy` equals the current user's display name

### Create API key without name

- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `POST /api/api-keys` with body `{"name": ""}`
- **Then** the response status is 400 Bad Request

### Create API key with null name

- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `POST /api/api-keys` with body `{}`
- **Then** the response status is 400 Bad Request

### Create API key with name exceeding max length

- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `POST /api/api-keys` with a name of 256 characters
- **Then** the response status is 400 Bad Request

### Create API key without authentication

- **Given** no authentication is provided
- **When** a request is sent to `POST /api/api-keys` with body `{"name": "Test"}`
- **Then** the response status is 401 Unauthorized

### Raw key is unique per creation

- **Given** a user is authenticated via OIDC/JWT
- **When** the user creates two API keys with the same name
- **Then** both succeed with status 201
- **Then** each response contains a different `key` value
- **Then** each response contains a different `id`

## API Key Listing

### List API keys with pagination

- **Given** 25 API keys exist in the system
- **When** a request is sent to `GET /api/api-keys?page=0&size=10`
- **Then** the response status is 200 OK
- **Then** the response contains 10 items in `content`
- **Then** `page.totalElements` equals 25
- **Then** `page.totalPages` equals 3

### List API keys returns no raw key

- **Given** an API key exists with name "CI Pipeline"
- **When** a request is sent to `GET /api/api-keys`
- **Then** the response status is 200 OK
- **Then** each item in `content` contains `id`, `name`, `keyPrefix`, `createdBy`, `createdAt`
- **Then** no item in `content` contains a `key` field

### List API keys when none exist

- **Given** no API keys exist
- **When** a request is sent to `GET /api/api-keys`
- **Then** the response status is 200 OK
- **Then** `content` is an empty array
- **Then** `page.totalElements` equals 0

### List API keys default sort order

- **Given** API key "Alpha" was created first and "Beta" was created second
- **When** a request is sent to `GET /api/api-keys` without sort parameter
- **Then** "Beta" appears before "Alpha" (default sort: `createdAt,desc`)

### List API keys via API key authentication

- **Given** an API key exists and is valid
- **When** a `GET /api/api-keys` request is sent with `X-API-Key` header
- **Then** the response status is 200 OK
- **Then** the list of API keys is returned

## API Key Deletion

### Delete existing API key

- **Given** an API key exists with a known ID
- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `DELETE /api/api-keys/{id}`
- **Then** the response status is 204 No Content
- **Then** the key no longer appears in the list

### Delete non-existent API key

- **Given** no API key exists with ID `00000000-0000-0000-0000-000000000000`
- **Given** a user is authenticated via OIDC/JWT
- **When** the user sends `DELETE /api/api-keys/00000000-0000-0000-0000-000000000000`
- **Then** the response status is 404 Not Found

### Delete API key via API key authentication

- **Given** an API key exists
- **When** a `DELETE /api/api-keys/{id}` request is sent with `X-API-Key` header (not JWT)
- **Then** the response status is 403 Forbidden (API keys are read-only)

### Deleted key stops working immediately

- **Given** an API key exists and is valid
- **Given** the key is deleted via `DELETE /api/api-keys/{id}`
- **When** a `GET /api/companies` request is sent with the deleted key in `X-API-Key` header
- **Then** the response status is 401 Unauthorized

## API Key Authentication — Read Access

### Valid API key on GET endpoint

- **Given** an API key was created and the raw key is known
- **When** a `GET /api/companies` request is sent with header `X-API-Key: <raw-key>`
- **Then** the response status is 200 OK
- **Then** the response contains the list of companies

### Valid API key on GET endpoint with path parameter

- **Given** an API key exists and a company with a known ID exists
- **When** a `GET /api/companies/{id}` request is sent with header `X-API-Key: <raw-key>`
- **Then** the response status is 200 OK
- **Then** the response contains the company details

### Valid API key on GET endpoint with query parameters

- **Given** an API key exists
- **When** a `GET /api/contacts?page=0&size=5` request is sent with header `X-API-Key: <raw-key>`
- **Then** the response status is 200 OK
- **Then** the response is paginated with size 5

### Valid API key on health endpoint

- **Given** an API key exists
- **When** a `GET /api/health` request is sent with header `X-API-Key: <raw-key>`
- **Then** the response status is 200 OK (health is public, key is ignored)

## API Key Authentication — Write Rejection

### Valid API key on POST endpoint

- **Given** an API key exists
- **When** a `POST /api/companies` request is sent with header `X-API-Key: <raw-key>` and a valid body
- **Then** the response status is 403 Forbidden
- **Then** the response body indicates API keys only grant read-only access

### Valid API key on PUT endpoint

- **Given** an API key exists and a company with a known ID exists
- **When** a `PUT /api/companies/{id}` request is sent with header `X-API-Key: <raw-key>` and a valid body
- **Then** the response status is 403 Forbidden

### Valid API key on DELETE endpoint

- **Given** an API key exists and a company with a known ID exists
- **When** a `DELETE /api/companies/{id}` request is sent with header `X-API-Key: <raw-key>`
- **Then** the response status is 403 Forbidden

## API Key Authentication — Invalid Keys

### Invalid API key

- **Given** no API key with value `crm_invalid000000000000000000000000000000000000000000` exists
- **When** a `GET /api/companies` request is sent with header `X-API-Key: crm_invalid000000000000000000000000000000000000000000`
- **Then** the response status is 401 Unauthorized

### Empty API key header

- **Given** the `X-API-Key` header is present but empty
- **When** a `GET /api/companies` request is sent
- **Then** the response status is 401 Unauthorized

### No API key header falls through to JWT

- **Given** no `X-API-Key` header is present
- **Given** no `Authorization` header is present
- **When** a `GET /api/companies` request is sent
- **Then** the response status is 401 Unauthorized (standard JWT auth failure)

### Both API key and JWT header present

- **Given** a valid API key exists
- **Given** both `X-API-Key` and `Authorization: Bearer <jwt>` headers are present
- **When** a `GET /api/companies` request is sent
- **Then** the API key takes precedence (filter runs first)
- **Then** the response status is 200 OK

## API Key Security

### Key hash is stored, not raw key

- **Given** an API key is created
- **When** the `api_keys` table is queried directly
- **Then** the `key_hash` column contains a 64-character hexadecimal string (SHA-256)
- **Then** no column contains the raw key value

### Different raw keys produce different hashes

- **Given** two API keys are created
- **When** both are persisted
- **Then** each has a unique `key_hash` value

## Frontend — API Key List Page

### Display API keys in table

- **Given** API keys exist in the system
- **When** the user navigates to the API Keys page
- **Then** a table is shown with columns: Name, Key, Created By, Created, Actions
- **Then** the Key column shows the stored prefix (e.g. `crm_a1B2...w3X4`)

### Empty state

- **Given** no API keys exist
- **When** the user navigates to the API Keys page
- **Then** an empty state is shown with an icon and a "Create API Key" button

### Sidebar navigation

- **Given** a user is logged in
- **When** the sidebar is visible
- **Then** an "API Keys" item is shown in the bottom section (alongside Admin and Webhooks)
- **Then** clicking it navigates to the API Keys page

## Frontend — Create API Key

### Create dialog opens and submits

- **Given** the user is on the API Keys page
- **When** the user clicks "New API Key"
- **Then** a dialog opens with a name input field
- **When** the user enters "CI Pipeline" and clicks "Create"
- **Then** the create dialog closes
- **Then** a second dialog opens showing the full raw key

### Key created dialog with copy

- **Given** the "Key Created" dialog is open with a raw key displayed
- **When** the user clicks "Copy"
- **Then** the raw key is copied to the clipboard
- **Then** the button text changes to "Copied!"

### Key created dialog warning

- **Given** the "Key Created" dialog is open
- **Then** a warning is displayed: "Copy this key now. It will not be shown again."

### Key created dialog close refreshes list

- **Given** the "Key Created" dialog is open after creating a key named "CI Pipeline"
- **When** the user closes the dialog
- **Then** the table refreshes and shows the new key with name "CI Pipeline"

### Create dialog validation

- **Given** the create dialog is open
- **When** the user clicks "Create" without entering a name
- **Then** an error message is displayed
- **Then** the dialog stays open

## Frontend — Delete API Key

### Delete with confirmation

- **Given** an API key "CI Pipeline" exists in the table
- **When** the user clicks the delete button on that row
- **Then** a confirmation dialog opens with a warning about immediate impact
- **When** the user confirms deletion
- **Then** the key is removed from the table

### Cancel delete

- **Given** the delete confirmation dialog is open
- **When** the user clicks "Cancel"
- **Then** the dialog closes
- **Then** the key remains in the table
