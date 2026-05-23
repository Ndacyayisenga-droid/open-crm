package com.openelements.crm.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the PNG → JPEG transcode helper. Generates fixtures
 * in-memory to avoid checked-in binary blobs.
 */
class ContactPhotoTranscoderTest {

    private static byte[] writePng(final BufferedImage image) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage readJpeg(final byte[] bytes) throws IOException {
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(decoded, "JPEG must decode");
        return decoded;
    }

    @Test
    void opaquePngIsTranscodedToValidJpegOfSameDimensions() throws IOException {
        final BufferedImage source = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = source.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 32, 24);
        g.dispose();
        final byte[] pngBytes = writePng(source);

        final byte[] jpegBytes = ContactPhotoTranscoder.pngToJpeg(pngBytes);

        final BufferedImage decoded = readJpeg(jpegBytes);
        assertEquals(32, decoded.getWidth());
        assertEquals(24, decoded.getHeight());
        assertNotEquals(pngBytes.length, jpegBytes.length, "Output should not equal input bytes");
    }

    @Test
    void transparentPngIsCompositedOverWhite() throws IOException {
        final BufferedImage source = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        // Entirely transparent — all pixels alpha=0.
        final byte[] pngBytes = writePng(source);

        final byte[] jpegBytes = ContactPhotoTranscoder.pngToJpeg(pngBytes);
        final BufferedImage decoded = readJpeg(jpegBytes);

        final int rgb = decoded.getRGB(4, 4) & 0x00FFFFFF;
        assertEquals(0xFFFFFF, rgb, "Transparent pixel should flatten to white");
    }

    @Test
    void semiTransparentPngBlendsWithWhite() throws IOException {
        final BufferedImage source = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = source.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        final byte[] pngBytes = writePng(source);

        final byte[] jpegBytes = ContactPhotoTranscoder.pngToJpeg(pngBytes);
        final BufferedImage decoded = readJpeg(jpegBytes);

        final int rgb = decoded.getRGB(4, 4);
        final int r = (rgb >> 16) & 0xFF;
        final int gComponent = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        // 50% red over white: ~(255, 128, 128). JPEG and the Graphics2D
        // composite both introduce a few units of drift — allow ±10.
        assertTrue(r >= 245, "red channel should stay near 255 but was " + r);
        assertTrue(Math.abs(gComponent - 128) <= 10, "green should be ~128 but was " + gComponent);
        assertTrue(Math.abs(b - 128) <= 10, "blue should be ~128 but was " + b);
    }

    @Test
    void malformedPngBytesYield400() {
        final byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> ContactPhotoTranscoder.pngToJpeg(garbage));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void outputIsJpegMagicBytes() throws IOException {
        final BufferedImage source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        final byte[] jpegBytes = ContactPhotoTranscoder.pngToJpeg(writePng(source));
        assertTrue(jpegBytes.length > 2, "JPEG output must have content");
        assertEquals((byte) 0xFF, jpegBytes[0]);
        assertEquals((byte) 0xD8, jpegBytes[1]);
    }
}
