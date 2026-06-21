# Behaviors: Company Logo & Contact Photo Upload

## Company Logo — Upload

### Logo can be uploaded as PNG

- **Given** an existing company without a logo
- **When** the user uploads a PNG file (≤ 2MB) via the company form
- **Then** the logo is stored and displayed in the detail view and list table

### Logo can be uploaded as JPEG

- **Given** an existing company without a logo
- **When** the user uploads a JPEG file (≤ 2MB) via the company form
- **Then** the logo is stored and displayed correctly

### Logo can be uploaded as SVG

- **Given** an existing company without a logo
- **When** the user uploads an SVG file (≤ 2MB) via the company form
- **Then** the logo is stored and displayed correctly

### Uploading a new logo replaces the old one

- **Given** a company with an existing PNG logo
- **When** the user uploads a new JPEG logo
- **Then** the old logo is replaced and the new logo is displayed

### Logo upload rejects files exceeding 2MB

- **Given** the user selects a PNG file larger than 2MB
- **When** the user attempts to upload it
- **Then** an error message is shown ("Datei ist zu groß" / "File is too large")
- **And** the logo is not changed

### Logo upload rejects invalid formats

- **Given** the user selects a GIF file
- **When** the user attempts to upload it
- **Then** an error message is shown ("Ungültiges Dateiformat" / "Invalid file format")
- **And** the logo is not changed

## Company Logo — Display

### Logo is shown as thumbnail in company list table

- **Given** a company with a logo
- **When** the company list table renders
- **Then** the first column shows a small thumbnail of the logo

### Placeholder icon shown in list when no logo exists

- **Given** a company without a logo
- **When** the company list table renders
- **Then** the first column shows a generic building placeholder icon

### Logo is shown larger in company detail view

- **Given** a company with a logo
- **When** the user views the company detail page
- **Then** the logo is displayed near the company name at a larger size

### Placeholder icon shown in detail when no logo exists

- **Given** a company without a logo
- **When** the user views the company detail page
- **Then** a generic building placeholder icon is shown

## Company Logo — Removal

### Logo can be removed

- **Given** a company with a logo
- **When** the user clicks "Logo entfernen" / "Remove logo"
- **Then** the logo is deleted and the placeholder icon is shown

## Company Logo — Soft Delete & Restore

### Logo is preserved when company is soft-deleted

- **Given** a company with a logo
- **When** the company is soft-deleted
- **Then** the logo data remains in the database

### Logo is available after company is restored

- **Given** a soft-deleted company that had a logo before deletion
- **When** the company is restored
- **Then** the logo is displayed again in list and detail views

## Contact Photo — Upload

### Photo can be uploaded as JPEG

- **Given** an existing contact without a photo
- **When** the user uploads a JPEG file (≤ 2MB) via the contact form
- **Then** the photo is stored and displayed in the detail view and list table

### Uploading a new photo replaces the old one

- **Given** a contact with an existing photo
- **When** the user uploads a new JPEG photo
- **Then** the old photo is replaced and the new photo is displayed

### Photo upload rejects files exceeding 2MB

- **Given** the user selects a JPEG file larger than 2MB
- **When** the user attempts to upload it
- **Then** an error message is shown
- **And** the photo is not changed

### Photo upload rejects non-JPEG formats

- **Given** the user selects a PNG file
- **When** the user attempts to upload it as a contact photo
- **Then** an error message is shown ("Ungültiges Dateiformat" / "Invalid file format")
- **And** the photo is not changed

## Contact Photo — Display

### Photo is shown as thumbnail in contact list table

- **Given** a contact with a photo
- **When** the contact list table renders
- **Then** the first column shows a small circular thumbnail of the photo

### Placeholder icon shown in list when no photo exists

- **Given** a contact without a photo
- **When** the contact list table renders
- **Then** the first column shows a generic user placeholder icon

### Photo is shown larger in contact detail view

- **Given** a contact with a photo
- **When** the user views the contact detail page
- **Then** the photo is displayed near the contact name at a larger size

### Placeholder icon shown in detail when no photo exists

- **Given** a contact without a photo
- **When** the user views the contact detail page
- **Then** a generic user placeholder icon is shown

## Contact Photo — Removal

### Photo can be removed

