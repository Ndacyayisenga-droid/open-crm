# Behaviors: Contact photo HEIC & WebP support

This document inherits all scenarios from spec 101's `behaviors.md` (JPEG and PNG paths). The scenarios below add HEIC and WebP coverage, plus regressions to ensure the existing paths remain green.

**Test fixtures.** Four happy-path fixtures live under `backend/src/test/resources/images/`:

- `images/test.jpeg` — iPhone 15 Pro JPEG with EXIF/GPS, orientation 1
- `images/test.png` — 800×800 RGB, opaque
- `images/test.webp` — VP8 lossy, opaque
- `images/test.heic` — iPhone HEIF/HEVC, opaque

Scenarios marked **[fixture-disabled]** are implemented as `@Disabled` tests in v1 because they need fixture variants not yet on disk (rotated HEIC, alpha images, animated WebP, oversize, probe sample). They become active once those fixtures land (tracked in `TODO.md`: "HEIC- und WebP-Testfixtures bereitstellen + Tests aktivieren").

## Backend — HEIC upload (new path)

### Opaque HEIC upload is transcoded to JPEG

- **Given** an authenticated user and an existing contact
- **And** `HeicSupportCheck.isHeicAvailable() == true`
- **And** the fixture `images/test.heic` (≤ 2 MB, opaque iPhone HEIC)
- **When** the client posts to `POST /api/contacts/{id}/photo` with `Content-Type: image/heic`
- **Then** the response is `200 OK`
- **And** the contact's `photo_content_type` is `image/jpeg` (not `image/heic`)
- **And** the stored bytes are valid JPEG and decode to a `BufferedImage` of the original HEIC's dimensions
- **And** the stored bytes are NOT byte-equal to the uploaded HEIC

### `image/heif` content type is accepted equivalently

- **Given** the fixture `images/test.heic` uploaded with `Content-Type: image/heif` (the parent-format MIME type some clients use)
- **When** the client posts it (size ≤ 2 MB, `heicAvailable == true`)
- **Then** the response is `200 OK`
- **And** the stored bytes are a valid JPEG

### HEIC with EXIF orientation 6 is decoded upright  [fixture-disabled]

- **Given** an HEIC file with `EXIF Orientation = 6` (image stored rotated 90° clockwise; iPhone-typical for portrait shots)
- **And** the visual subject occupies the upper half of the rotated frame
- **When** the file is uploaded successfully
- **Then** the stored JPEG, when decoded, has dimensions swapped (`H × W` instead of `W × H` from the raw decode)
- **And** the visual subject is in the upper half of the upright frame

### HEIC > 2 MB is rejected before any decode  [fixture-disabled]

