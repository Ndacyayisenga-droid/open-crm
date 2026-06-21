# Behaviors: Updates View

## Endpoint authorization

### Unauthenticated request is rejected

- **Given** no OIDC session is present
- **When** a client calls `GET /api/updates`
- **Then** the server responds with HTTP 401

### Any authenticated user can read updates

- **Given** the caller is authenticated with a regular (non-admin) user
- **When** the client calls `GET /api/updates`
- **Then** the server responds with HTTP 200 and a `Page<UpdateEntryDto>`

### IT-ADMIN user can also read updates

- **Given** the caller has role `ROLE_IT_ADMIN`
- **When** the client calls `GET /api/updates`
- **Then** the server responds with HTTP 200 (no additional permissions required)

## Page-size validation

### Default size is 20

- **Given** no `size` parameter is provided
- **When** the client calls `GET /api/updates`
- **Then** the response `page.size` equals 20
- **And** `page.totalPages` equals 1

### Size 50, 100 and 200 are accepted

- **Given** a `size` query parameter of `50`, `100` or `200`
- **When** the client calls `GET /api/updates?size={value}`
- **Then** the response `page.size` equals the requested value
- **And** `content.length` is at most the requested value

### Disallowed size is rejected

- **Given** a `size` query parameter that is not in `{20, 50, 100, 200}` (e.g. `17`,
  `0`, `-5`, `1000`)
- **When** the client calls `GET /api/updates?size={value}`
- **Then** the server responds with HTTP 400

### Page parameter is ignored beyond page 0

- **Given** the underlying audit log has any number of relevant entries
- **When** the client calls `GET /api/updates?size=20&page=2`
- **Then** the response `content` is an empty array
- **And** no error is returned

## Company events

### Company created

- **Given** a user creates a new company "Open Elements GmbH"
- **When** another user calls `GET /api/updates`
- **Then** the newest entry has `type = COMPANY_CREATED`
- **And** `entityId` equals the new company's UUID
- **And** `entityName` equals `"Open Elements GmbH"`
- **And** `user` is the creating user

### Company updated

- **Given** an existing company is updated (any field)
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = COMPANY_UPDATED` appears
- **And** `entityId` equals the company's UUID
- **And** `entityName` equals the company's current name

### Company deleted

- **Given** a company is deleted
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = COMPANY_DELETED` appears
- **And** `entityId` is `null`
- **And** `entityName` is `null`

### Renamed company shows current name in older entries

- **Given** a company was created as "ACME" and later renamed to "ACME Corp"
- **And** both create and rename are visible in the updates feed
- **When** the client calls `GET /api/updates`
- **Then** the `COMPANY_CREATED` entry's `entityName` equals `"ACME Corp"` (current name)
- **And** the `COMPANY_UPDATED` entry's `entityName` equals `"ACME Corp"`

## Contact events

### Contact created

- **Given** a user creates a new contact "John Doe"
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_CREATED` appears
- **And** `entityId` equals the new contact's UUID
- **And** `entityName` equals the contact's display name (`"John Doe"`)

### Contact updated

- **Given** an existing contact is updated
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_UPDATED` appears
- **And** `entityName` reflects the contact's current display name

### Contact deleted

- **Given** a contact is deleted
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_DELETED` appears
- **And** `entityId` is `null`
- **And** `entityName` is `null`

## Comment events — company

### Comment added to a company

- **Given** a user adds a comment to company "Open Elements GmbH"
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = COMPANY_COMMENT_CREATED` appears
- **And** `entityId` equals the company's UUID (not the comment's UUID)
- **And** `entityName` equals `"Open Elements GmbH"`

### Comment on a company is updated

- **Given** a user updates an existing comment on a company
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = COMPANY_COMMENT_UPDATED` appears
- **And** `entityId` equals the parent company's UUID
- **And** `entityName` equals the parent company's name

### Comment on a company is deleted

- **Given** a user deletes a comment on a company that still exists
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = COMPANY_COMMENT_DELETED` appears
- **And** `entityId` equals the parent company's UUID
- **And** `entityName` equals the parent company's name

## Comment events — contact

### Comment added to a contact

- **Given** a user adds a comment to contact "John Doe"
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_COMMENT_CREATED` appears
- **And** `entityId` equals the contact's UUID
- **And** `entityName` equals the contact's display name

### Comment on a contact is updated

- **Given** a user updates a comment on a contact
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_COMMENT_UPDATED` appears

