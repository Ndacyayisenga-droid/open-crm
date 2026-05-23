package com.openelements.crm.contact;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Decodes PNG bytes, flattens any alpha channel over a white background, and
 * re-encodes as JPEG. The output never carries an alpha channel and never
 * preserves the source PNG's textual chunks — re-encoding is the metadata
 * strip.
 */
final class ContactPhotoTranscoder {

    private static final Color BACKGROUND = Color.WHITE;
    private static final float JPEG_QUALITY = 0.9f;
    private static final String JPEG_FORMAT = "jpeg";

    private ContactPhotoTranscoder() {
    }

    static byte[] pngToJpeg(final byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes must not be null");

        final BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (final IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not decode PNG", e);
        }
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not decode PNG");
        }

        final BufferedImage flattened = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = flattened.createGraphics();
        try {
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, flattened.getWidth(), flattened.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }

        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter available in this JDK");
        }
        final ImageWriter writer = writers.next();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            try (final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(flattened, null, null), params);
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to encode JPEG", e);
            }
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