- **Given** an HEIC file > 2 MB and `heicAvailable == true`
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`
- **And** the `HeicTranscoderService` was never invoked (verifiable via mock assertions in unit tests)
- **And** the contact's `photo` column is not modified

### Malformed HEIC is rejected with 400

- **Given** an upload with `Content-Type: image/heic` whose bytes are a programmatically-generated random `byte[]` of length ≤ 2 MB, `heicAvailable == true`
- **When** the client posts it
- **Then** the response is `400 BAD REQUEST`
- **And** the response body contains a recognizable "could not decode HEIC" message
- **And** the contact's `photo` column is not modified

Note: this scenario uses an in-test-generated random byte array, not a disk fixture — no `@Disabled` needed.

### HEIC upload returns 415 when libheif is unavailable

- **Given** the backend has booted in an environment where `libheif` / `libheif-plugin-libde265` are missing or broken
- **And** `HeicSupportCheck.isHeicAvailable() == false` and the startup log emitted the corresponding `ERROR`
- **When** a client posts a valid `image/heic` file ≤ 2 MB
- **Then** the response is `415 UNSUPPORTED_MEDIA_TYPE`
- **And** the response body contains the message `"HEIC support is not available in this deployment"`
- **And** the contact's `photo` column is not modified
- **And** no decode work was attempted (verifiable by absence of an `ImageIO.read` call in the mocked code path)

## Backend — WebP upload (new path)

### Opaque lossy WebP upload is transcoded to JPEG

- **Given** an authenticated user and an existing contact
- **And** the fixture `images/test.webp` (VP8 lossy, opaque, ≤ 2 MB)
- **When** the client posts to `POST /api/contacts/{id}/photo` with `Content-Type: image/webp`
- **Then** the response is `200 OK`
- **And** the contact's `photo_content_type` is `image/jpeg`
- **And** the stored bytes are valid JPEG of the original WebP's dimensions

### Lossless WebP with alpha is flattened over white  [fixture-disabled]

- **Given** a lossless WebP of dimensions W×H with an alpha channel; pixel `(x, y)` is fully transparent (`alpha = 0`)
- **When** the WebP is uploaded successfully
- **Then** the stored JPEG decodes to dimensions W×H
- **And** the decoded pixel at `(x, y)` is RGB `(255, 255, 255)` (white)

### Animated WebP stores the first frame and returns 200  [fixture-disabled]

- **Given** an animated WebP with two or more frames, total size ≤ 2 MB
- **When** the client uploads it
- **Then** the response is `200 OK` (no error, no warning surfaced to the client)
- **And** the stored bytes are a valid static JPEG
- **And** the JPEG's pixel content matches the first frame of the animation (visually verified in the test by comparing a sampled pixel against the first-frame ground truth)

### WebP with EXIF orientation is decoded upright  [fixture-disabled]

- **Given** a WebP file with `EXIF Orientation = 6`
- **When** the file is uploaded successfully
- **Then** the stored JPEG is in upright orientation (dimensions swapped from the raw decode, subject in the upper half)

### WebP > 2 MB is rejected before decode  [fixture-disabled]

- **Given** a WebP file > 2 MB
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`
- **And** no transcoder invocation occurred
- **And** the contact's `photo` column is not modified

### Malformed WebP is rejected with 400

- **Given** an upload with `Content-Type: image/webp` whose bytes are a programmatically-generated random `byte[]` of length ≤ 2 MB
- **When** the client posts it
- **Then** the response is `400 BAD REQUEST`
- **And** the response body contains a recognizable "could not decode WebP" message
- **And** the contact's `photo` column is not modified

Note: this scenario uses an in-test-generated random byte array, not a disk fixture — no `@Disabled` needed.

## Backend — startup probe

### Successful startup probe sets heicAvailable = true  [fixture-disabled — depends on probe sample]

- **Given** the backend is starting with `libheif1` + `libheif-plugin-libde265` installed and the `imageio-heif` JAR on the classpath
- **And** the probe sample `classpath:/heic-probe/sample.heic` is present
- **When** `HeicSupportCheck.@PostConstruct` runs
- **Then** `isHeicAvailable()` returns `true`
- **And** an `INFO` log line is emitted including the probe-image dimensions

### Failed startup probe sets heicAvailable = false (missing library)

- **Given** the backend is starting in an environment where `libheif` is missing or fails to load
- **When** `HeicSupportCheck.@PostConstruct` runs
- **Then** `isHeicAvailable()` returns `false`
- **And** an `ERROR` log line is emitted explaining what to install and that HEIC uploads will be rejected with 415
- **And** the application context **still starts** (no fail-fast)

### Failed startup probe sets heicAvailable = false (reader unregistered)