### Comment on a contact is deleted

- **Given** a user deletes a comment on a contact that still exists
- **When** the client calls `GET /api/updates`
- **Then** an entry with `type = CONTACT_COMMENT_DELETED` appears
- **And** `entityId` equals the parent contact's UUID
- **And** `entityName` equals the contact's display name

## Dedupe of consecutive UPDATEs

### Two consecutive UPDATEs on the same company by the same user are merged

- **Given** user U updates company C, then immediately updates company C again
- **And** no other update on company C by a different user lies between them
- **When** the client calls `GET /api/updates`
- **Then** exactly one entry with `type = COMPANY_UPDATED` for company C by user U
  appears
- **And** its `createdAt` equals the timestamp of the **second** (latest) update
- **And** its `id` equals the audit-log id of the second update

### Consecutive UPDATEs by different users are not merged

- **Given** user U1 updates company C, then user U2 updates company C
- **When** the client calls `GET /api/updates`
- **Then** two separate `COMPANY_UPDATED` entries appear (one per user)

### Consecutive UPDATEs on different companies by the same user are not merged

- **Given** user U updates company C1, then updates company C2
- **When** the client calls `GET /api/updates`
- **Then** two separate `COMPANY_UPDATED` entries appear (one per company)

### CREATE followed by UPDATE on the same company by the same user is not merged

- **Given** user U creates company C, then immediately updates company C
- **When** the client calls `GET /api/updates`
- **Then** both `COMPANY_CREATED` and `COMPANY_UPDATED` entries appear

### UPDATE followed by DELETE on the same company by the same user is not merged

- **Given** user U updates company C, then deletes company C
- **When** the client calls `GET /api/updates`
- **Then** both `COMPANY_UPDATED` and `COMPANY_DELETED` entries appear

### Dedupe applies to contacts identically

- **Given** user U updates contact P, then immediately updates contact P again
- **When** the client calls `GET /api/updates`
- **Then** exactly one `CONTACT_UPDATED` entry appears

### Dedupe applies to comment UPDATEs

- **Given** user U updates a comment on company C, then immediately updates another
  comment on the same company C
- **When** the client calls `GET /api/updates`
- **Then** exactly one `COMPANY_COMMENT_UPDATED` entry appears (parent ID is the
  same; this collapse is intentional)

### Dedupe skips filtered entries between candidates

- **Given** user U updates company C, then performs an action on an out-of-scope entity
  (e.g. webhook), then updates company C again
- **When** the client calls `GET /api/updates`
- **Then** exactly one `COMPANY_UPDATED` entry for company C by user U appears
  (filter removes the webhook event, the two UPDATEs become adjacent)

## Sorting and ordering

### Entries are sorted newest first

- **Given** several audit entries with distinct timestamps exist
- **When** the client calls `GET /api/updates`
- **Then** the response `content` is sorted by `createdAt` descending

## Empty state and small result sets

### Empty audit log returns empty content

- **Given** no relevant audit-log entries exist
- **When** the client calls `GET /api/updates`
- **Then** `content` is an empty array
- **And** `page.totalElements` is `0`

### Fewer entries than requested size

- **Given** only 3 relevant audit-log entries exist
- **When** the client calls `GET /api/updates?size=20`
- **Then** `content.length` is `3`
- **And** `page.size` is `20`
- **And** `page.totalElements` is `3`

### Iterative fetch returns up to size after heavy dedupe

- **Given** the latest 500 audit entries are dominated by repeated UPDATEs that
  collapse, and at least 20 distinct events exist in the full audit log
- **When** the client calls `GET /api/updates?size=20`
- **Then** `content.length` is `20`

## Name resolution edge cases

### Entity vanishes between audit emission and read

- **Given** an audit entry exists for `COMPANY_UPDATED` on company C
- **And** company C no longer exists in the database (unexpected, but possible)
- **When** the client calls `GET /api/updates`
- **Then** the entry's `entityName` is `null`
- **And** the entry's `entityId` is the company's UUID (link target is dead but
  preserved for diagnosis)

### Comment parent has been deleted

- **Given** an audit entry exists for `COMPANY_COMMENT_DELETED` with parent company C
- **And** company C has since been deleted
- **When** the client calls `GET /api/updates`
- **Then** the entry's `entityName` is `null`

