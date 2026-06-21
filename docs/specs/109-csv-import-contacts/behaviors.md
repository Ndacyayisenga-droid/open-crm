# Behaviors: CSV Import for Contacts

## Authorization

### APP-ADMIN can open the import dialog

- **Given** a signed-in user whose roles include `APP-ADMIN`
- **When** they navigate to `/contacts`
- **Then** the toolbar shows both an "Export" and an "Import" button
- **And** clicking "Import" opens the multi-step import dialog at the Upload step

### IT-ADMIN can open the import dialog

- **Given** a signed-in user whose roles include `IT-ADMIN` (and not `APP-ADMIN`)
- **When** they navigate to `/contacts`
- **Then** the toolbar shows the "Import" button and clicking it opens the dialog

### Non-admin does not see the import button

- **Given** a signed-in user whose roles include neither `APP-ADMIN` nor `IT-ADMIN`
- **When** they navigate to `/contacts`
- **Then** the toolbar shows only the existing "Export" button
- **And** the "Import" button is not rendered

### Backend rejects /preview without admin role

- **Given** a signed-in user without `APP-ADMIN` or `IT-ADMIN` and a valid auth cookie
- **When** they POST to `/api/contacts/import/preview` with any payload
- **Then** the response is HTTP `403`
- **And** `ContactImportService` is not invoked

### Backend rejects /commit without admin role

- **Given** a signed-in user without `APP-ADMIN` or `IT-ADMIN` and a valid auth cookie
- **When** they POST to `/api/contacts/import/commit` with any payload
- **Then** the response is HTTP `403`
- **And** no rows are inserted

## Upload step

### Import button is disabled until a file is selected

- **Given** the import dialog is open at the Upload step
- **When** no file has been chosen
- **Then** the "Next" button is disabled

### Default encoding is UTF-8 and header checkbox is checked

- **Given** the import dialog is opened for the first time in a session
- **When** the Upload step renders
- **Then** the encoding dropdown shows `UTF-8` selected
- **And** the "first row is header" checkbox is checked

### Encoding dropdown offers exactly UTF-8 and Windows-1252 in v1

- **Given** the Upload step is rendered
- **When** the encoding dropdown is expanded
- **Then** the visible options are exactly `UTF-8` and `Windows-1252`

## Preview endpoint

### Comma-delimited file is parsed correctly

- **Given** a UTF-8, comma-delimited CSV with a header row and 55 data rows
- **When** the frontend POSTs the file to `/api/contacts/import/preview` with `encoding=UTF-8` and `hasHeader=true`
- **Then** the response is HTTP `200`
- **And** the body contains `delimiter=","`, `totalRows=55`, and `sampleRows.length=3`
- **And** the `columns` array equals the header row in original order

### Semicolon-delimited file is auto-detected

- **Given** a UTF-8, semicolon-delimited CSV with a header row and 10 data rows
- **When** the frontend POSTs the file to `/preview` with `encoding=UTF-8` and `hasHeader=true`
- **Then** the response body contains `delimiter=";"` and `totalRows=10`
- **And** `sampleRows` contains the first three rows correctly split on `;`

### Tied delimiter counts default to comma

- **Given** a CSV whose first line contains exactly the same number of unquoted commas as unquoted semicolons
- **When** /preview is called
- **Then** the response uses `delimiter=","`

### Windows-1252 file with umlauts is decoded correctly

- **Given** a Windows-1252-encoded CSV containing the value `Müller` in the `lastName` column
- **When** the frontend POSTs the file with `encoding=Windows-1252`
- **Then** the corresponding `sampleRows` entry contains `Müller` (not mojibake like `MÃ¼ller`)

### UTF-8 with BOM is accepted

- **Given** a UTF-8 CSV that begins with the UTF-8 byte-order-mark `EF BB BF`
- **When** /preview is called with `encoding=UTF-8`
- **Then** the BOM is stripped before the first column name is read
- **And** the first column header does not contain the BOM characters

### Headerless file produces synthesized column names

- **Given** a CSV without a header row (first line is already a data record) and 4 columns per row
- **When** /preview is called with `hasHeader=false`
- **Then** the response `columns` is exactly `["Spalte 1", "Spalte 2", "Spalte 3", "Spalte 4"]`
- **And** `totalRows` counts every row in the file (no row is treated as a header)

