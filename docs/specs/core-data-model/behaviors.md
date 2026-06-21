# Behaviors: Core Data Model

## Company CRUD

### Create company with all fields

- **Given** no companies exist
- **When** a POST request is made to `/api/companies` with name, email, website, and full address
- **Then** the response status is 201
- **And** the response body contains the created company with a generated UUID, `deleted: false`, and timestamps

### Create company with only required fields

- **Given** no companies exist
- **When** a POST request is made to `/api/companies` with only `name`
- **Then** the response status is 201
- **And** optional fields (email, website, address) are null in the response

### Create company fails without name

- **Given** no companies exist
- **When** a POST request is made to `/api/companies` with an empty or missing `name`
- **Then** the response status is 400
- **And** the response body contains a validation error for the `name` field

### Get company by ID

- **Given** a company with ID `{id}` exists
- **When** a GET request is made to `/api/companies/{id}`
- **Then** the response status is 200
- **And** the response body contains the company data

### Get company with non-existent ID

- **Given** no company with ID `{id}` exists
- **When** a GET request is made to `/api/companies/{id}`
- **Then** the response status is 404

### Update company

- **Given** a company with ID `{id}` exists
- **When** a PUT request is made to `/api/companies/{id}` with updated fields
- **Then** the response status is 200
- **And** the response body contains the updated company data
- **And** the `updatedAt` timestamp is updated

### Update company with non-existent ID

- **Given** no company with ID `{id}` exists
- **When** a PUT request is made to `/api/companies/{id}`
- **Then** the response status is 404

### Update company fails with blank name

- **Given** a company with ID `{id}` exists
- **When** a PUT request is made to `/api/companies/{id}` with a blank `name`
- **Then** the response status is 400

## Company Soft-Delete and Restore

### Soft-delete company without contacts

- **Given** a company with ID `{id}` exists and has no associated contacts
- **When** a DELETE request is made to `/api/companies/{id}`
- **Then** the response status is 204
- **And** the company's `deleted` flag is set to true in the database
- **And** the company no longer appears in default list queries

### Soft-delete company with contacts fails

- **Given** a company with ID `{id}` exists and has associated contacts
- **When** a DELETE request is made to `/api/companies/{id}`
- **Then** the response status is 409 (Conflict)
- **And** the response body contains an error message indicating contacts must be removed first
- **And** the company's `deleted` flag remains false

### Soft-delete non-existent company

- **Given** no company with ID `{id}` exists
- **When** a DELETE request is made to `/api/companies/{id}`
- **Then** the response status is 404

### Restore soft-deleted company

- **Given** a soft-deleted company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/restore`
- **Then** the response status is 200
- **And** the company's `deleted` flag is set to false
- **And** the company appears again in default list queries

### Restore non-deleted company

- **Given** a company with ID `{id}` exists and is not deleted
- **When** a POST request is made to `/api/companies/{id}/restore`
- **Then** the response status is 200 (idempotent)
- **And** the company remains unchanged

### Restore non-existent company

- **Given** no company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/restore`
- **Then** the response status is 404

### Get soft-deleted company by ID

- **Given** a soft-deleted company with ID `{id}` exists
- **When** a GET request is made to `/api/companies/{id}`
- **Then** the response status is 200
- **And** the response body contains the company with `deleted: true`

## Company List, Pagination, Filtering, Sorting

### List companies with default pagination

- **Given** 25 companies exist
- **When** a GET request is made to `/api/companies`
- **Then** the response status is 200
- **And** the response contains 20 companies (default page size)
- **And** the response includes pagination metadata (totalElements, totalPages, page, size)

### List companies with custom page size

- **Given** 25 companies exist
- **When** a GET request is made to `/api/companies?page=0&size=10`
- **Then** the response contains 10 companies
- **And** totalElements is 25

### List companies excludes soft-deleted by default

- **Given** 3 companies exist, 1 of which is soft-deleted
- **When** a GET request is made to `/api/companies`
- **Then** the response contains 2 companies
- **And** the soft-deleted company is not in the results

### List companies includes soft-deleted with filter

- **Given** 3 companies exist, 1 of which is soft-deleted
- **When** a GET request is made to `/api/companies?includeDeleted=true`
- **Then** the response contains 3 companies

### Filter companies by name

- **Given** companies "Open Elements" and "Acme Corp" exist
- **When** a GET request is made to `/api/companies?name=open`
- **Then** the response contains only "Open Elements" (case-insensitive partial match)

### Filter companies by city

