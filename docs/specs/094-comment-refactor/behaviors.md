# 094 — Behaviors

## Comment creation — Company

### Happy path
- **Given** an authenticated user and an existing company
- **When** the user calls `POST /api/companies/{companyId}/comments` with a non-blank `text`
- **Then** the response is `201 Created` with a `CommentDto` whose `id` is non-null, `text` matches the request, `author` is the lib `UserDto` of the calling user, and `createdAt`/`updatedAt` are present
- **And** a row in `comments` is inserted with `author_id` equal to the caller's user UUID
- **And** a row in `company_comments` is inserted linking that comment to `companyId`
- **And** subsequent `GET /api/companies/{companyId}` returns a `commentCount` incremented by 1

### Blank text
- **Given** an authenticated user and an existing company
- **When** the user calls `POST /api/companies/{companyId}/comments` with empty or whitespace-only `text`
- **Then** the response is `400 Bad Request`
- **And** no row is inserted into `comments` or `company_comments`

### Unknown company
- **Given** an authenticated user
- **When** the user calls `POST /api/companies/{nonExistentId}/comments` with valid `text`
- **Then** the response is `404 Not Found`
- **And** no row is inserted

### Unauthenticated request
- **Given** no authentication header
- **When** any client calls `POST /api/companies/{companyId}/comments`
- **Then** the response is `401 Unauthorized`

### Concurrent creation
- **Given** two simultaneous `POST /api/companies/{companyId}/comments` requests
- **When** both succeed
- **Then** both comments and both `company_comments` rows exist, each with a distinct `comment_id`

## Comment creation — Contact and Task

The behaviors mirror the Company case for `POST /api/contacts/{contactId}/comments` and `POST /api/tasks/{taskId}/comments`. The join row is inserted in `contact_comments` resp. `task_comments`. `commentCount` on `ContactDto` / `TaskDto` increments accordingly.

## Comment listing

### Happy path
- **Given** a company has 3 attached comments
- **When** the user calls `GET /api/companies/{companyId}/comments`
- **Then** the response is `200 OK` with a JSON array of 3 `CommentDto` objects
- **And** each entry contains a fully resolved `author: UserDto`
- **And** the response shape is a plain array (not a `Page` wrapper with `content`/`totalElements`)

### Empty
- **Given** a company has no attached comments
- **When** the user calls `GET /api/companies/{companyId}/comments`
- **Then** the response is `200 OK` with an empty array `[]`

### Unknown owner
- **Given** no company exists with the given id
- **When** the user calls `GET /api/companies/{nonExistentId}/comments`
- **Then** the response is `404 Not Found`

### Cross-owner isolation
- **Given** comment `c1` is attached to company `A` and comment `c2` is attached to company `B`
- **When** the user calls `GET /api/companies/A/comments`
- **Then** the response contains `c1` only — `c2` is not present

### SYSTEM-USER author
- **Given** a comment whose `author_id` equals the SYSTEM-USER UUID
- **When** the user calls `GET /api/companies/{companyId}/comments`
- **Then** the comment's `author` field is the SYSTEM-USER `UserDto` (`name = "System"`)

## Comment update

### Happy path
- **Given** a comment `c1` attached to company `A`
- **When** the user calls `PUT /api/companies/A/comments/c1` with new `text`
- **Then** the response is `200 OK` with the updated `CommentDto`
- **And** the `text` of `c1` in the database is updated
- **And** `updatedAt` is later than the previous value
- **And** `author_id` is unchanged

### Mismatched owner
- **Given** comment `c1` is attached to company `A`
- **When** the user calls `PUT /api/companies/B/comments/c1` (where `B` is a different company)
- **Then** the response is `404 Not Found`
- **And** `c1` is not modified

### Update across types
- **Given** comment `c1` is attached to contact `X`
- **When** the user calls `PUT /api/companies/A/comments/c1`
- **Then** the response is `404 Not Found` (the comment is not a comment of `A`)

### Unknown comment
- **Given** no comment exists with the given id
- **When** the user calls `PUT /api/companies/A/comments/{nonExistentId}`
- **Then** the response is `404 Not Found`

### Blank text
- **When** the user calls `PUT .../comments/{commentId}` with empty/whitespace `text`
- **Then** the response is `400 Bad Request`
- **And** the comment is not modified

## Comment deletion

### Happy path
- **Given** an admin user (role `ADMIN`) and a comment `c1` attached to company `A`
- **When** the admin calls `DELETE /api/companies/A/comments/c1`
- **Then** the response is `204 No Content`
- **And** the `company_comments` row for `c1` is removed
- **And** the `comments` row `c1` is removed
- **And** `commentCount` on `CompanyDto` decrements

### Non-admin
- **Given** an authenticated user **without** the `ADMIN` role
- **When** the user calls `DELETE /api/companies/A/comments/c1`
- **Then** the response is `403 Forbidden`
- **And** nothing is deleted

### Mismatched owner
- **Given** comment `c1` is attached to company `A` and the caller is admin
- **When** the admin calls `DELETE /api/companies/B/comments/c1`
- **Then** the response is `404 Not Found`
- **And** `c1` and its join row remain

### Unknown comment
- **When** an admin calls `DELETE /api/companies/A/comments/{nonExistentId}`
- **Then** the response is `404 Not Found`

