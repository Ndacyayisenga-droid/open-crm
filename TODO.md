# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (firstName, lastName, email, companyId, language, sort) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Currently, only `companyId` is read from the URL
on initial load, and the filter dropdown does not reflect the URL-driven filter value.

## H2 Tests: Switch to Flyway + validate

Switch H2-based tests from `ddl-auto: create-drop` (Flyway disabled) to Flyway-managed schema creation with
`ddl-auto: validate`. This would catch migration/entity mismatches without needing Testcontainers. Requires adding
`flyway-database-h2` as a test dependency. All 5 existing migrations are H2-compatible.

**Context:** Identified during the grill session for Spec 018. Prerequisite: Spec 018 (component tests) should be
completed first.

## Testcontainers Integration Tests

Add Testcontainers-based integration tests that run against a real PostgreSQL database via a separate Spring profile.
This catches PostgreSQL-specific issues that H2 cannot reproduce.

**Context:** Identified during the grill sessions for Spec 017 and 018. Prerequisite: H2 Flyway + validate switch should
be done first.

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec.

## Admin-View mit Bearer Token für Swagger

Eine Admin-Seite im Frontend, die den aktuellen Bearer Token (Access Token) des eingeloggten Users anzeigt. So kann man
den Token einfach kopieren und in Swagger UIs "Authorize"-Dialog einfügen, ohne die Browser-DevTools bemühen zu müssen.

**Context:** Identifiziert nach Implementierung von Specs 048/049. Swagger UI nutzt einen manuellen
Bearer-Token-Paste-Workflow.

## Comment Author: Umstellung auf User-FK

Das `author`-Feld (String) in Kommentaren soll durch einen Fremdschlüssel auf die neue User-Entity ersetzt werden. Damit
werden Kommentare einem konkreten User zugeordnet, Namensänderungen automatisch reflektiert und GDPR-Löschanträge sauber
umsetzbar.

**Context:** Identifiziert während der Grill-Session für Spec 065 (User Entity). Voraussetzung: Spec 065 muss zuerst
implementiert sein.

## Webhook Integration Tests

Add integration tests for webhook firing that use an embedded HTTP server (e.g. MockWebServer or WireMock) to verify
that webhook calls are actually sent with the correct payload, headers, and timing. Unit tests with mocked HTTP client
are part of the initial implementation — these integration tests go beyond that.

**Context:** Identified during the grill session for Spec 075 (Webhook Support). Prerequisite: Spec 075 must be
implemented first.

## Move topics to frontend lib

- User Table
- Audit table

## GDPR-Abdeckung für Updates-View (Mitarbeiter-Aktivitätstransparenz)

Die geplante „Updates"-View (Activity Feed) zeigt jedem eingeloggten Benutzer, welcher Kollege wann welche Firma/
Person/Kommentar erstellt, geändert oder gelöscht hat. Das ist eine personenbezogene Aktivitätsverfolgung von
Mitarbeitenden durch andere Mitarbeitende und benötigt eine saubere rechtliche Grundlage — z. B. eine
Betriebsvereinbarung oder eine entsprechende Klausel im AV-Vertrag, die diese Transparenz abdeckt.

**Context:** Offene Frage aus der Grill-Session zur Updates-View-Spec. Die Spec wird mit der Annahme erstellt, dass
diese Grundlage geschaffen wird; das eigentliche Dokument/Vereinbarung ist ein separater organisatorischer Schritt.

## Awesome DB Backup

der DB_Backup Container soll aufgebohrt werden und ein REST API bereistellen durch den man Backups triggern kann und
sich Backups runterladen kann.
Es soll keine FUnktionalität geben um Backups zu löschen, da die Backups automatisch nach 7 Tagen gelöscht werden.
Alles soll additiv sein.
Es soll auch keine Funktionalität geben um Backups zu planen, da die Backups automatisch alle 24 Stunden erstellt
werden.

Das Backjend kann dann darauf zugreifen und im Frontend kann man im Admin Bereich funktionen zum triggern von Backups
und den Download des letzten backups bereitstellen.

# HEIC & WebP → JPEG Conversion in Java (ImageIO + Docker)

This guide covers integrating HEIC and WebP decoding into a Spring Boot backend
using ImageIO plugins, and shipping any required native libraries inside the
Docker image.