- **Given** companies in "Berlin" and "Munich" exist
- **When** a GET request is made to `/api/companies?city=Berlin`
- **Then** the response contains only the Berlin company

### Filter companies by country

- **Given** companies in "Germany" and "Austria" exist
- **When** a GET request is made to `/api/companies?country=Germany`
- **Then** the response contains only the German company

### Sort companies by name

- **Given** companies "Zebra Inc" and "Alpha GmbH" exist
- **When** a GET request is made to `/api/companies?sort=name,asc`
- **Then** "Alpha GmbH" appears before "Zebra Inc"

### Sort companies by creation date

- **Given** company A was created before company B
- **When** a GET request is made to `/api/companies?sort=createdAt,desc`
- **Then** company B appears before company A

## Contact CRUD

### Create contact with all fields

- **Given** a company with ID `{companyId}` exists
- **When** a POST request is made to `/api/contacts` with all fields including `companyId`
- **Then** the response status is 201
- **And** the response body contains the created contact with generated UUID and timestamps
- **And** `syncedToBrevo` is false and `doubleOptIn` is false

### Create contact without company

- **Given** no companies exist
- **When** a POST request is made to `/api/contacts` with firstName, lastName, and language but no companyId
- **Then** the response status is 201
- **And** `companyId` is null in the response

### Create contact with non-existent company fails

- **Given** no company with ID `{companyId}` exists
- **When** a POST request is made to `/api/contacts` with that `companyId`
- **Then** the response status is 400
- **And** the response body contains an error about invalid company reference

### Create contact with soft-deleted company fails

- **Given** a soft-deleted company with ID `{companyId}` exists
- **When** a POST request is made to `/api/contacts` with that `companyId`
- **Then** the response status is 400
- **And** the response body contains an error about invalid company reference

### Create contact fails without required fields

- **Given** no contacts exist
- **When** a POST request is made to `/api/contacts` with missing `firstName`, `lastName`, or `language`
- **Then** the response status is 400
- **And** the response body contains validation errors for the missing fields

### Create contact with null gender

- **Given** no contacts exist
- **When** a POST request is made to `/api/contacts` with `gender: null`
- **Then** the response status is 201
- **And** gender is null in the response (unknown)

### Get contact by ID

- **Given** a contact with ID `{id}` exists at company "Open Elements"
- **When** a GET request is made to `/api/contacts/{id}`
- **Then** the response status is 200
- **And** the response includes `companyId` and `companyName`

### Update contact

- **Given** a contact with ID `{id}` exists
- **When** a PUT request is made to `/api/contacts/{id}` with updated fields
- **Then** the response status is 200
- **And** the updated fields are reflected in the response
- **And** `updatedAt` is updated

### Update contact ignores Brevo fields

- **Given** a contact with ID `{id}` exists with `syncedToBrevo: false`
- **When** a PUT request is made to `/api/contacts/{id}` with a body that includes `syncedToBrevo: true`
- **Then** the response status is 200
- **And** `syncedToBrevo` remains false (field is read-only)

### Hard-delete contact

- **Given** a contact with ID `{id}` exists with associated comments
- **When** a DELETE request is made to `/api/contacts/{id}`
- **Then** the response status is 204
- **And** the contact is physically removed from the database
- **And** all associated comments are also removed (cascade)

### Delete non-existent contact

- **Given** no contact with ID `{id}` exists
- **When** a DELETE request is made to `/api/contacts/{id}`
- **Then** the response status is 404

## Contact List, Pagination, Filtering, Sorting

### List contacts with default pagination

- **Given** 25 contacts exist
- **When** a GET request is made to `/api/contacts`
- **Then** the response contains 20 contacts (default page size)
- **And** the response includes pagination metadata

### Filter contacts by last name

- **Given** contacts "Ebbers" and "Schmidt" exist
- **When** a GET request is made to `/api/contacts?lastName=ebb`
- **Then** the response contains only "Ebbers" (case-insensitive partial match)

### Filter contacts by first name

- **Given** contacts "Hendrik" and "Hans" exist
- **When** a GET request is made to `/api/contacts?firstName=Hendrik`
- **Then** the response contains only "Hendrik"

### Filter contacts by email

- **Given** contacts with emails "a@example.com" and "b@example.com" exist
- **When** a GET request is made to `/api/contacts?email=a@example`
- **Then** the response contains only the first contact

### Filter contacts by company