- **Given** a contact with a photo
- **When** the user clicks "Foto entfernen" / "Remove photo"
- **Then** the photo is deleted and the placeholder icon is shown

## Contact Photo — Hard Delete

### Photo is deleted when contact is deleted

- **Given** a contact with a photo
- **When** the contact is hard-deleted
- **Then** the photo data is removed from the database (no orphan data)

## Backend API — Company Logo

### GET returns logo with correct content type

- **Given** a company with a PNG logo
- **When** the API receives `GET /api/companies/{id}/logo`
- **Then** the response has status 200, `Content-Type: image/png`, and the binary image data

### GET returns 404 when no logo exists

- **Given** a company without a logo
- **When** the API receives `GET /api/companies/{id}/logo`
- **Then** the response has status 404

### GET returns 404 for non-existent company

- **Given** no company with the given ID exists
- **When** the API receives `GET /api/companies/{id}/logo`
- **Then** the response has status 404

### POST stores logo and returns 200

- **Given** an existing company
- **When** the API receives `POST /api/companies/{id}/logo` with a valid PNG file
- **Then** the logo bytes and content type are stored
- **And** the response has status 200

### POST returns 400 for invalid content type

- **Given** an existing company
- **When** the API receives `POST /api/companies/{id}/logo` with a GIF file
- **Then** the response has status 400

### POST returns 400 for file exceeding size limit

- **Given** an existing company
- **When** the API receives `POST /api/companies/{id}/logo` with a 3MB file
- **Then** the response has status 400

### DELETE removes logo and returns 204

- **Given** a company with a logo
- **When** the API receives `DELETE /api/companies/{id}/logo`
- **Then** the logo is set to null and the response has status 204

## Backend API — Contact Photo

### GET returns photo with correct content type

- **Given** a contact with a JPEG photo
- **When** the API receives `GET /api/contacts/{id}/photo`
- **Then** the response has status 200, `Content-Type: image/jpeg`, and the binary data

### GET returns 404 when no photo exists

- **Given** a contact without a photo
- **When** the API receives `GET /api/contacts/{id}/photo`
- **Then** the response has status 404

### POST returns 400 for non-JPEG format

- **Given** an existing contact
- **When** the API receives `POST /api/contacts/{id}/photo` with a PNG file
- **Then** the response has status 400

### DELETE removes photo and returns 204

- **Given** a contact with a photo
- **When** the API receives `DELETE /api/contacts/{id}/photo`
- **Then** the photo is set to null and the response has status 204

## DTO — hasLogo / hasPhoto Flags

### CompanyDto hasLogo is true when logo exists

- **Given** a company with an uploaded logo
- **When** the API returns the company DTO
- **Then** `hasLogo` is `true`

### CompanyDto hasLogo is false when no logo exists

- **Given** a company without a logo
- **When** the API returns the company DTO
- **Then** `hasLogo` is `false`

### ContactDto hasPhoto is true when photo exists

- **Given** a contact with an uploaded photo
- **When** the API returns the contact DTO
- **Then** `hasPhoto` is `true`

### ContactDto hasPhoto is false when no photo exists

- **Given** a contact without a photo
- **When** the API returns the contact DTO
- **Then** `hasPhoto` is `false`

## List Table — Column Order

### Company list has image as first column

- **Given** the company list table
- **When** it renders
- **Then** the column order is: (Image), Name, Website, Contacts, Comments, Actions

### Contact list has image as first column

- **Given** the contact list table
- **When** it renders
- **Then** the column order is: (Image), First Name, Last Name, Company, Comments, Actions

## i18n

### German labels for logo upload

- **Given** the UI is set to German
- **When** the company form renders
- **Then** labels read "Logo", "Logo hochladen", "Logo entfernen"

### English labels for logo upload

- **Given** the UI is set to English
- **When** the company form renders
- **Then** labels read "Logo", "Upload logo", "Remove logo"

### German labels for photo upload

- **Given** the UI is set to German
- **When** the contact form renders
- **Then** labels read "Foto", "Foto hochladen", "Foto entfernen"

### English labels for photo upload

- **Given** the UI is set to English
- **When** the contact form renders
- **Then** labels read "Photo", "Upload photo", "Remove photo"
