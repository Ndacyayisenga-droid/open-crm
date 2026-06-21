# Behaviors: Tag Count Columns & Filter Navigation

## Tag List — Count Columns

### Count displays correct number of active companies

- **Given** a tag assigned to 3 companies (2 active, 1 soft-deleted)
- **When** the tag list is loaded
- **Then** the "Firmen" column shows "2" for that tag

### Count displays correct number of contacts

- **Given** a tag assigned to 5 contacts
- **When** the tag list is loaded
- **Then** the "Kontakte" column shows "5" for that tag

### Count displays zero when tag is unused

- **Given** a tag assigned to no companies and no contacts
- **When** the tag list is loaded
- **Then** the "Firmen" column shows "0" and the "Kontakte" column shows "0"

### Navigation icon visible when count is greater than zero

- **Given** a tag with companyCount = 3
- **When** the tag list is rendered
- **Then** an arrow/link icon is shown next to the company count

### Navigation icon hidden when count is zero

- **Given** a tag with companyCount = 0
- **When** the tag list is rendered
- **Then** no arrow/link icon is shown next to the company count

### Navigation icon links to filtered company list

- **Given** a tag with id `abc-123` and companyCount = 2
- **When** the user clicks the arrow icon in the "Firmen" column
- **Then** the browser navigates to `/companies?tagIds=abc-123`

### Navigation icon links to filtered contact list

- **Given** a tag with id `abc-123` and contactCount = 4
- **When** the user clicks the arrow icon in the "Kontakte" column
- **Then** the browser navigates to `/contacts?tagIds=abc-123`

### Counts columns always visible on mobile

- **Given** a mobile viewport
- **When** the tag list is rendered
- **Then** the "Firmen" and "Kontakte" columns are visible (not hidden like Description)

## Tag List — Backend Count API

### includeCounts=false returns null counts

- **Given** tags exist in the database
- **When** `GET /api/tags` is called without `includeCounts` parameter
- **Then** each tag in the response has `companyCount: null` and `contactCount: null`

### includeCounts=true returns computed counts

- **Given** Tag A is assigned to 2 active companies and 3 contacts
- **When** `GET /api/tags?includeCounts=true` is called
- **Then** Tag A in the response has `companyCount: 2` and `contactCount: 3`

### Counts exclude soft-deleted companies

- **Given** Tag A is assigned to 3 companies, 1 of which is soft-deleted
- **When** `GET /api/tags?includeCounts=true` is called
- **Then** Tag A has `companyCount: 2`

### Single tag endpoint does not include counts

- **Given** Tag A exists
- **When** `GET /api/tags/{id}` is called
- **Then** the response has `companyCount: null` and `contactCount: null`

## Company List — Tag Filter

### Filter by single tag

- **Given** 5 companies exist, 2 of which have Tag A
- **When** the company list is loaded with `?tagIds={tagA-id}`
- **Then** only the 2 companies with Tag A are shown

### Filter by multiple tags with AND semantics

- **Given** Company X has Tag A and Tag B, Company Y has only Tag A
- **When** the company list is loaded with `?tagIds={tagA-id}&tagIds={tagB-id}`
- **Then** only Company X is shown (has both tags)

### Tag filter combines with name filter

- **Given** Company "Acme" has Tag A, Company "Beta" has Tag A
- **When** the company list is filtered with tagIds=Tag A and name="Acme"
- **Then** only Company "Acme" is shown

### Tag filter combines with Brevo filter

- **Given** Company X has Tag A and is from Brevo, Company Y has Tag A and is not from Brevo
- **When** the company list is filtered with tagIds=Tag A and brevo=true
- **Then** only Company X is shown

### Tag filter respects archive toggle

- **Given** Company X has Tag A and is active, Company Y has Tag A and is soft-deleted
- **When** the company list is filtered with tagIds=Tag A and includeDeleted=false
- **Then** only Company X is shown

### Tag filter with non-existent tag ID returns empty results

- **Given** no tags with the given ID exist
- **When** the company list is loaded with `?tagIds={non-existent-id}`
- **Then** an empty list is returned (no error)

### Page resets when tag filter changes

- **Given** the user is on page 3 of the company list
- **When** the user selects a tag in the filter
- **Then** the page resets to page 1

### TagMultiSelect shows selected tags from URL

- **Given** the company list is opened with `?tagIds={tagA-id}`
- **When** the page renders
- **Then** the TagMultiSelect filter shows Tag A as selected

## Contact List — Tag Filter

### Filter by single tag

- **Given** 5 contacts exist, 3 of which have Tag B
- **When** the contact list is loaded with `?tagIds={tagB-id}`
- **Then** only the 3 contacts with Tag B are shown

### Filter by multiple tags with AND semantics

- **Given** Contact X has Tag A and Tag B, Contact Y has only Tag A
- **When** the contact list is loaded with `?tagIds={tagA-id}&tagIds={tagB-id}`
- **Then** only Contact X is shown

### Tag filter combines with search filter

- **Given** Contact "John Doe" has Tag A, Contact "Jane Smith" has Tag A
- **When** the contact list is filtered with tagIds=Tag A and search="John"
- **Then** only Contact "John Doe" is shown

### Tag filter combines with company filter

- **Given** Contact X has Tag A and belongs to Company Z, Contact Y has Tag A and has no company
- **When** the contact list is filtered with tagIds=Tag A and companyId={companyZ-id}
- **Then** only Contact X is shown

### Tag filter combines with language filter

- **Given** Contact X has Tag A and language=DE, Contact Y has Tag A and language=EN
- **When** the contact list is filtered with tagIds=Tag A and language=DE
- **Then** only Contact X is shown

### Page resets when tag filter changes

- **Given** the user is on page 2 of the contact list
- **When** the user selects a tag in the filter
- **Then** the page resets to page 1

### TagMultiSelect shows selected tags from URL

- **Given** the contact list is opened with `?tagIds={tagB-id}`
- **When** the page renders
- **Then** the TagMultiSelect filter shows Tag B as selected

## Edge Cases

### Tag deleted while assigned to entities

- **Given** Tag A is assigned to 3 companies
- **When** Tag A is deleted
- **Then** Tag A no longer appears in the tag list and the company-tag associations are removed

### Empty tag list

- **Given** no tags exist
- **When** the tag list is loaded with `includeCounts=true`
- **Then** the empty state is shown (no errors from count computation)

### Tag assigned to both companies and contacts

- **Given** Tag A is assigned to 2 companies and 4 contacts
- **When** the tag list is loaded
- **Then** "Firmen" shows "2" and "Kontakte" shows "4" independently

### URL with invalid tag ID

- **Given** the company list is opened with `?tagIds=not-a-valid-uuid`
- **When** the backend receives the request
- **Then** a 400 Bad Request is returned (Spring UUID parsing)