### File exceeding 5 000 rows is rejected

- **Given** a valid CSV with 5 001 data rows
- **When** /preview is called
- **Then** the response is HTTP `413`
- **And** the response body identifies the row-cap as the cause
- **And** no parsing of the row body beyond row 5 001 is required to determine this

### File exceeding 20 MB is rejected by the upload layer

- **Given** a CSV whose payload exceeds 20 MB
- **When** the frontend POSTs it to /preview
- **Then** the response is HTTP `413` (Spring multipart size limit) before `ContactImportService` is invoked

### Empty file is rejected

- **Given** a 0-byte file
- **When** /preview is called
- **Then** the response is HTTP `400`
- **And** the body indicates the file has no rows

### File with header only is rejected

- **Given** a CSV containing only a header row and no data rows
- **When** /preview is called with `hasHeader=true`
- **Then** the response is HTTP `400`
- **And** the body indicates the file has no data rows

### Unsupported encoding is rejected

- **Given** the frontend sends `encoding=UTF-16` in the /preview request
- **When** the backend processes the request
- **Then** the response is HTTP `415`
- **And** the body identifies the encoding as unsupported

### Mojibake in the header is not auto-corrected

- **Given** a CSV that is actually UTF-8 but the client sent `encoding=Windows-1252`
- **When** /preview is called
- **Then** the response is HTTP `200` with garbled column names visible in `columns`
- **And** no warning is emitted (the user is responsible for re-uploading with the correct encoding)

## Mapping step

### Required mappings are enforced before commit

- **Given** the user is on the Mapping step
- **When** they have mapped a column to `LAST_NAME` but no column to `FIRST_NAME`
- **Then** the "Next" / "Import" button is disabled
- **And** an inline hint indicates that both `firstName` and `lastName` are required

### Same target cannot be selected twice

- **Given** the user has selected `EMAIL` as the target for column A
- **When** they try to select `EMAIL` as the target for column B
- **Then** the dialog prevents the second selection (e.g. the option is disabled in the second dropdown) and surfaces a hint
- **And** the import cannot proceed until the conflict is resolved

### Unmapped columns are dropped

- **Given** a CSV with columns `Vorname, Nachname, Email, Typ, Owner, Notiz`
- **And** the user maps only `Vorname → FIRST_NAME`, `Nachname → LAST_NAME`, `Email → EMAIL`
- **When** /commit runs
- **Then** every successfully created contact has only `firstName`, `lastName`, and `email` populated
- **And** the values of `Typ`, `Owner`, and `Notiz` are not stored anywhere

### Commit request without required mappings returns 400

- **Given** the frontend bypasses the disabled "Import" button and posts a /commit request whose mapping omits `FIRST_NAME`
- **When** the backend processes the request
- **Then** the response is HTTP `400`
- **And** no rows are inserted

## Preview rendering

### Preview shows exactly the first three data rows

- **Given** a CSV with 55 data rows
- **When** the user advances to the Preview step
- **Then** the preview table renders exactly three rows
- **And** they are rows 1, 2, and 3 of the data section (after skipping the header if `hasHeader=true`)

### Preview reflects the user's mapping live

- **Given** the user is on the Mapping step
- **When** they change a mapping (e.g. `Vorname → FIRST_NAME` to `Vorname → POSITION`)
- **And** advance to Preview
- **Then** the preview shows `Vorname`'s value in the `position` field of the three sample contacts
- **And** the `firstName` field of the preview is empty (or shows the column now mapped to `FIRST_NAME`)

### Preview highlights validation errors on the three sample rows

- **Given** the first sample row has an invalid email (`"not-an-email"`) and `EMAIL` is mapped
- **When** the Preview step renders
- **Then** the email cell for that row is visually marked as invalid
- **And** an inline reason ("invalid email") is shown adjacent to the cell
- **And** the error originates from the `sampleContacts[0].errors` array returned by the backend, not from any client-side validation

## Preview/commit consistency (single source of truth)

### Frontend never transforms a row on its own

- **Given** the import dialog is open and a mapping has been chosen
- **When** the Preview step renders
- **Then** every Contact-shaped value displayed in the preview came from the backend's
  `sampleContacts` field — no field is constructed in JavaScript from `sampleRows` cells

### Preview-mapping call goes through the same transformAndValidate as commit

