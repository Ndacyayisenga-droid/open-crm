# Behaviors: spring-services 0.14 upgrade

## Flyway migration V32

### Fresh database migration succeeds

- **Given** an empty database with no prior migrations applied
- **When** the application starts and Flyway runs all migrations through V32
- **Then** the `audit_log` table has a `user_id UUID NOT NULL` column with a foreign key to `users(id)` and no `user_name` column
- **And** the `comments` table has an `author_id UUID NOT NULL` column with a foreign key to `users(id)` and no `author` column
- **And** the System User row exists in `users` (inserted by V30, not deleted by V32)

### Existing database migration preserves all rows

- **Given** a database at migration V31 with N existing `audit_log` rows (mixture of named users and the literal string `"System"`) and M existing `comments` rows (`author` already stores UUID strings, all set to System User by V30)
- **When** V32 is applied
- **Then** the `audit_log` table contains exactly N rows after the migration
- **And** every row's `user_id` equals `SystemUser.ID` (`00000000-0000-0000-0000-000000000000`)
- **And** the `comments` table contains exactly M rows after the migration
- **And** every row's `author_id` matches the UUID it previously stored as text

### NULL user_name rows are not lost

- **Given** an existing `audit_log` row with `user_name IS NULL`
- **When** V32 is applied
- **Then** the row survives the migration and its `user_id` is `SystemUser.ID`

### Migration fails fast on missing System User

- **Given** the System User row has been deleted manually before V32 runs
- **When** V32 attempts to add the foreign key constraint
- **Then** the migration fails with a foreign key violation and no partial schema change persists

## AuditLogDataService API

### Filtering by user UUID returns matching entries

- **Given** three audit entries — two for user Alice, one for user Bob
- **When** `findByUser(aliceId, pageable)` is called
- **Then** the result contains exactly two entries, both with `user.id == aliceId`

### Filtering by entity type and user UUID combines both predicates

- **Given** entries: `CompanyDto/Alice`, `ContactDto/Alice`, `CompanyDto/Bob`
- **When** `findByEntityTypeAndUser("CompanyDto", aliceId, pageable)` is called
- **Then** the result contains exactly one entry — `CompanyDto/Alice`

### createEntry persists the user association

- **Given** a managed `UserEntity` for Alice
- **When** `createEntry("CompanyDto", someUuid, INSERT, alice)` is called
- **Then** a new `audit_log` row exists with `user_id == alice.id`
- **And** the returned DTO has `user.id == alice.id` and `user.name == "Alice"`

## GET /api/audit-logs endpoint

### Returns audit log DTOs with embedded user object

- **Given** an audit entry created by Alice
- **When** an IT-ADMIN performs `GET /api/audit-logs`
- **Then** the response is HTTP 200 with `content[0].user` as a nested object containing `id`, `name`, `email`, `avatarUrl`
- **And** `content[0].user.id` equals Alice's UUID

### Filtering by user UUID via query parameter

- **Given** entries from Alice and Bob
- **When** `GET /api/audit-logs?user=<aliceUuid>` is called
- **Then** only Alice's entries are returned
- **And** every returned entry has `user.id == aliceUuid`

### Filtering by entity type and user UUID

- **Given** entries: `CompanyDto/Alice`, `ContactDto/Alice`, `CompanyDto/Bob`
- **When** `GET /api/audit-logs?entityType=CompanyDto&user=<aliceUuid>` is called
- **Then** the response contains exactly one entry with `entityType == "CompanyDto"` and `user.id == aliceUuid`

### Invalid UUID in user parameter returns 400

- **Given** an authenticated IT-ADMIN
- **When** `GET /api/audit-logs?user=not-a-uuid` is called
- **Then** the response is HTTP 400 (Spring's `MethodArgumentTypeMismatchException`)

### Unknown UUID in user parameter returns empty page

- **Given** no audit entries exist for UUID `X`
- **When** `GET /api/audit-logs?user=X` is called
- **Then** the response is HTTP 200 with `content == []` and `page.totalElements == 0`

### Sorting by createdAt descending is preserved

- **Given** three audit entries created in order A, B, C
- **When** `GET /api/audit-logs` is called
- **Then** the response order is C, B, A (newest first)

## CommentEntity author association

### Comment creation captures the current user as author

- **Given** an authenticated user
- **When** the user creates a comment via `POST /api/companies/{id}/comments`
- **Then** the persisted `CommentEntity.author` references the current `UserEntity`
- **And** the response `CommentDto.author` mirrors that user's `UserDto`

### Reading a comment with System User author

- **Given** a comment whose `author_id` is `SystemUser.ID`
- **When** the comment is fetched via `GET /api/companies/{id}/comments`
- **Then** the response `author.name` is `"System"`

## Frontend audit log view

### User dropdown sends UUID, not name

- **Given** the audit log admin page is loaded with three users in the dropdown
- **When** the admin selects "Alice" from the user filter
- **Then** the request to `/api/audit-logs` includes `user=<alice-uuid>` as the query parameter

### Table renders nested user.name

- **Given** the API returns audit entries with `user` as a `UserDto` object
- **When** the table is rendered
- **Then** the user column shows `entry.user.name` (not `[object Object]`)

### System User is not in the dropdown

- **Given** the admin loads the audit log page
- **When** the user dropdown options are populated from `/api/users`
- **Then** the System User does not appear (because `/api/users` excludes it server-side)
- **And** no client-side hard-coded `"System"` filter is required

### Dropdown empty state

- **Given** `/api/users` returns an empty page
- **When** the user dropdown is opened
- **Then** only the "All users" option is visible and the picker does not crash

## SystemUser deduplication

### Local SystemUser class is gone

- **Given** the post-upgrade codebase
- **When** a developer searches for `com.openelements.crm.user.SystemUser`
- **Then** no matches are found
- **And** `UserController` imports `com.openelements.spring.base.security.user.SystemUser`

### UserController still hides the System User

- **Given** the System User row exists and three other users exist
- **When** an IT-ADMIN performs `GET /api/users`
- **Then** the response contains exactly three users and does not include the System User

## Backwards compatibility check

### Existing audit entries are queryable after migration

- **Given** the database had audit entries before the upgrade
- **When** the upgraded application starts and an IT-ADMIN visits `/admin/audit-logs`
- **Then** every pre-migration row is visible
- **And** every pre-migration row shows "System" as the user
