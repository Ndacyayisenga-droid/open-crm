package com.openelements.crm.contact;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Startup probe that verifies HEIC decoding is actually available in the
 * running JVM and the surrounding runtime image. NightMonkeys' {@code
 * imageio-heif} is a Multi-Release JAR that silently unregisters its reader on
 * older JDKs, and the libheif native library may simply be missing from the
 * runtime image. Either failure mode is opaque to operators by default — this
 * component makes both visible at startup via log lines.
 *
 * <p>The check has two levels: a registry check ({@code ImageIO.getReader…}),
 * and a real decode against a tiny bundled probe sample.
 */
@Component
public class HeicSupportCheck {

    private static final Logger log = LoggerFactory.getLogger(HeicSupportCheck.class);
    private static final String PROBE_RESOURCE = "/heic-probe/sample.heic";

    private volatile boolean heicAvailable = false;

    /**
     * Returns whether the runtime can decode HEIC images. The value is set
     * exactly once during startup by {@link #verifyHeicSupport()}. Callers
     * must short-circuit HEIC uploads with 415 when this returns {@code false}.
     */
    public boolean isHeicAvailable() {
        return heicAvailable;
    }

    @PostConstruct
    void verifyHeicSupport() {
        final boolean readerRegistered = Arrays.stream(ImageIO.getReaderFormatNames())
            .anyMatch(n -> n.equalsIgnoreCase("heif") || n.equalsIgnoreCase("heic"));
        if (!readerRegistered) {
            log.warn("HEIC: no ImageIO reader registered. Check JDK 21+ and the "
                + "imageio-heif dependency. HEIC uploads will be rejected with 415.");
            return;
        }
        try (InputStream sample = getClass().getResourceAsStream(PROBE_RESOURCE)) {
            if (sample == null) {
                log.error("HEIC: probe sample {} is missing on classpath. "
                    + "HEIC uploads will be rejected with 415 until the sample is provided.",
                    PROBE_RESOURCE);
                return;
            }
            final BufferedImage img = ImageIO.read(sample);
            if (img != null) {
                heicAvailable = true;
                log.info("HEIC: support verified (decoded {}x{} probe image).",
                    img.getWidth(), img.getHeight());
            } else {
                log.error("HEIC: reader registered but decode returned null. "
                    + "Install libheif1 + libheif-plugin-libde265 in the runtime image. "
                    + "HEIC uploads will be rejected with 415.");
            }
        } catch (final UnsatisfiedLinkError | Exception e) {
            log.error("HEIC: support check failed. Install libheif1 + "
                + "libheif-plugin-libde265 in the runtime image. HEIC uploads will be "
                + "rejected with 415.", e);
        }
    }
}
