# Design: Contact photo PNG support (server-side transcode to JPEG)

## GitHub Issue

[#24](https://github.com/OpenElementsLabs/open-crm/issues/24)

## Summary

Contact photos today accept only `image/jpeg`. The team frequently has portraits as PNG (screenshots, downloads, cropped images) and converts to JPEG locally before uploading — needless friction. This spec adds PNG to the accepted upload types and resolves the storage-size concern by **transcoding PNG → JPEG on the server** before persisting. The database column continues to hold JPEG bytes only, with `photo_content_type = "image/jpeg"` for every contact photo regardless of the original upload format. The 2 MB pre-transcode size cap from spec 013 stays.

Company logos (which already accept PNG/SVG/JPEG natively and store the original bytes) are not touched.

## Goals

- Accept `image/png` uploads in `POST /api/contacts/{id}/photo` end-to-end (client and server).
- Always store contact photos as JPEG bytes — the DB never holds PNG for contacts.
- Keep the 2 MB upload cap exactly where it is today (pre-transcode, on the raw upload bytes).
- Strip EXIF/GPS as a side effect for the PNG path (privacy-positive); keep the JPEG path byte-for-byte stored as today.
- No new runtime dependencies — JDK's `javax.imageio.ImageIO` covers PNG decode and JPEG encode.
- No database migration.

## Non-goals

- Adding SVG, HEIC, WebP, AVIF, or any other input format. A follow-up spec can pick these up if concrete need arises — for the OE-CRM team today, PNG covers the workflow.
- Lifting the 2 MB cap. PNGs > 2 MB still must be compressed locally — accepted trade-off (see Open Questions for the deferred "lift cap post-transcode" idea).
- Symmetric EXIF stripping for JPEG uploads. JPEGs continue to retain EXIF (including GPS) for now. GDPR-hardening can come later as a dedicated spec.
- Configurable background color for alpha flattening. White (`#FFFFFF`) is hardcoded.
- Image resizing or thumbnail generation.
- Refactoring `ContactService` to use `ImageData.of(file)` (the spring-services helper). The transcoding requirement makes that helper insufficient and the refactor would broaden scope without clear payoff.
- Changes to company logos.

## Technical approach

### 1. Allowed upload formats

| Input `Content-Type` | Backend action | Stored bytes | Stored `photo_content_type` |
|---|---|---|---|
| `image/jpeg` | Pass through unchanged | Original JPEG bytes | `image/jpeg` |
| `image/png` | Decode + composite-on-white + JPEG-encode | Transcoded JPEG bytes | `image/jpeg` |
| anything else | Reject `400 BAD REQUEST` | — | — |

The 2 MB cap applies to the **uploaded** bytes, before any transcoding work happens.

### 2. Backend changes

**`ContactService.uploadPhoto(UUID id, byte[] bytes, String contentType)`** (currently at `backend/src/main/java/com/openelements/crm/contact/ContactService.java`):

The pseudo-flow after the change:

```java
public void uploadPhoto(UUID id, byte[] bytes, String contentType) {
  if (bytes.length > ImageData.MAX_IMAGE_SIZE) {
    throw new ResponseStatusException(BAD_REQUEST, "Photo exceeds 2 MB");
  }

  byte[] storedBytes;
  String storedContentType;
  switch (contentType) {
    case "image/jpeg" -> {
      storedBytes = bytes;
      storedContentType = "image/jpeg";
    }
    case "image/png" -> {
      storedBytes = ContactPhotoTranscoder.pngToJpeg(bytes);
      storedContentType = "image/jpeg";
    }
    default -> throw new ResponseStatusException(
        BAD_REQUEST, "Only JPEG and PNG are accepted");
  }

  // existing persist call
  contactRepository.savePhoto(id, storedBytes, storedContentType);
}
```

The explicit 2 MB check is added on the raw upload bytes because the current code path does not flow through `ImageData` (which would enforce it). Adding the check at the entry of `uploadPhoto` makes the cap explicit and consistent across both branches.

**Rationale for the early size check:** B7 of the grill resolved that the 2 MB applies pre-transcode. If a 5 MB PNG comes in, we reject it before doing CPU work on it. This protects against DoS via large-but-compressible PNGs and matches today's contract.

**New helper class `ContactPhotoTranscoder`** in `backend/src/main/java/com/openelements/crm/contact/`:

```java
final class ContactPhotoTranscoder {
  private static final Color BACKGROUND = Color.WHITE;
  private static final float JPEG_QUALITY = 0.9f;

  private ContactPhotoTranscoder() { /* utility */ }

  static byte[] pngToJpeg(byte[] pngBytes) {
    // 1. Read PNG into a BufferedImage via ImageIO.read.
    //    If null (unreadable), throw ResponseStatusException(BAD_REQUEST).
    // 2. Create a new RGB BufferedImage of the same dimensions
    //    (TYPE_INT_RGB — no alpha channel).
    // 3. Fill with BACKGROUND, then drawImage() the PNG on top
    //    (composites alpha pixels over white).
    // 4. Encode as JPEG with quality 0.9 via ImageWriter + IIOImage,
    //    output to ByteArrayOutputStream.
    // 5. Return the byte[].
  }
}
```

Package-private, lives next to `ContactService`, single static method. No `ImageData` involvement — the transcoder produces raw JPEG bytes that `ContactService` then hands to the persistence layer like any other JPEG upload.

**Rationale for keeping the helper local** (B10 of the grill): one PR in one repo, no `spring-services` release coordination. If a second consumer ever needs the same logic, extraction becomes its own spec. Today there is no second consumer.

**Rationale for `0.9` JPEG quality:** typical default for photo-quality re-encodes. Avatar-sized portraits do not visibly suffer at 0.9. Hardcoded — not a tuning knob today.

**Error handling:**

- Size > 2 MB → `400 BAD REQUEST`, message `"Photo exceeds 2 MB"` (the existing English error path; localized client-side).
- Content-type not in {`image/jpeg`, `image/png`} → `400`, message `"Only JPEG and PNG are accepted"`.
- PNG decode fails (`ImageIO.read` returns `null` or throws `IIOException`) → `400`, message `"Could not decode PNG"`.
- JPEG encode failure (extremely unlikely in practice) → `500` with the underlying `IOException` propagated; this is a server bug, not user input.

### 3. Frontend changes

**`frontend/src/components/contact-form.tsx`**:

- `ALLOWED_CONTACT_PHOTO_TYPES` (line 11 area) changes from `["image/jpeg"]` to `["image/jpeg", "image/png"]`.
- The file input `accept` attribute becomes `"image/jpeg,image/png"`.
- Client-side guard against other types uses the same array.
- 2 MB client-side check stays as-is.
- Error message key in i18n is reworded for the new "JPEG or PNG" wording.

**`frontend/src/lib/i18n/de.ts` and `en.ts`**:

The existing key (currently something like "Bitte wählen Sie eine JPEG-Datei" / "Please choose a JPEG file") is updated to list both formats:

- `de`: "Bitte eine JPEG- oder PNG-Datei wählen."
- `en`: "Please choose a JPEG or PNG file."

The size-error key is unchanged.

### 4. Display path

No changes. The detail view continues to render `<img src={getContactPhotoUrl(contact.id)} />`, and the backend's GET endpoint already returns whatever bytes + content-type are stored. Because every stored photo is now JPEG, the served `Content-Type` header is always `image/jpeg` for contacts. The existing `object-cover rounded-full` styling applies as before; transparency questions are moot because the stored bytes have no alpha channel.

### 5. Database

No migration. The `photo` column already stores arbitrary bytes (`BYTEA`); the `photo_content_type VARCHAR(50)` already holds the MIME string. Existing JPEG contact photos remain valid and untouched.

### 6. Tests

Backend (`backend/src/test/.../contact/`):

- Happy path JPEG (existing) — must still pass.
- Happy path PNG — uploads a small test PNG, asserts the entity now holds JPEG bytes (`photo_content_type == "image/jpeg"`) and that the bytes are non-empty and decode back to a `BufferedImage` of the same dimensions.
- Transparent PNG — uploads a PNG with an alpha channel; asserts the stored JPEG decodes to a BufferedImage and that a sample pixel in a previously-transparent region is white (`(255, 255, 255)`).
- Oversized — uploads a 3 MB file (either JPEG or PNG); asserts 400 and that no DB write occurred.
- Wrong content type — uploads `image/gif`, `image/webp`, `image/svg+xml`; asserts 400 for each.
- Malformed PNG — uploads `Content-Type: image/png` with garbage bytes; asserts 400.

Test fixtures are tiny generated PNGs/JPEGs (a few hundred bytes each), kept in `backend/src/test/resources/contact-photo/`.

Frontend (`frontend/src/components/__tests__/`):

- The existing contact-form file-input test gains a PNG case (the guard accepts `image/png`).
- A test verifies the i18n error text updates appear correctly when an unsupported type is selected.

### 7. Implementation order

The change is small enough for a single PR:

1. Backend: add `ContactPhotoTranscoder`, update `ContactService.uploadPhoto`, add backend tests + fixtures.
2. Frontend: extend the allowlist, update i18n, update component test.
3. Manual verification: upload JPEG (still works), upload a transparent PNG (becomes a JPEG with white background), upload a 3 MB PNG (rejected), upload a `.gif` (rejected).

### 8. Performance

PNG decoding + alpha composite + JPEG re-encode for a 2 MB PNG takes single-digit milliseconds on commodity hardware. Contact-photo uploads are infrequent (single-user, single-upload events). No measurable production impact expected.

## Dependencies

- JDK's `javax.imageio.ImageIO` (already on the classpath, no Maven changes).
- No `spring-services` changes — the transcoder lives locally in `open-crm`.
- No frontend dependency changes.

## Security considerations

- Inputs pass through `ImageIO.read`, which is implemented in the JDK and well-hardened against typical malformed-image issues. The trusted-user model from spec 013 remains the primary defense; the transcode step is a fortunate side effect that also acts as a format validator (B5 of the grill).
- EXIF stripping happens for PNG uploads as a side effect — a privacy improvement for that path. JPEG uploads remain EXIF-preserving (acknowledged asymmetry from B9; a future GDPR-hardening spec can fix this).
- The 2 MB pre-transcode cap protects against very large uploads driving CPU/memory exhaustion via ImageIO decoding. Without this cap, a malicious user could submit a small-compressed PNG that decompresses to a gigantic BufferedImage (the "PNG bomb" attack). The 2 MB pre-transcode bound makes that impractical.

## GDPR / personal-data note

Contact photos are personal data. This spec does not change the legal basis or retention model — it only changes *which formats can be uploaded* and *whether EXIF is preserved* for the new format. Net effect on the contact-photo GDPR posture: small improvement (PNG uploads no longer carry EXIF/GPS into the DB). No data-subject-rights or export-format change.

## Open questions

- **Lift the 2 MB cap post-transcode (B7 follow-up):** the current spec keeps the 2 MB on raw upload bytes, meaning PNGs > 2 MB still need local compression. If user friction with this trade-off appears in practice, a follow-up spec can change to "10 MB pre-transcode hard cap, 2 MB post-transcode storage cap". Not in this spec.
- **EXIF-strip for JPEG path too (B9 follow-up):** would require either a re-encode (quality loss) or a metadata-extractor library. Deferred until GDPR posture review demands it.
- **Extract `ContactPhotoTranscoder` to `spring-services`:** if a second OE app or the company-logo path ever needs the same logic, it becomes its own extraction spec.