## Owner deletion cascades

### Company delete
- **Given** company `A` has 3 attached comments `c1`, `c2`, `c3`
- **When** an admin calls `DELETE /api/companies/A`
- **Then** the response indicates success (per existing company-delete contract)
- **And** rows `c1`, `c2`, `c3` are removed from `comments`
- **And** all rows in `company_comments` referencing `A` are removed
- **And** no orphaned comment rows remain that previously belonged to `A`

### Contact delete
- **Given** contact `X` has 2 attached comments
- **When** the contact is deleted
- **Then** both comment rows and their `contact_comments` rows are removed

### Task delete
- **Given** task `T` has 1 attached comment
- **When** the task is deleted
- **Then** the comment row and its `task_comments` row are removed

## SYSTEM-USER

### Existence
- **Given** the application starts on a freshly-migrated database
- **When** the user table is queried for `id = '00000000-0000-0000-0000-000000000000'`
- **Then** exactly one row exists with `sub = 'system'` and `name = 'System'`

### Idempotent migration
- **Given** the SYSTEM-USER row already exists (e.g. from a previous run on a dev DB)
- **When** the migration is re-run
- **Then** no duplicate row is inserted and no error is raised

### Hidden from admin user list
- **Given** the SYSTEM-USER row exists
- **When** an admin calls the admin users endpoint (Spec 089)
- **Then** the response does **not** include SYSTEM-USER

### Resolvable via UserService
- **Given** a comment with `author_id = '00000000-0000-0000-0000-000000000000'`
- **When** the comment is returned via any `GET .../comments` endpoint
- **Then** the `author` field is fully populated with the SYSTEM-USER `UserDto`

## Migration

### Existing comments preserved
- **Given** a database with N existing comment rows (including some with `company_id`, some with `contact_id`, some with `task_id`)
- **When** Flyway runs `V30__refactor_comments.sql`
- **Then** the post-migration `comments` table contains exactly N rows
- **And** every row has `author_id = '00000000-0000-0000-0000-000000000000'`
- **And** the `comments` table has columns `id`, `text`, `author_id`, `created_at`, `updated_at` (and **no** `company_id`, `contact_id`, `task_id`, `author`)

### Join tables backfilled
- **Given** a pre-migration comment row with `company_id = X`
- **When** the migration completes
- **Then** `company_comments` contains one row with `comment_id` equal to the comment's id and `company_id = X`

### CHECK constraint removed
- **When** the migration completes
- **Then** the constraint `chk_comment_owner` no longer exists on the `comments` table

### Schema indexes
- **When** the migration completes
- **Then** indexes `idx_comments_company_id`, `idx_comments_contact_id`, `idx_comments_task_id` no longer exist
- **And** indexes `idx_company_comments_company_id`, `idx_contact_comments_contact_id`, `idx_task_comments_task_id` exist

## Owner uniqueness invariant

### Service path
- **Given** a comment is created via `POST /api/companies/{id}/comments`
- **When** the operation completes
- **Then** exactly one row exists in `company_comments` for that comment
- **And** zero rows exist in `contact_comments` or `task_comments` for that comment

### PK constraint
- **Given** a comment `c1` already exists in `company_comments` with `company_id = A`
- **When** an attempt is made to insert `(c1, B)` into `company_comments`
- **Then** the insert fails with a primary-key violation

## Endpoint removal

### `/api/comments/{id}` is gone
- **When** any client calls `PUT /api/comments/{anyId}` or `DELETE /api/comments/{anyId}`
- **Then** the response is `404 Not Found` (Spring's default for unmapped paths)

## `commentCount` on owner DTOs

### After create
- **Given** company `A` has 0 comments and `GET /api/companies/A` returns `commentCount = 0`
- **When** a comment is created via `POST /api/companies/A/comments`
- **And** the user calls `GET /api/companies/A` again
- **Then** the response has `commentCount = 1`

### After delete
- **Given** company `A` has 1 comment
- **When** an admin deletes that comment
- **And** the user calls `GET /api/companies/A`
- **Then** the response has `commentCount = 0`

### Same for Contact and Task
- The `commentCount` field on `ContactDto` and `TaskDto` follows the same behavior for their respective comment endpoints.

## Frontend

### Listing display
- **Given** a company detail page
- **When** the page loads and `GET /api/companies/{id}/comments` returns 3 comments
- **Then** all 3 comments are rendered (no pagination control)

### Author display
- **Given** a comment with `author.name = "System"` (legacy / SYSTEM-USER author)
- **When** the comment is rendered
- **Then** the displayed author is "System"

### Add comment dialog
- **Given** the AddCommentDialog is open for a company
- **When** the user submits valid text
- **Then** `createCompanyComment(companyId, { text })` is called
- **And** the new comment appears in the list without a full page reload (existing live-update behavior preserved)

### Delete comment
- **Given** an admin user on the company detail page
- **When** they click the delete button on a comment and confirm
- **Then** `deleteCompanyComment(companyId, commentId)` is called
- **And** the comment disappears from the list and `commentCount` in the heading decrements

### Update comment
- **Given** the comment edit dialog is open
- **When** the user submits new text
- **Then** `updateCompanyComment(companyId, commentId, { text })` is called
- **And** the updated text is shown in the list