- **HEIC** is handled by the **NightMonkeys `imageio-heif`** plugin, which needs
  the native `libheif` library.
- **WebP** can be handled either by the pure-Java **TwelveMonkeys `imageio-webp`**
  plugin (no native dependency, read-only) or by **NightMonkeys `imageio-webp`**
  (native `libwebp`, read + write). For a WebP → JPEG conversion use case,
  TwelveMonkeys is the lower-maintenance choice — see section 6.

In all cases the Java code is identical: once a plugin is on the classpath,
`ImageIO.read(...)` decodes the format and `ImageIO.write(..., "jpg", ...)`
produces the JPEG.

## How it works

NightMonkeys registers an ImageIO plugin for HEIC. Once it is on the classpath,
the standard `javax.imageio.ImageIO` API can read HEIC files transparently — no
custom decoder code is needed. The plugin uses the **Foreign Function & Memory
API (Project Panama)** instead of classic JNI, which is why it requires **JDK 21+**.

It does **not** bundle the native library. `libheif` (and, on modern distros, the
HEVC decoder plugin) must be present in the runtime image.

## 1. Maven dependency

```xml
<dependency>
    <groupId>com.github.gotson.nightmonkeys</groupId>
    <artifactId>imageio-heif</artifactId>
    <version>1.1.0</version>
</dependency>
```

The JAR is a multi-release JAR: under Java < 21 the plugin unregisters itself and
does nothing; under Java 21 it activates. Adding it is safe regardless of JDK,
but you need Java 21 at runtime for it to actually work.

## 2. Java conversion code

The same code converts any format an installed ImageIO plugin can read —
HEIC, WebP, or otherwise. Reading by `InputStream` lets ImageIO probe all
registered readers automatically.

```java
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImageToJpegConverter {

    /**
     * Converts any plugin-readable image stream (HEIC, WebP, ...) to JPEG.
     *
     * @return true on success, false if no decoder could read the input
     */
    public static boolean convertToJpeg(InputStream input, OutputStream jpegOutput)
            throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            // No registered reader could decode the input.
            // Most common cause: a required native library / plugin is missing.
            return false;
        }
        return ImageIO.write(image, "jpg", jpegOutput);
    }

    public static void convertFile(Path sourceFile, Path jpegFile) throws IOException {
        try (InputStream in = Files.newInputStream(sourceFile);
             OutputStream out = Files.newOutputStream(jpegFile)) {
            if (!convertToJpeg(in, out)) {
                throw new IOException("Could not decode image file: " + sourceFile);
            }
        }
    }
}
```

### Verifying HEIC support at startup

The `imageio-heif` plugin registers itself **even when `libheif` is missing** —
the failure only surfaces on the first real decode. A startup check should
therefore not just confirm the reader is registered, but confirm it can actually
decode. There are two levels:

- **Level 1 — reader registered?** Cheap, but weak. On JDK < 21 the plugin
  unregisters itself (reader genuinely absent). On JDK 21 with a missing
  `libheif`, the reader is still present and fails only at runtime. This check
  alone is not sufficient.
- **Level 2 — real decode test.** Decode a tiny embedded HEIC sample at startup.
  Success proves both the native library and the libde265 decoder plugin are
  present. This is the reliable check.

The component below performs both. A tiny probe HEIC is bundled in the JAR and
decoded once on startup:

```java
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

@Component
public class HeicSupportCheck {

    private static final Logger log = LoggerFactory.getLogger(HeicSupportCheck.class);

    /** True if HEIC can actually be decoded in this runtime. */
    private volatile boolean heicAvailable = false;

    public boolean isHeicAvailable() {
        return heicAvailable;
    }

    @PostConstruct
    void verifyHeicSupport() {
        // Level 1: is a reader registered at all?
        boolean readerRegistered = Arrays.stream(ImageIO.getReaderFormatNames())
                .anyMatch(n -> n.equalsIgnoreCase("heif") || n.equalsIgnoreCase("heic"));

        if (!readerRegistered) {
            log.warn("HEIC: no ImageIO reader registered — check JDK 21 and the "
                    + "imageio-heif dependency. HEIC conversion disabled.");
            return;
        }

        // Level 2: can the reader actually use the native library?
        try (InputStream sample = getClass().getResourceAsStream("/heic-probe/sample.heic")) {
            if (sample == null) {
                log.error("HEIC: probe sample /heic-probe/sample.heic not found on classpath.");
                return;
            }
            BufferedImage img = ImageIO.read(sample);
            if (img != null) {
                heicAvailable = true;
                log.info("HEIC: support verified (decoded {}x{} probe image).",
                        img.getWidth(), img.getHeight());
            } else {
                log.error("HEIC: reader registered but decode returned null — "
                        + "native libheif or the libde265 decoder plugin is missing.");
            }
        } catch (UnsatisfiedLinkError | Exception e) {
            // UnsatisfiedLinkError: native libheif cannot be loaded at all.
            // Exception: library present but decoder plugin (libde265) missing
            //            -> "No decoder available".
            log.error("HEIC: support check failed — {}. Install libheif1 + "
                    + "libheif-plugin-libde265 in the runtime image.", e.toString());
        }
    }
}
```