- **Given** the backend is starting on a JDK < 21 (NightMonkeys' MRJAR has unregistered itself)
- **When** `HeicSupportCheck.@PostConstruct` runs
- **Then** `isHeicAvailable()` returns `false`
- **And** a `WARN` log line is emitted noting the missing reader and pointing to the JDK version requirement
- **And** the application context still starts

Note: the JDK-< 21 case should not occur in practice because `maven-enforcer-plugin` blocks the build at JDK < 21 — this scenario covers the defense-in-depth case where someone runs the JAR on a downgraded JRE.

## Backend — rejected content types

### Existing rejection list extends to AVIF / TIFF / BMP / SVG / GIF / PDF

- **Given** an upload with `Content-Type` set to any of: `image/gif`, `image/svg+xml`, `image/bmp`, `image/tiff`, `image/avif`, `application/pdf`
- **When** the client posts it (size ≤ 2 MB)
- **Then** the response is `400 BAD REQUEST` for every case
- **And** the response message indicates JPEG, PNG, WebP, and HEIC are the only accepted types
- **And** the contact's `photo` column is not modified

### Missing content type is rejected

- **Given** an upload with no `Content-Type` header on the file part
- **When** the client uploads it
- **Then** the response is `400 BAD REQUEST`

## Backend — display path

### GET after a HEIC upload returns JPEG

- **Given** a contact whose photo was uploaded as HEIC (and transcoded server-side)
- **When** the client requests `GET /api/contacts/{id}/photo`
- **Then** the response `Content-Type` is `image/jpeg`
- **And** the body decodes to a `BufferedImage` of the original HEIC's (post-orientation) dimensions

### GET after a WebP upload returns JPEG

- **Given** a contact whose photo was uploaded as WebP
- **When** the client requests `GET /api/contacts/{id}/photo`
- **Then** the response `Content-Type` is `image/jpeg`

## Frontend — file input

### File input accepts HEIC and WebP in addition to JPEG and PNG

- **Given** the contact form is open
- **When** the user opens the file picker
- **Then** the input's `accept` attribute includes all of `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`

### Client-side guard accepts HEIC

- **Given** the user has selected a `.heic` file ≤ 2 MB
- **When** the file change handler runs
- **Then** no error message is shown
- **And** the selected file is queued for upload

### Client-side guard accepts WebP

- **Given** the user has selected a `.webp` file ≤ 2 MB
- **When** the file change handler runs
- **Then** no error message is shown
- **And** the selected file is queued for upload

### Client-side guard rejects formats outside the new allowlist

- **Given** the user selects a file of MIME type `image/gif`, `image/svg+xml`, `image/bmp`, `image/avif`, or `application/pdf`
- **When** the file change handler runs
- **Then** an inline error message is shown using the updated i18n key (mentioning JPEG, PNG, WebP, and HEIC)
- **And** the file is not queued for upload
- **And** no network request is made

### Client-side size guard still rejects > 2 MB regardless of format

- **Given** the user selects a JPEG, PNG, WebP, or HEIC file > 2 MB
- **When** the file change handler runs
- **Then** the existing size-error message is shown
- **And** no upload is initiated

## Internationalization

### German translation lists all four formats

- **Given** the user has selected an invalid file in the German UI
- **Then** the error message text reads "Bitte eine JPEG-, PNG-, WebP- oder HEIC-Datei wählen." (or equivalent wording that lists the four allowed formats)

### English translation lists all four formats

- **Given** the user has selected an invalid file in the English UI
- **Then** the error message text reads "Please choose a JPEG, PNG, WebP, or HEIC file." (or equivalent)

## Regressions to guard against

### Existing JPEG / PNG contacts still load

- **Given** a contact whose photo was uploaded under spec 013 (JPEG, byte-for-byte stored) or spec 101 (PNG, transcoded to JPEG)
- **When** the user opens the detail view
- **Then** the photo loads and renders identically to before

### Company-logo upload is unaffected

- **Given** the user uploads a JPEG, PNG, or SVG company logo (the existing logo path)
- **When** the upload completes
- **Then** the logo path behaves exactly as today — no transcoding, no new MIME types accepted there
- **And** the logo display in list and detail views is unchanged

### Company-logo upload of HEIC is still rejected

- **Given** the user attempts to upload an `image/heic` company logo
- **When** the upload is submitted
- **Then** the existing logo-path validation rejects it (this spec deliberately does NOT extend the logo path; the asymmetry is tracked in `TODO.md` as a follow-up)

### Maven build fails on JDK 20 or older

- **Given** a developer's local machine has Maven invoked with JDK 20
- **When** they run `./mvnw clean package`
- **Then** the build fails at the `maven-enforcer-plugin:enforce` step with the configured message ("NightMonkeys imageio-heif requires JDK 21+; build aborted.")
- **And** no JAR is produced

### `/api/contacts/{id}/photo` content-type is always `image/jpeg` regardless of upload format

- **Given** any contact with a photo uploaded under spec 013, 101, or 102
- **When** `GET /api/contacts/{id}/photo` is called
- **Then** the response `Content-Type` is always `image/jpeg`
