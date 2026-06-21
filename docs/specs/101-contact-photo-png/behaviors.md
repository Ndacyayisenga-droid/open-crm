# Behaviors: Contact photo PNG support

## Backend — JPEG upload (unchanged path)

### JPEG upload stores bytes as-is

- **Given** an authenticated user and an existing contact
- **And** a JPEG file ≤ 2 MB
- **When** the client posts `POST /api/contacts/{id}/photo` with `Content-Type: image/jpeg` and the file bytes
- **Then** the response is `200 OK`
- **And** the contact's `photo` column contains exactly the uploaded bytes (byte-for-byte equality)
- **And** the contact's `photo_content_type` is `image/jpeg`

### JPEG upload preserves EXIF metadata

- **Given** a JPEG file that contains EXIF metadata (including GPS coordinates)
- **When** it is uploaded successfully
- **Then** the stored bytes still contain that EXIF block
- **And** re-downloading the photo via `GET /api/contacts/{id}/photo` returns bytes byte-identical to the upload

### JPEG upload of an oversized file is rejected

- **Given** a JPEG file > 2 MB
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`
- **And** the contact's `photo` column is not modified
- **And** the response body contains a recognizable "size exceeds" error message

## Backend — PNG upload (new path)

### Opaque PNG upload is transcoded to JPEG

- **Given** an authenticated user and an existing contact
- **And** an opaque PNG (no alpha channel) ≤ 2 MB
- **When** the client posts to `POST /api/contacts/{id}/photo` with `Content-Type: image/png`
- **Then** the response is `200 OK`
- **And** the contact's `photo_content_type` is `image/jpeg` (not `image/png`)
- **And** the stored bytes are valid JPEG (`ImageIO.read` succeeds and returns a `BufferedImage` of the original PNG's dimensions)
- **And** the stored bytes are NOT byte-equal to the uploaded PNG

### Transparent PNG is flattened over white

- **Given** a PNG of dimensions W×H with an alpha channel; a sample pixel `(x, y)` is fully transparent (`alpha = 0`)
- **When** the PNG is uploaded successfully
- **Then** the stored JPEG, when decoded, has dimensions W×H
- **And** the decoded pixel at `(x, y)` is RGB `(255, 255, 255)` (white)

### Semi-transparent PNG is composited over white

- **Given** a PNG where pixel `(x, y)` is solid red `(255, 0, 0)` with 50% alpha (i.e., `RGBA(255, 0, 0, 128)`)
- **When** the PNG is uploaded successfully
- **Then** the decoded pixel at `(x, y)` in the stored JPEG is approximately RGB `(255, 128, 128)` (50/50 composite of red over white, within JPEG-compression tolerance)

### Oversized PNG is rejected before transcoding

- **Given** a PNG file > 2 MB (regardless of how small it would be after JPEG re-encode)
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`
- **And** no transcoding work was performed (verifiable by absence of CPU spike / by mocking the transcoder and asserting it was not invoked in unit tests)
- **And** the contact's `photo` column is not modified

### Malformed PNG is rejected with 400

- **Given** an upload with `Content-Type: image/png` whose bytes are not a valid PNG (e.g., random bytes, truncated PNG, or another format mislabelled)
- **When** the client posts it (size ≤ 2 MB)
- **Then** the response is `400 BAD REQUEST`
- **And** the response body contains a recognizable "could not decode PNG" message
- **And** the contact's `photo` column is not modified

### PNG transcode strips EXIF

- **Given** a PNG that carries embedded textual metadata (`tEXt`/`iTXt` chunks) or any non-pixel data
- **When** the PNG is uploaded and transcoded
- **Then** the stored JPEG does not contain that metadata (re-encode does not propagate source metadata)

## Backend — rejected content types

### GIF, WebP, SVG, HEIC, BMP, PDF are rejected