- **Given** the backend `/preview` endpoint is called with a non-null `mapping`
- **When** the response is produced
- **Then** each entry in `sampleContacts` was produced by `ContactImportService.transformAndValidate(rawRow, mapping)`
- **And** the row loop inside `/commit` calls the **same** `transformAndValidate(rawRow, mapping)` method
- **And** there is no second implementation of CSV-row-to-Contact transformation in the code base

### A row that previews valid commits as valid

- **Given** /preview-with-mapping returned `sampleContacts[i].errors = []` for the first three rows
- **And** the database, the network, and the request payload are unchanged between /preview and /commit
- **When** /commit is called immediately afterward
- **Then** the corresponding rows in /commit's response are reported as `CREATED`
- **And** they are not reported in `failures`

### A row that previews invalid commits as invalid with the same reason

- **Given** /preview-with-mapping returned `sampleContacts[i].errors = [{field: "email", reason: "invalid"}]` for row 2
- **And** the request payload is unchanged
- **When** /commit is called
- **Then** row 2 appears in `failures` with `field="email"` and `reason="invalid"`
- **And** no `ContactEntity` is created for row 2

### Mapping that fails preview validation never reaches commit

- **Given** the user submits a /preview-with-mapping request where the mapping omits `FIRST_NAME`
- **When** the backend processes the request
- **Then** /preview returns HTTP `400` with the same error code that /commit would return
- **And** the frontend disables the "Import" button so /commit is never called for this mapping

## Commit endpoint — happy path

### All-valid 5-row CSV produces 5 created, 0 failed

- **Given** a 5-row CSV with all rows passing validation and a complete mapping
- **When** /commit runs
- **Then** the response is HTTP `200` with body `{ createdCount: 5, failedCount: 0, failures: [] }`
- **And** the contacts table contains 5 new rows after the request
- **And** each new contact has a per-contact audit-log entry with `createdBy=<importing user>`

### LinkedIn URL maps to a SocialLinkEntity with LINKEDIN

- **Given** a row whose mapped `LINKEDIN_URL` cell is `https://www.linkedin.com/in/hendrikebbers`
- **When** /commit runs
- **Then** the created contact has exactly one `SocialLinkEntity` with `networkType=LINKEDIN`
- **And** its `value` is `https://www.linkedin.com/in/hendrikebbers`

### Website URL maps to a SocialLinkEntity with WEBSITE

- **Given** a row whose mapped `WEBSITE_URL` cell is `https://open-elements.com`
- **When** /commit runs
- **Then** the created contact has exactly one `SocialLinkEntity` with `networkType=WEBSITE`
- **And** its `value` is `https://open-elements.com`

### Empty optional cell does not create a SocialLink

- **Given** a row whose mapped `LINKEDIN_URL` cell is the empty string
- **When** /commit runs
- **Then** the created contact has no `SocialLinkEntity` of type `LINKEDIN`

### Trailing whitespace in required fields is trimmed

- **Given** a row whose `firstName` cell is `"  Holger  "` and `lastName` cell is `"Dyroff"`
- **When** /commit runs
- **Then** the created contact has `firstName="Holger"` and `lastName="Dyroff"`

## Commit endpoint — partial success

### Blank required field marks the row FAILED but commits others

- **Given** a 5-row CSV where row 3 has an empty `lastName` cell
- **When** /commit runs
- **Then** the response is `{ createdCount: 4, failedCount: 1, failures: [{ row: 3, field: "lastName", reason: "required" }] }`
- **And** the database contains 4 new contacts (none from row 3)

### Invalid email marks the row FAILED

- **Given** a 5-row CSV where row 2 has `email="not-an-email"` and `EMAIL` is mapped
- **When** /commit runs
- **Then** `failures` contains `{ row: 2, field: "email", reason: "invalid" }`
- **And** `createdCount=4` and the database contains 4 new contacts

### Overlong value marks the row FAILED

- **Given** a row whose mapped `position` cell is a 300-character string (the column is 255)
- **When** /commit runs
- **Then** `failures` contains `{ row: <n>, field: "position", reason: "too_long" }`

### Malformed row keeps the rest of the import running

- **Given** a 10-row CSV where row 6 has an unclosed quote that confuses commons-csv
- **When** /commit runs
- **Then** `failures` contains `{ row: 6, field: null, reason: "malformed_row" }`
- **And** `createdCount=9`

### Failed rows do not roll back earlier rows

