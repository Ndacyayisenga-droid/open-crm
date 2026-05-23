package com.openelements.crm.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Integration tests for POST/GET /api/contacts/{id}/photo covering the JPEG
 * passthrough, the new PNG transcode path, oversized rejection, unsupported
 * types, malformed PNG, and the GET content-type contract.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactPhotoUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedSystemUser() {
        if (userRepository.findBySub(SystemUser.SUB).isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO users (id, sub, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SystemUser.ID, SystemUser.SUB, SystemUser.NAME);
        }
    }

    @AfterEach
    void clean() {
        contactRepository.deleteAll();
    }

    private static <T extends MockHttpServletRequestBuilder> T asUser(final T builder) {
        final List<String> roles = List.of();
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        builder.with(jwt().jwt(jwt).authorities(authorities));
        return builder;
    }

    private ContactEntity newContact() {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName("Jane");
        contact.setLastName("Doe");
        return contactRepository.saveAndFlush(contact);
    }

    private static byte[] writePng(final BufferedImage image) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] writeJpeg(final BufferedImage image) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }

    private static byte[] solidJpeg(final int width, final int height, final Color color) throws IOException {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return writeJpeg(img);
    }

    private static byte[] solidPng(final int width, final int height, final Color color) throws IOException {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return writePng(img);
    }

    private static MockMultipartHttpServletRequestBuilder upload(final ContactEntity contact) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/contacts/" + contact.getId() + "/photo");
    }

    // -- JPEG passthrough --

    @Test
    void jpegUploadStoresBytesAsIs() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] jpegBytes = solidJpeg(40, 30, Color.GRAY);
        final MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isOk());

        final ContactEntity reloaded = contactRepository.findByIdOrThrow(contact.getId());
        assertEquals("image/jpeg", reloaded.getPhotoContentType());
        assertArraysEqual(jpegBytes, reloaded.getPhoto());
    }

    @Test
    void oversizedJpegIsRejected() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] big = new byte[3 * 1024 * 1024];
        final MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", big);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest());

        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto(),
            "Oversized upload must not be persisted");
    }

    // -- PNG transcoding --

    @Test
    void opaquePngUploadIsTranscodedToJpeg() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] pngBytes = solidPng(40, 30, Color.BLUE);
        final MockMultipartFile file = new MockMultipartFile("file", "p.png", "image/png", pngBytes);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isOk());

        final ContactEntity reloaded = contactRepository.findByIdOrThrow(contact.getId());
        assertEquals("image/jpeg", reloaded.getPhotoContentType());

        final byte[] stored = reloaded.getPhoto();
        assertNotNull(stored);
        assertNotEquals(pngBytes.length, stored.length, "Stored bytes should not equal source PNG");
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored));
        assertNotNull(decoded, "Stored bytes must decode as image");
        assertEquals(40, decoded.getWidth());
        assertEquals(30, decoded.getHeight());
    }

    @Test
    void transparentPngIsFlattenedOverWhite() throws Exception {
        final ContactEntity contact = newContact();
        final BufferedImage src = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Entirely transparent.
        final byte[] pngBytes = writePng(src);
        final MockMultipartFile file = new MockMultipartFile("file", "t.png", "image/png", pngBytes);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isOk());

        final ContactEntity reloaded = contactRepository.findByIdOrThrow(contact.getId());
        assertEquals("image/jpeg", reloaded.getPhotoContentType());
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(reloaded.getPhoto()));
        assertNotNull(decoded);
        final int rgb = decoded.getRGB(8, 8) & 0x00FFFFFF;
        assertEquals(0xFFFFFF, rgb, "Transparent area must flatten to white");
    }

    @Test
    void oversizedPngIsRejectedBeforeTranscoding() throws Exception {
        final ContactEntity contact = newContact();
        // 3 MB of zeros — content does not need to be a valid PNG; the size
        // check fires first.
        final byte[] big = new byte[3 * 1024 * 1024];
        final MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest());

        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    @Test
    void malformedPngIsRejected() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] junk = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
        final MockMultipartFile file = new MockMultipartFile("file", "junk.png", "image/png", junk);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest());

        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    // -- Rejected content types --

    @Test
    void gifWebpSvgHeicBmpPdfAreRejected() throws Exception {
        final ContactEntity contact = newContact();
        final List<String> rejected = List.of(
            "image/gif", "image/webp", "image/svg+xml",
            "image/heic", "image/bmp", "application/pdf");
        for (final String ct : rejected) {
            final MockMultipartFile file = new MockMultipartFile("file", "x.bin", ct, new byte[]{0x01});
            mockMvc.perform(asUser(upload(contact).file(file)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(org.hamcrest.Matchers.containsString("JPEG and PNG")));
            assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto(),
                "Photo must remain unmodified after rejected upload (ct=" + ct + ")");
        }
    }

    @Test
    void missingContentTypeIsRejectedAs400() throws Exception {
        final ContactEntity contact = newContact();
        // Pass null content-type — MockMultipartFile stores it and Spring forwards
        // the null to the service. The service must map null → 400, not 500.
        final MockMultipartFile file = new MockMultipartFile("file", "x.bin", null, new byte[]{0x01});
        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest());
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    @Test
    void pngTranscodeStripsTextChunkMetadata() throws Exception {
        final ContactEntity contact = newContact();
        // Build a PNG carrying a tEXt chunk via PNGMetadata.
        final BufferedImage src = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = src.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, 8, 8);
        g.dispose();

        final ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        final javax.imageio.ImageWriter pngWriter =
            ImageIO.getImageWritersByFormatName("png").next();
        try (final javax.imageio.stream.MemoryCacheImageOutputStream ios =
                 new javax.imageio.stream.MemoryCacheImageOutputStream(pngOut)) {
            pngWriter.setOutput(ios);
            final javax.imageio.metadata.IIOMetadata md = pngWriter.getDefaultImageMetadata(
                javax.imageio.ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
                null);
            final String nativeFormat = md.getNativeMetadataFormatName();
            final javax.imageio.metadata.IIOMetadataNode root = new javax.imageio.metadata.IIOMetadataNode(nativeFormat);
            final javax.imageio.metadata.IIOMetadataNode text = new javax.imageio.metadata.IIOMetadataNode("tEXt");
            final javax.imageio.metadata.IIOMetadataNode entry = new javax.imageio.metadata.IIOMetadataNode("tEXtEntry");
            entry.setAttribute("keyword", "Comment");
            entry.setAttribute("value", "PNG-SECRET-METADATA");
            text.appendChild(entry);
            root.appendChild(text);
            md.mergeTree(nativeFormat, root);
            pngWriter.write(null, new javax.imageio.IIOImage(src, null, md), null);
        } finally {
            pngWriter.dispose();
        }
        final byte[] pngBytes = pngOut.toByteArray();
        assertTrue(new String(pngBytes, java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains("PNG-SECRET-METADATA"),
            "Source PNG should carry the metadata before upload");

        final MockMultipartFile file = new MockMultipartFile("file", "p.png", "image/png", pngBytes);
        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isOk());

        final byte[] stored = contactRepository.findByIdOrThrow(contact.getId()).getPhoto();
        assertNotNull(stored);
        assertTrue(!new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains("PNG-SECRET-METADATA"),
            "Transcoded JPEG must not carry the source PNG's tEXt metadata");
    }

    // -- GET path --

    @Test
    void getReturnsImageJpegHeaderAfterPngUpload() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] pngBytes = solidPng(20, 20, Color.GREEN);
        final MockMultipartFile file = new MockMultipartFile("file", "p.png", "image/png", pngBytes);
        mockMvc.perform(asUser(upload(contact).file(file))).andExpect(status().isOk());

        mockMvc.perform(asUser(get("/api/contacts/" + contact.getId() + "/photo")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    void getReturnsExactJpegBytesAfterJpegUpload() throws Exception {
        final ContactEntity contact = newContact();
        final byte[] jpegBytes = solidJpeg(20, 20, Color.RED);
        final MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", jpegBytes);
        mockMvc.perform(asUser(upload(contact).file(file))).andExpect(status().isOk());

        final byte[] body = mockMvc.perform(asUser(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/contacts/" + contact.getId() + "/photo")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE))
            .andReturn().getResponse().getContentAsByteArray();
        assertArraysEqual(jpegBytes, body);
    }

    private static void assertArraysEqual(final byte[] expected, final byte[] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length, "byte-length mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i] == actual[i], "byte at index " + i + " differs");
        }
    }
}