- **Given** an upload with `Content-Type` set to any of: `image/gif`, `image/webp`, `image/svg+xml`, `image/heic`, `image/bmp`, `application/pdf`
- **When** the client uploads (size ≤ 2 MB)
- **Then** the response is `400 BAD REQUEST` for every case
- **And** the response message indicates JPEG and PNG are the only accepted types
- **And** the contact's `photo` column is not modified

### Missing content type is rejected

- **Given** an upload with no `Content-Type` header set on the file part
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`

## Backend — display path

### GET returns stored JPEG with correct header

- **Given** a contact that previously had a JPEG uploaded
- **When** the client requests `GET /api/contacts/{id}/photo`
- **Then** the response status is `200`
- **And** the `Content-Type` response header is `image/jpeg`
- **And** the body is byte-equal to the originally-uploaded JPEG

### GET after a PNG upload also returns JPEG

- **Given** a contact whose photo was uploaded as PNG (and transcoded server-side)
- **When** the client requests `GET /api/contacts/{id}/photo`
- **Then** the response `Content-Type` is `image/jpeg`
- **And** the body decodes to a `BufferedImage` of the original PNG's dimensions

## Frontend — file input

### File input accepts PNG and JPEG

- **Given** the contact form is open
- **When** the user opens the file picker
- **Then** the input's `accept` attribute is `"image/jpeg,image/png"` (or an equivalent that includes both MIME types)

### Client-side guard accepts JPEG

- **Given** the user has selected a `.jpg` / `.jpeg` file ≤ 2 MB
- **When** the file change handler runs
- **Then** no error message is shown
- **And** the selected file is queued for upload

### Client-side guard accepts PNG

- **Given** the user has selected a `.png` file ≤ 2 MB
- **When** the file change handler runs
- **Then** no error message is shown
- **And** the selected file is queued for upload

### Client-side guard rejects other formats

- **Given** the user selects a file of any other MIME type (e.g., `image/gif`, `image/webp`, `image/svg+xml`, `image/heic`, `application/pdf`)
- **When** the file change handler runs
- **Then** an inline error message is shown using the updated i18n key (mentioning JPEG and PNG)
- **And** the file is not queued for upload
- **And** no network request is made

### Client-side size guard still rejects > 2 MB

- **Given** the user selects a JPEG or PNG file > 2 MB
- **When** the file change handler runs
- **Then** the existing size-error message is shown
- **And** no upload is initiated

### Successful upload re-renders the photo

- **Given** the user submits a valid PNG upload
- **And** the backend returns 200
- **When** the form completes
- **Then** the contact detail view re-fetches and displays the photo
- **And** the rendered `<img>` shows the transcoded JPEG correctly inside the `rounded-full` avatar

## Internationalization

### German translation lists both formats

- **Given** the user has selected an invalid file in the German UI
- **Then** the error message text reads "Bitte eine JPEG- oder PNG-Datei wählen." (or whatever final wording is decided during implementation, as long as both formats are listed)

### English translation lists both formats

- **Given** the user has selected an invalid file in the English UI
- **Then** the error message text reads "Please choose a JPEG or PNG file."

## Regressions to guard against

### Existing contacts with JPEG photos still load

- **Given** a contact whose photo was uploaded before this spec landed (stored as JPEG bytes, `photo_content_type = "image/jpeg"`)
- **When** the user opens the detail view
- **Then** the photo loads and renders identically to before

### Company logo upload is unaffected

- **Given** the user uploads a PNG company logo (which already worked pre-spec)
- **When** the upload completes
- **Then** the logo is stored byte-for-byte as PNG (no transcoding)
- **And** `companies.logo_content_type` is `image/png` (not `image/jpeg`)
- **And** the detail view renders the PNG with its alpha channel intact

### `/api/contacts/{id}/photo` content-type is always `image/jpeg`

- **Given** any contact with a photo uploaded after this spec lands
- **When** `GET /api/contacts/{id}/photo` is called
- **Then** the response `Content-Type` is always `image/jpeg`, regardless of whether the original upload was JPEG or PNG