- **Given** a 100-row CSV in which row 80 fails validation
- **When** /commit runs
- **Then** rows 1–79 are persisted in the database before the row 80 failure is recorded
- **And** rows 81–100 are still attempted and contribute to `createdCount`

### No deduplication — same email twice creates two contacts

- **Given** a 2-row CSV whose rows both carry `email="dyroff@b1-systems.de"`
- **When** /commit runs
- **Then** `createdCount=2` and `failedCount=0`
- **And** the database contains two `ContactEntity` rows with the same email after the request
- **And** neither row is reported as `SKIPPED` (no skip status exists in v1)

### No deduplication against existing DB contacts

- **Given** a `ContactEntity` with `email="dyroff@b1-systems.de"` already exists in the database
- **And** a 1-row CSV is uploaded carrying the same email
- **When** /commit runs
- **Then** `createdCount=1` and `failedCount=0`
- **And** the database contains two contacts with that email after the request

## Commit endpoint — file-level failures

### Row cap is re-enforced on /commit

- **Given** an attacker bypasses /preview and posts a 6 000-row CSV directly to /commit
- **When** /commit runs
- **Then** the response is HTTP `413` before any row is inserted
- **And** the database contains zero new contacts

### File size is re-enforced on /commit

- **Given** a 21 MB file POSTed to /commit
- **When** the request hits the backend
- **Then** the response is HTTP `413` and no rows are inserted

### Backend database outage produces HTTP 500, not partial success

- **Given** the PostgreSQL connection is unavailable when /commit starts
- **When** /commit runs
- **Then** the response is HTTP `500`
- **And** the response is not a `200` with `failedCount=N` (database-down is a file-level failure, not a row-level one)

### Database outage mid-import surfaces remaining rows as FAILED

- **Given** the database is healthy for the first 20 rows and then becomes unreachable for the next 30 rows of a 50-row file
- **When** /commit runs
- **Then** rows 21–50 are recorded as `FAILED` with a generic infrastructure reason
- **And** rows 1–20 remain persisted (they were committed in their own transactions)
- **And** the response is HTTP `200` with `createdCount=20, failedCount=30`

## Result step

### Result shows aggregate counts at the top

- **Given** /commit returned `{ createdCount: 52, failedCount: 3, ... }`
- **When** the Result step renders
- **Then** the header reads exactly "52 created, 3 failed" (localized)
- **And** the failure table renders 3 rows

### Failure table shows row, field, and reason columns

- **Given** the Result step renders for a response with three failures
- **When** the user inspects the table
- **Then** the table has columns `row`, `field`, `reason`
- **And** each row's `reason` is rendered as a localized string (not the raw machine code)

### Download failure CSV produces the failed rows plus error columns

- **Given** the Result step is shown with three failed rows
- **When** the user clicks "Download failure CSV"
- **Then** a CSV is downloaded that uses the same delimiter and encoding as the upload
- **And** it contains the failed rows in their original form
- **And** two extra columns `_error_field` and `_error_reason` are appended on the right of each row

### Successful contacts list refreshes after close

- **Given** /commit returned `createdCount=5`
- **When** the user closes the dialog after seeing the result
- **Then** the contacts list re-fetches and renders 5 additional contacts (subject to current filters and pagination)

## File handling and GDPR

### Uploaded file is not persisted to disk

- **Given** /commit runs to completion
- **When** the import response has been returned
- **Then** no copy of the uploaded CSV remains on the backend filesystem
- **And** no copy is written to the database (no `imported_files` table, no blob storage)

### Failed-row contents are not logged

- **Given** /commit runs and a row fails because of an invalid email value
- **When** the application logs are inspected
- **Then** the offending email value is not present in the logs
- **And** only the row number, field name, and reason code are visible in any logged audit trail

### Per-contact audit-log entries are produced for created rows

- **Given** /commit creates 10 contacts as user `u-42`
- **When** the audit log is queried
- **Then** there are 10 new audit-log rows of type `CONTACT_CREATED` with `createdBy=u-42` and `entityName=Contact`
- **And** no aggregated "import operation" audit-log row exists

### No audit-log row is produced for failed rows

- **Given** /commit produces 3 failed rows
- **When** the audit log is queried
- **Then** there are no new audit-log rows corresponding to those failures
- **And** the only trace of them is the response body returned to the user