## Comment audit emission (backend invariant)

### Adding a comment to a company emits an audit entry

- **Given** a request `POST /api/companies/{id}/comments` succeeds
- **When** the operation is committed
- **Then** an audit-log row exists with `entityType = "CompanyComment"`,
  `entityId = {id}`, `action = INSERT`, and `user = current user`

### Updating a company comment emits an audit entry

- **Given** a request `PUT /api/companies/{id}/comments/{commentId}` succeeds
- **When** the operation is committed
- **Then** an audit-log row exists with `entityType = "CompanyComment"`,
  `entityId = {id}`, `action = UPDATE`

### Deleting a company comment emits an audit entry

- **Given** a request `DELETE /api/companies/{id}/comments/{commentId}` succeeds
- **When** the operation is committed
- **Then** an audit-log row exists with `entityType = "CompanyComment"`,
  `entityId = {id}`, `action = DELETE`

### Same invariants for contact comments

- **Given** the equivalent endpoints on `/api/contacts/{id}/comments` are called
- **Then** audit-log rows are emitted with `entityType = "ContactComment"` and
  `entityId = {id}` (parent contact UUID)

### Task comments are not emitted as updates

- **Given** a comment is created or modified on a task (Spec 071)
- **When** the client calls `GET /api/updates`
- **Then** no entry appears for the task comment (task comments are out of scope)

## Filter — out-of-scope entity types

### User-related audit entries are not exposed

- **Given** a user account is created or modified, generating audit entries with
  `entityType` outside `{CompanyDto, ContactDto, CompanyComment, ContactComment}`
- **When** the client calls `GET /api/updates`
- **Then** no entry for the user change appears

### Webhook, API key, tag audit entries are not exposed

- **Given** audit entries exist for webhooks, API keys or tags
- **When** the client calls `GET /api/updates`
- **Then** none of these entries appears in `content`

## Frontend rendering

### Navigation entry is shown to every authenticated user

- **Given** the user is logged in (any role)
- **When** the main layout renders
- **Then** the sidebar contains an "Updates" entry as the first item in the main
  section, above Companies / Contacts / Tags

### Page renders the empty-state text when content is empty

- **Given** the API returns `content = []`
- **When** the Updates page is loaded
- **Then** the page shows the translated text "Noch keine Änderungen" (de) /
  "No changes yet" (en)

### Entity name in the entry text is a link when entityId is non-null

- **Given** an entry has `type = COMPANY_UPDATED`, `entityId = X`, `entityName = "Y"`
- **When** the row is rendered
- **Then** the substring `"Y"` in the message is a link to `/companies/X`

### No link for COMPANY_DELETED and CONTACT_DELETED

- **Given** an entry has `type = COMPANY_DELETED` (or `CONTACT_DELETED`)
- **When** the row is rendered
- **Then** the message is shown as plain text without any link
- **And** no entity name appears in the message

### Comment entries link to the parent entity

- **Given** an entry has `type = COMPANY_COMMENT_CREATED`, `entityId = X`,
  `entityName = "Y"`
- **When** the row is rendered
- **Then** `"Y"` is a link to `/companies/X` (not to a comment URL)

### Page-size combobox persists choice in localStorage

- **Given** the user selects `100` in the page-size combobox
- **When** the user reloads the page later
- **Then** the combobox is preselected to `100`
- **And** the initial fetch uses `size=100`

### Manual reload reflects newly created entries

- **Given** the Updates page is open and shows entries A, B, C
- **And** a colleague creates a new company in another browser
- **When** the user reloads the page
- **Then** the new `COMPANY_CREATED` entry appears at the top

### Page does not auto-refresh

- **Given** the Updates page is open
- **And** a new event occurs while the page is open
- **When** the user does **not** trigger a reload
- **Then** the list shown does not change (no polling, no push)

## i18n

### German user sees German texts

- **Given** the user's interface language is `de`
- **When** the Updates page renders
- **Then** event rows show the German template (e.g. "Neue Firma X wurde angelegt")
- **And** the page title and empty-state text are German

### English user sees English texts

- **Given** the user's interface language is `en`
- **When** the Updates page renders
- **Then** event rows show the English template (e.g. "New company X was created")
- **And** the page title and empty-state text are English
