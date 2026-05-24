# Design: Contact photo HEIC & WebP support (server-side transcode to JPEG)

## Prerequisite

**Spec 101 (contact-photo-png) must be merged first.** This spec extends the `ContactPhotoTranscoder` introduced by 101 with two additional static methods (`heicToJpeg`, `webpToJpeg`) and the rejection-list / control-flow in `ContactService.uploadPhoto` introduced by 101.

## Summary

Spec 101 added PNG support to contact-photo uploads, transcoded server-side to JPEG. This spec adds two more input formats that real-world users keep hitting:

- **HEIC** — the default iPhone format; users uploading their own portrait from a phone today get rejected.
- **WebP** — modern browsers' "Save image as" produces WebP from many sites; same friction.

Both are decoded server-side and re-encoded as JPEG before persistence. The DB still only holds JPEG bytes with `photo_content_type = "image/jpeg"`. The 2 MB pre-transcode cap stays.

HEIC requires a native library (`libheif` + `libheif-plugin-libde265`) that is not on the current Alpine runtime image — therefore this spec also **switches the backend Docker base image from Alpine to Debian (`eclipse-temurin:21-jre`)**. WebP needs no native dependency.

Company logos remain out of scope (separate code path, deferred — see `TODO.md`).

## Goals

- Accept `image/heic`, `image/heif`, and `image/webp` uploads in `POST /api/contacts/{id}/photo` end-to-end.
- Always store contact photos as JPEG bytes — the DB never holds HEIC or WebP for contacts.
- Apply EXIF `Orientation` during decode for HEIC and WebP so re-encoded JPEGs are upright (iPhone HEICs are routinely rotated 90°).
- Animated WebPs are accepted; only the first frame is transcoded and stored. No error.
- Embedded ICC color profiles are not preserved — the JPEG output is sRGB.
- Surface HEIC availability via a startup probe (`HeicSupportCheck`) so a broken deploy (`libheif` missing) is visible in logs.
- Enforce JDK ≥ 21 at build time via `maven-enforcer-plugin` (NightMonkeys' Multi-Release JAR silently disables itself under older JDKs).
- No database migration.

## Non-goals

- AVIF, TIFF, BMP, GIF, SVG, or any other input format. Same rule as 101: pick up if concrete demand appears.
- WebP **output** — we transcode to JPEG, no need for write-side WebP support. (Hence TwelveMonkeys, not NightMonkeys, for WebP.)
- Lifting the 2 MB pre-transcode cap.
- Configurable background color, JPEG quality knob, or output-image resizing.
- Symmetric HEIC/WebP support for company logos. Separate code path (`CompanyService.updateLogo` via `ImageData.of(file)`), tracked in `TODO.md` as a follow-up spec.
- Actuator health indicator for HEIC support — not in v1; logs cover detection. A visual admin-page status row is tracked in `TODO.md`.
- Bytedeco `libheif-platform` (native lib bundled in JAR) — only relevant if avoiding system packages mattered; `apt-get` on Debian is fine here.
- Honoring HEIC's editable depth-/auxiliary-image data — we transcode the primary image only.

## Technical approach

### 1. Allowed upload formats

| Input `Content-Type` | Backend action | Stored bytes | Stored `photo_content_type` |
|---|---|---|---|
| `image/jpeg` | Pass through unchanged (spec 013) | Original JPEG bytes | `image/jpeg` |
| `image/png` | Decode + composite-on-white + JPEG-encode (spec 101) | Transcoded JPEG bytes | `image/jpeg` |
| `image/webp` | Decode + EXIF-rotate + composite-on-white (if alpha) + JPEG-encode | Transcoded JPEG bytes | `image/jpeg` |
| `image/heic`, `image/heif` | Decode (libheif) + EXIF-rotate + JPEG-encode | Transcoded JPEG bytes | `image/jpeg` |
| anything else | Reject `400 BAD REQUEST` | — | — |

Two HEIC MIME types are accepted because clients vary in what they send (`image/heic` is most common; `image/heif` is the parent format and seen occasionally).

The 2 MB cap applies to the **uploaded** bytes, before any transcoding work happens — unchanged from spec 101.

### 2. Maven dependencies

`backend/pom.xml`:

```xml
<!-- HEIC decoding via libheif. Multi-Release JAR — activates under JDK 21+. -->
<dependency>
  <groupId>com.github.gotson.nightmonkeys</groupId>
  <artifactId>imageio-heif</artifactId>
  <version>1.1.0</version>
</dependency>

<!-- WebP decoding, pure-Java. No native dependency. -->
<dependency>
  <groupId>com.twelvemonkeys.imageio</groupId>
  <artifactId>imageio-webp</artifactId>
  <version>3.12.0</version>
</dependency>
```

Plus a build-time JDK version enforcement to prevent NightMonkeys' silent deactivation on JDK downgrade:

```xml
<plugin>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>
      <id>enforce-jdk-21</id>
      <goals><goal>enforce</goal></goals>
      <configuration>
        <rules>
          <requireJavaVersion>
            <version>[21,)</version>
            <message>NightMonkeys imageio-heif requires JDK 21+; build aborted.</message>
          </requireJavaVersion>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 3. Backend changes

**`ContactPhotoTranscoder`** (introduced by spec 101) gains two new package-private static methods:

```java
static byte[] webpToJpeg(byte[] webpBytes) {
  // 1. ImageIO.read(...) the WebP via TwelveMonkeys reader.
  //    If null/IIOException → ResponseStatusException(BAD_REQUEST,
  //    "Could not decode WebP").
  // 2. Read EXIF "Orientation" tag (if present) from the WebP container
  //    via the TwelveMonkeys metadata API; apply the corresponding rotation
  //    /flip to the BufferedImage. (See helper applyExifOrientation below.)
  // 3. If the image has an alpha channel, composite onto white (TYPE_INT_RGB,
  //    fill BACKGROUND, drawImage) — same approach as pngToJpeg from spec 101.
  // 4. Encode as JPEG at quality 0.9 via ImageWriter + IIOImage,
  //    sRGB color space. ICC profile is intentionally not propagated.
  // 5. Return byte[].
}

static byte[] heicToJpeg(byte[] heicBytes) {
  // Identical structure to webpToJpeg, but the reader is NightMonkeys'
  // imageio-heif. HEIC files are always opaque (no alpha) — the
  // composite-on-white step is skipped, but EXIF orientation IS applied.
  // If HeicSupportCheck reports !heicAvailable, throw
  // ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "HEIC support is not
  // available in this deployment") *before* attempting decode.
}

private static BufferedImage applyExifOrientation(BufferedImage src,
                                                  int orientation) {
  // Standard EXIF orientation mapping (1=normal, 3=180°, 6=90° CW, 8=270° CW,
  // 2/4/5/7=mirrored variants). Returns a new BufferedImage.
}
```

**Animated WebP:** TwelveMonkeys' `ImageIO.read(...)` returns only the first frame for animated WebPs. We do not invoke the `ImageReader` API explicitly to enumerate frames, so the "first frame only" behavior is automatic. No error, no warning — the user gets a static avatar of the first animation frame, which is the sane fallback.

**`ContactService.uploadPhoto(...)`** extends spec 101's `switch`:

```java
switch (contentType) {
  case "image/jpeg" -> { storedBytes = bytes; storedContentType = "image/jpeg"; }
  case "image/png"  -> { storedBytes = ContactPhotoTranscoder.pngToJpeg(bytes);  storedContentType = "image/jpeg"; }
  case "image/webp" -> { storedBytes = ContactPhotoTranscoder.webpToJpeg(bytes); storedContentType = "image/jpeg"; }
  case "image/heic", "image/heif" -> {
    storedBytes = ContactPhotoTranscoder.heicToJpeg(bytes);
    storedContentType = "image/jpeg";
  }
  default -> throw new ResponseStatusException(
      BAD_REQUEST, "Only JPEG, PNG, WebP, and HEIC are accepted");
}
```

**`HeicSupportCheck`** — new `@Component` in `com.openelements.crm.contact`:

```java
@Component
public class HeicSupportCheck {
  private static final Logger log = LoggerFactory.getLogger(HeicSupportCheck.class);
  private volatile boolean heicAvailable = false;

  public boolean isHeicAvailable() { return heicAvailable; }

  @PostConstruct
  void verifyHeicSupport() {
    // Level 1 — reader registered?
    boolean readerRegistered = Arrays.stream(ImageIO.getReaderFormatNames())
        .anyMatch(n -> n.equalsIgnoreCase("heif") || n.equalsIgnoreCase("heic"));
    if (!readerRegistered) {
      log.warn("HEIC: no ImageIO reader registered. Check JDK 21+ and the "
          + "imageio-heif dependency. HEIC uploads will be rejected with 415.");
      return;
    }
    // Level 2 — real decode test.
    try (InputStream sample = getClass().getResourceAsStream("/heic-probe/sample.heic")) {
      if (sample == null) {
        log.error("HEIC: probe sample /heic-probe/sample.heic is missing on classpath.");
        return;
      }
      BufferedImage img = ImageIO.read(sample);
      if (img != null) {
        heicAvailable = true;
        log.info("HEIC: support verified (decoded {}x{} probe image).",
            img.getWidth(), img.getHeight());
      } else {
        log.error("HEIC: reader registered but decode returned null. "
            + "Install libheif1 + libheif-plugin-libde265 in the runtime image. "
            + "HEIC uploads will be rejected with 415.");
      }
    } catch (UnsatisfiedLinkError | Exception e) {
      log.error("HEIC: support check failed — {}. Install libheif1 + "
          + "libheif-plugin-libde265 in the runtime image. HEIC uploads will be "
          + "rejected with 415.", e.toString());
    }
  }
}
```

`ContactPhotoTranscoder.heicToJpeg(...)` consults this bean (via constructor injection in a small `HeicTranscoderService` wrapper, since the transcoder itself is a utility class) and short-circuits with 415 if `heicAvailable == false`.

**Error handling additions:**

- HEIC upload while `heicAvailable == false` → `415 UNSUPPORTED_MEDIA_TYPE`, message `"HEIC support is not available in this deployment"`.
- HEIC decode failure at runtime (corrupt file, unexpected variant) → `400 BAD REQUEST`, message `"Could not decode HEIC"`.
- WebP decode failure → `400`, message `"Could not decode WebP"`.
- All existing errors from spec 013/101 remain.

### 4. Dockerfile changes (Alpine → Debian)

`backend/Dockerfile` is rewritten from the current Alpine base to Debian, because `libheif` is needed at runtime and is best installed via `apt-get`:

```dockerfile
# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Native libraries for HEIC decoding.
# libheif1                 -> the shared library
# libheif-plugin-libde265  -> the HEVC decoder plugin (required to decode HEIC)
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libheif1 \
        libheif-plugin-libde265 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
```

Layer ordering: the `apt-get` instruction sits before the JAR `COPY` so it benefits from Docker's layer cache and only re-runs when the apt-get list itself changes.

The `--enable-native-access=ALL-UNNAMED` flag silences the JDK 21 warning about Foreign Function & Memory API access used by NightMonkeys.

Image-size impact: ~80 MB (Alpine JRE) → ~250 MB (Debian JRE + libheif). Acceptable per the operator (no deploy-time concerns).

### 5. Frontend changes

**`frontend/src/components/contact-form.tsx`**:

- `ALLOWED_CONTACT_PHOTO_TYPES` is extended to `["image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"]`.
- The file input `accept` attribute becomes `"image/jpeg,image/png,image/webp,image/heic,image/heif"`.
- The client-side guard uses the same array.
- 2 MB client-side size check unchanged.
- i18n key for the format-error message is reworded.

**`frontend/src/lib/i18n/de.ts` and `en.ts`**:

- `de`: "Bitte eine JPEG-, PNG-, WebP- oder HEIC-Datei wählen."
- `en`: "Please choose a JPEG, PNG, WebP, or HEIC file."

The size-error key is unchanged.

No changes to the display path — `<img src={getContactPhotoUrl(...)}>` still works because every stored photo is JPEG with `Content-Type: image/jpeg`.

### 6. Database

No migration. The `photo` (`BYTEA`) and `photo_content_type` (`VARCHAR(50)`) columns are already in place from spec 013, work unchanged.

### 7. Tests

**Fixture inventory.** The user has placed real-world fixtures under `backend/src/test/resources/images/`:

| File | Bytes | Properties |
|---|---|---|
| `images/test.jpeg` | 1.4 MB | iPhone 15 Pro JPEG with EXIF (orientation 1, GPS data) |
| `images/test.png` | 870 KB | 800×800 RGB, opaque (no alpha channel) |
| `images/test.webp` | 565 KB | VP8 lossy, 3840×5783, opaque |
| `images/test.heic` | 1.0 MB | iPhone HEIF/HEVC, opaque |

Each is under 2 MB so each is **accepted** by the size cap and exercises the full decode → transcode → store path.

Several edge-case fixtures are **not yet present** and are tracked in `TODO.md` ("HEIC- und WebP-Testfixtures bereitstellen + Tests aktivieren"). Tests that require them ship `@Disabled` with the message `"Awaiting fixture — see TODO.md: HEIC- und WebP-Testfixtures bereitstellen"`. Missing fixtures:

- **EXIF-orientation HEIC** (e.g. `iphone-portrait-orient-6.heic`) — for the "decoded upright" test.
- **Transparent PNG / lossless WebP + alpha** — for the "flatten on white" tests.
- **Animated WebP** — for the "silent first frame" test.
- **Oversized fixtures (> 2 MB) per format** — for the size-cap test. The existing four fixtures are all under 2 MB so a separate `oversize.heic` / `oversize.webp` / `oversize.png` is required.
- **Probe sample `heic-probe/sample.heic`** (< 10 KB target) — bundled into the production JAR for `HeicSupportCheck`. The existing 1 MB `test.heic` is too large to ship inside the production artifact.

**Malformed fixtures** are generated programmatically at test time (random byte arrays with the matching `Content-Type`); no disk asset needed.

**Test matrix:**

| Scenario | Active in CI? |
|---|---|
| Existing JPEG / PNG cases from spec 101, using `images/test.jpeg` and `images/test.png` | Active |
| JPEG passthrough preserves EXIF including GPS (uses `images/test.jpeg`) | Active (spec 101 contract; verified here as a regression) |
| Happy-path opaque HEIC upload (uses `images/test.heic`) | Active |
| Happy-path lossy opaque WebP upload (uses `images/test.webp`) | Active |
| HEIC upload when `heicAvailable = false` → 415 (uses a mock `HeicSupportCheck`) | Active |
| Rejected MIME types (`image/gif`, `image/bmp`, `image/svg+xml`, `image/avif`, `application/pdf`) → 400 | Active |
| Malformed HEIC bytes → 400 (programmatic random bytes) | Active |
| Malformed WebP bytes → 400 (programmatic random bytes) | Active |
| HEIC with EXIF orientation 6 (90° CW) is decoded upright | `@Disabled` (awaits fixture) |
| HEIC > 2 MB → 400 | `@Disabled` (awaits fixture) |
| WebP > 2 MB → 400 | `@Disabled` (awaits fixture) |
| PNG > 2 MB → 400 (also spec 101) | `@Disabled` (awaits fixture) |
| Lossless WebP + alpha → flattened on white | `@Disabled` (awaits fixture) |
| Transparent PNG → flattened on white (also spec 101) | `@Disabled` (awaits fixture) |
| Animated WebP → first frame stored, no error | `@Disabled` (awaits fixture) |
| `HeicSupportCheck` reports `heicAvailable = true` after startup probe | `@Disabled` (awaits small probe sample) |

Frontend (`frontend/src/components/__tests__/`):

- The existing contact-form file-input test gains HEIC and WebP cases (the guard accepts both).
- The i18n test verifies the updated wording renders for both languages.

### 8. Implementation order

1. **Pre-flight:** confirm spec 101 is merged.
2. Add the two Maven dependencies + the `maven-enforcer-plugin` config.
3. Rewrite the Dockerfile (Alpine → Debian, add `libheif` packages, swap ENTRYPOINT).
4. Add `HeicSupportCheck` component + probe sample (`src/main/resources/heic-probe/sample.heic`) — placeholder sample for now, fixture-quality probe later via the same TODO entry.
5. Extend `ContactPhotoTranscoder` with `webpToJpeg`, `heicToJpeg`, and the `applyExifOrientation` helper.
6. Extend `ContactService.uploadPhoto` switch.
7. Add test class scaffolding with all scenarios listed in §7, with `@Disabled` annotations on the fixture-dependent ones.
8. Frontend: extend `ALLOWED_CONTACT_PHOTO_TYPES`, update i18n.
9. Manual verification: upload a HEIC from an iPhone, a WebP from a browser, an oversized HEIC, an `image/gif`, and confirm 415 behavior by temporarily uninstalling `libheif` in a local container.

### 9. Performance

HEIC decode via libheif: tens to low hundreds of ms for a typical 4–8 MP iPhone photo (≤ 2 MB compressed). WebP decode is similar. Plus EXIF rotation (≤ 5 ms) and JPEG re-encode (≤ 20 ms). Contact-photo uploads are infrequent, single-user events. No production capacity impact expected.

## Dependencies

- `com.github.gotson.nightmonkeys:imageio-heif:1.1.0` (HEIC decode; FFM API; JDK 21+).
- `com.twelvemonkeys.imageio:imageio-webp:3.12.0` (WebP decode; pure Java).
- `maven-enforcer-plugin` (build-time JDK enforcement).
- Native (in Docker image): `libheif1`, `libheif-plugin-libde265` via `apt-get`.
- No `spring-services` changes — transcoder logic remains local in `open-crm`.

## Security considerations

- All decodes flow through `javax.imageio.ImageIO`, which delegates to NightMonkeys/TwelveMonkeys readers. Both are actively maintained; TwelveMonkeys' WebP is pure Java and well-tested. NightMonkeys uses libheif via the FFM API — the same library used by Apple, Adobe, etc.
- The 2 MB pre-transcode cap from spec 013/101 stays. Without it, a small-compressed HEIC could decode to a gigantic in-memory bitmap (the "image bomb" attack). The cap protects against that.
- ICC color profiles are not propagated — also protects against profile-based exploits seen historically in image processing libraries.
- EXIF metadata is consumed (for orientation) but is not propagated to the output JPEG. This is a privacy-positive side effect: GPS/camera metadata in HEIC/WebP uploads is dropped at the transcode boundary, mirroring spec 101's PNG behavior.

## GDPR / personal-data note

Same posture as spec 101: contact photos remain personal data; this spec changes only *which input formats are accepted* and *whether EXIF survives* for the new formats. Net effect on GDPR posture: small improvement (HEIC/WebP uploads no longer carry EXIF/GPS into the DB, same as PNG). No data-subject-rights or export-format change.

## Operations note: silent regressions

If a future deploy ships without `libheif` (e.g., a Dockerfile edit, a base-image swap, an `apt-get` cache miss), the `HeicSupportCheck` will log at `ERROR` level at startup and `heicAvailable` will be `false`. HEIC uploads then return 415 with a clear message. WebP and JPEG/PNG continue to work — the regression is partial, not total.

Operator detection is **log-based in v1**. A visual admin-page status indicator is tracked as a follow-up in `TODO.md` ("HEIC-Support-Status im Admin-Bereich anzeigen"). The reasoning: a one-line WARN at startup is enough discipline for a team that reviews TODO/log discipline at sprint planning; a UI indicator is hardening, not a v1 requirement.

## Open questions

- **Probe sample provenance** — `heic-probe/sample.heic` needs to be a real (tiny) HEIC. Tracked via the existing `TODO.md` entry "HEIC- und WebP-Testfixtures bereitstellen + Tests aktivieren". The implementation can land with a placeholder probe (e.g., decoding any small HEIC available locally to the implementer) and the fixture TODO closes the loop.
- **Company-logo symmetry** — explicitly out of scope; the UX inconsistency (HEIC works for contact photos but not for logos) is acknowledged. Tracked via `TODO.md` "HEIC- und WebP-Support für Company-Logos".
- **Actuator health endpoint for HEIC** — deferred; tracked via `TODO.md` "HEIC-Support-Status im Admin-Bereich anzeigen" (the same indicator could feed both an admin UI and an Actuator indicator if Actuator is added later).
- **Lift the 2 MB cap post-transcode** — same deferred question as spec 101, no change here.