- **Given** contacts at company A and company B exist
- **When** a GET request is made to `/api/contacts?companyId={companyA-id}`
- **Then** the response contains only contacts of company A

### Filter contacts by language

- **Given** contacts with language DE and EN exist
- **When** a GET request is made to `/api/contacts?language=DE`
- **Then** the response contains only DE contacts

### Sort contacts by last name

- **Given** contacts "Zebra" and "Alpha" (last names) exist
- **When** a GET request is made to `/api/contacts?sort=lastName,asc`
- **Then** "Alpha" appears before "Zebra"

## Comment CRUD

### Add comment to company

- **Given** a company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/comments` with text and author
- **Then** the response status is 201
- **And** the response contains the comment with `companyId` set and `contactId` null
- **And** `createdAt` is set automatically

### Add comment to contact

- **Given** a contact with ID `{id}` exists
- **When** a POST request is made to `/api/contacts/{id}/comments` with text and author
- **Then** the response status is 201
- **And** the response contains the comment with `contactId` set and `companyId` null

### Add comment to non-existent company

- **Given** no company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/comments`
- **Then** the response status is 404

### Add comment to non-existent contact

- **Given** no contact with ID `{id}` exists
- **When** a POST request is made to `/api/contacts/{id}/comments`
- **Then** the response status is 404

### Add comment fails without text

- **Given** a company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/comments` with blank text
- **Then** the response status is 400

### Add comment fails without author

- **Given** a company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/comments` with blank author
- **Then** the response status is 400

### Add comment to soft-deleted company

- **Given** a soft-deleted company with ID `{id}` exists
- **When** a POST request is made to `/api/companies/{id}/comments` with text and author
- **Then** the response status is 201
- **And** the comment is created (comments on soft-deleted companies are allowed)

### List comments for company

- **Given** a company with ID `{id}` has 3 comments created at different times
- **When** a GET request is made to `/api/companies/{id}/comments`
- **Then** the response status is 200
- **And** the comments are sorted by `createdAt` descending (newest first)
- **And** the response includes pagination metadata

### List comments for contact

- **Given** a contact with ID `{id}` has 3 comments
- **When** a GET request is made to `/api/contacts/{id}/comments`
- **Then** the response status is 200
- **And** the comments are sorted by `createdAt` descending

### Update comment

- **Given** a comment with ID `{id}` exists
- **When** a PUT request is made to `/api/comments/{id}` with updated text
- **Then** the response status is 200
- **And** the text is updated
- **And** `updatedAt` is updated

### Update comment fails with blank text

- **Given** a comment with ID `{id}` exists
- **When** a PUT request is made to `/api/comments/{id}` with blank text
- **Then** the response status is 400

### Delete comment

- **Given** a comment with ID `{id}` exists
- **When** a DELETE request is made to `/api/comments/{id}`
- **Then** the response status is 204
- **And** the comment is physically removed from the database

### Delete non-existent comment

- **Given** no comment with ID `{id}` exists
- **When** a DELETE request is made to `/api/comments/{id}`
- **Then** the response status is 404

## Cascade and Referential Integrity

### Contact deletion cascades comments

- **Given** a contact with ID `{id}` has 5 comments
- **When** the contact is deleted via DELETE `/api/contacts/{id}`
- **Then** all 5 comments are also deleted from the database

### Company soft-delete preserves comments

- **Given** a company with ID `{id}` has 3 comments and no contacts
- **When** the company is soft-deleted via DELETE `/api/companies/{id}`
- **Then** the 3 comments still exist in the database
- **And** the comments are still accessible via GET `/api/companies/{id}/comments`

### Contact cannot reference soft-deleted company

- **Given** a soft-deleted company with ID `{companyId}` exists
- **When** a contact is created with that `companyId`
- **Then** the response status is 400

### Contact cannot be moved to soft-deleted company

- **Given** a contact exists at company A
- **And** company B is soft-deleted
- **When** the contact is updated with `companyId` pointing to company B
- **Then** the response status is 400

## OpenAPI / Swagger

### All endpoints are documented in Swagger UI

- **Given** the application is running
- **When** a GET request is made to `/v3/api-docs`
- **Then** all company, contact, and comment endpoints are listed
- **And** request/response schemas are documented
- **And** status codes are documented

## Database Migrations

### Flyway migrations run successfully

- **Given** a clean PostgreSQL database (or H2 for tests)
- **When** the application starts
- **Then** Flyway runs all migrations (V1, V2, V3) successfully
- **And** all tables (companies, contacts, comments) are created with correct columns and constraints