Notes:

- **`UnsatisfiedLinkError` must be caught separately** — it is an `Error`, not an
  `Exception`, and is thrown when `libheif` cannot be loaded at all. If only the
  libde265 plugin is missing (library loads, but no decoder), you get a normal
  `Exception` with "No decoder available" instead. The check catches both.
- **The probe sample** goes in `src/main/resources/heic-probe/sample.heic`. Use a
  deliberately tiny HEIC (a few pixels, < 1 KB). Generate it once with `heif-enc`
  or `magick input.png sample.heic` — and make sure it is a real HEIC, not a
  renamed JPEG.
- **Fail-fast vs. continue.** The example keeps the application running and just
  records `heicAvailable = false`; the conversion endpoint can query this and
  return a clean 415/503 instead of crashing on upload. If HEIC support is
  critical, throw an exception in the failure branch instead — `@PostConstruct`
  will then fail the context startup.

Note: the format name may register as `heif` or `heic` depending on the plugin
version. Reading by `InputStream` (as in section 2) sidesteps the naming
question, since ImageIO probes all readers.

### Optional: expose the check as an Actuator health indicator

Surfacing the HEIC status under `/actuator/health` lets you confirm after a
deploy that the image shipped the native libraries correctly — without uploading
a HEIC file. This is particularly handy in a Coolify setup.

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class HeicHealthIndicator implements HealthIndicator {

    private final HeicSupportCheck check;

    public HeicHealthIndicator(HeicSupportCheck check) {
        this.check = check;
    }

    @Override
    public Health health() {
        return check.isHeicAvailable()
                ? Health.up().withDetail("heic", "decodable").build()
                : Health.down().withDetail("heic", "native libheif/decoder missing").build();
    }
}
```

### JPEG quality (optional)

`ImageIO.write(..., "jpg", ...)` uses a default quality setting. If you need
control, use an `ImageWriter` with an `ImageWriteParam` and call
`setCompressionQuality(0.0f–1.0f)`. Also be aware that HEIC images from iPhones
may contain an alpha channel or non-RGB color model; if `ImageIO.write` produces
a distorted JPEG, redraw the image into a `BufferedImage.TYPE_INT_RGB` canvas
first before writing.

## 3. Dockerfile

The key point: on Debian Bookworm and newer (and recent Ubuntu), the HEVC
decoder was split out of `libheif1` into a separate plugin package. Installing
only `libheif1` gives you a library that **cannot decode HEIC** ("No decoder
plugin available"). You must also install `libheif-plugin-libde265`.

### Debian-based image (recommended — `eclipse-temurin:21`)

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

# FFM API needs native access enabled; flag silences the JDK 21 warning.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
```

`libheif1` installs into a standard library path (`/usr/lib/...`), so it is found
automatically — no `-Djava.library.path` needed in the Debian case.

### Alpine-based image (alternative — smaller, but musl)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# On Alpine the de265 decoder ships as a separate package too.
RUN apk add --no-cache libheif libde265

COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
```

Alpine produces a smaller image but uses musl libc. If you hit loader issues,
the Debian image is the lower-risk choice — favour it unless image size is a
hard constraint.

## 4. Keeping Coolify builds fast

`apt-get install libheif1 libheif-plugin-libde265` runs on every rebuild and adds
to build time. Two ways to avoid paying that cost repeatedly:

1. **Layer ordering.** Place the `apt-get` instruction *before* the `COPY` of the
   built JAR (as shown above). Docker caches it; it only re-runs when the
   instruction itself changes, not when application code changes.

2. **Custom base image.** Build a thin base image once with the native libraries
   baked in, push it to your registry, and have the application image start
   `FROM` it:

   ```dockerfile
   # base image: openelements/temurin21-heif:1.0  — built once, rarely changes
   FROM eclipse-temurin:21-jre
   RUN apt-get update \
       && apt-get install -y --no-install-recommends libheif1 libheif-plugin-libde265 \
       && rm -rf /var/lib/apt/lists/*
   ```

   ```dockerfile
   # application image — rebuilt on every deploy, but native deps already present
   FROM openelements/temurin21-heif:1.0
   COPY --from=build /app/target/*.jar app.jar
   ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
   ```

   This cleanly decouples the slow-changing native dependency from the
   fast-changing application code.

## 5. Verification checklist (HEIC)

- Runtime JDK is 21 or newer.
- Both `libheif1` and `libheif-plugin-libde265` (or Alpine `libheif` + `libde265`)
  are installed.
- The `HeicSupportCheck` component (section 2) logs "support verified" at
  startup — or `/actuator/health` reports `heic: decodable`.
- A test HEIC file from an actual iPhone decodes (test the real source format,
  not a re-encoded sample).

## 6. Adding WebP support

There are two ways to add WebP decoding. They differ mainly in whether you need
a native library.

### Option A — TwelveMonkeys (pure Java, recommended for read-only)

TwelveMonkeys ships a WebP ImageIO plugin written entirely in Java. It needs **no
native library**, runs on **any JDK**, and therefore requires **no Dockerfile
change at all**.

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-webp</artifactId>
    <version>3.12.0</version>
</dependency>
```

Caveat: the TwelveMonkeys WebP plugin is **read-only** — it provides a reader but
no writer. For a WebP → JPEG conversion use case this is exactly enough: you read
WebP and write JPEG via the built-in JPEG plugin. Verify with real test files
that all WebP variants you expect (lossless, alpha, animated) decode correctly,
since pure-Java decoders have historically lagged on edge cases.

### Option B — NightMonkeys (native libwebp, read + write)

If you also need to **write** WebP, or need broader variant coverage, use the
NightMonkeys WebP plugin. It works like `imageio-heif`: JDK 21+, FFM API, and the
native `libwebp` must be present in the image.

```xml
<dependency>
    <groupId>com.github.gotson.nightmonkeys</groupId>
    <artifactId>imageio-webp</artifactId>
    <version>1.1.0</version>
</dependency>
```

Unlike HEIC, WebP has **no separate decoder plugin package** — the decoder is
built into `libwebp` itself, so there is one less package to worry about.

Dockerfile additions for Option B:

```dockerfile
# Debian — add to the apt-get install list:
#   libwebp7        -> the shared library (decoder included)
#   libwebpdemux2   -> only needed for reading animated WebP
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libheif1 \
        libheif-plugin-libde265 \
        libwebp7 \
        libwebpdemux2 \
    && rm -rf /var/lib/apt/lists/*
```

```dockerfile
# Alpine — add libwebp to the apk add list:
RUN apk add --no-cache libheif libde265 libwebp
```

### Which to choose

For Open Elements' use case (decoding uploads, converting to JPEG), **Option A
(TwelveMonkeys)** is the better fit: it removes the WebP native dependency
entirely, so WebP adds zero Docker complexity. Reserve Option B for when WebP
*output* is required. Since `libheif` is already in the image, adding `libwebp`
is a small step — but one fewer native dependency is still the
lower-maintenance path.

## 7. Verification checklist (WebP)

- If using TwelveMonkeys: `imageio-webp` is on the classpath; no native check
  needed.
- If using NightMonkeys: JDK 21+, and `libwebp` (Debian `libwebp7`, Alpine
  `libwebp`) is installed.
- `ImageIO.getReaderFormatNames()` lists `webp` at startup.
- Test with lossy, lossless, and alpha-channel WebP files; add an animated WebP
  if that variant is expected.

## 8. Alternative for HEIC with zero `apt-get`

If avoiding the system dependency entirely matters more than a clean ImageIO API,
use **bytedeco `libheif-platform`** instead of NightMonkeys for HEIC. It ships the
precompiled native library inside the JAR (per-platform), so no `apt-get`/`apk`
is needed — at the cost of a larger JAR and a less elegant API than standard
ImageIO.
