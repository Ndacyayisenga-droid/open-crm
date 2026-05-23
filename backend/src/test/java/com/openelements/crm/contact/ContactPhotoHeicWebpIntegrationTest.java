package com.openelements.crm.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Integration tests for spec 102 (HEIC + WebP). The HEIC happy path, WebP
 * happy path, EXIF orientation, animated-WebP-first-frame, and oversized
 * cases all require binary fixtures that cannot be generated in-memory
 * (TwelveMonkeys provides WebP decode only; HEIC needs libheif). Those
 * scenarios live here as {@code @Disabled} placeholders, gated on the
 * fixture-provision TODO.
 *
 * <p>Active in this v1: the HEIC-unavailable → 415 path (the test environment
 * has no probe sample on classpath, so {@code HeicSupportCheck.isHeicAvailable()}
 * naturally returns false) and the malformed-WebP rejection path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactPhotoHeicWebpIntegrationTest {

    private static final String FIXTURE_TODO =
        "Awaiting test fixtures — see TODO.md: HEIC- und WebP-Testfixtures bereitstellen";

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

    private static MockMultipartHttpServletRequestBuilder upload(final ContactEntity contact) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/contacts/" + contact.getId() + "/photo");
    }

    // -- Active: HEIC unavailability path --

    @Test
    void heicUploadReturns415WhenLibheifIsUnavailable() throws Exception {
        // The test environment ships no /heic-probe/sample.heic on the classpath,
        // so HeicSupportCheck leaves heicAvailable = false. The service must
        // return 415 *before* attempting to decode.
        final ContactEntity contact = newContact();
        final MockMultipartFile file = new MockMultipartFile("file", "p.heic", "image/heic",
            new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'});

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(status().reason(org.hamcrest.Matchers.containsString(
                "HEIC support is not available")));
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto(),
            "Photo must remain unmodified after 415 rejection");
    }

    @Test
    void heifContentTypeAlsoReturns415WhenLibheifIsUnavailable() throws Exception {
        final ContactEntity contact = newContact();
        final MockMultipartFile file = new MockMultipartFile("file", "p.heif", "image/heif",
            new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'i', 'f', '1'});

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isUnsupportedMediaType());
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    // -- Active: WebP rejection on malformed input --

    @Test
    void malformedWebpIsRejectedAs400() throws Exception {
        final ContactEntity contact = newContact();
        // Random bytes labelled image/webp — TwelveMonkeys' reader will refuse.
        final byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
        final MockMultipartFile file = new MockMultipartFile("file", "junk.webp", "image/webp", garbage);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(org.hamcrest.Matchers.containsString("Could not decode WebP")));
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    @Test
    void oversizedWebpIsRejectedBeforeDecode() throws Exception {
        final ContactEntity contact = newContact();
        // 3 MB of zeros under image/webp — the size guard fires before the
        // transcoder is invoked, so the reader is never consulted.
        final byte[] big = new byte[3 * 1024 * 1024];
        final MockMultipartFile file = new MockMultipartFile("file", "big.webp", "image/webp", big);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(org.hamcrest.Matchers.containsString("2 MB")));
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    @Test
    void oversizedHeicIsRejectedBeforeDecode() throws Exception {
        final ContactEntity contact = newContact();
        // Size cap fires before the heicAvailable check — verifies the order
        // of validation in ContactService.uploadPhoto.
        final byte[] big = new byte[3 * 1024 * 1024];
        final MockMultipartFile file = new MockMultipartFile("file", "big.heic", "image/heic", big);

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(org.hamcrest.Matchers.containsString("2 MB")));
        assertNull(contactRepository.findByIdOrThrow(contact.getId()).getPhoto());
    }

    // -- @Disabled: scenarios that need binary fixtures --

    @Test
    @Disabled(FIXTURE_TODO)
    void opaqueHeicUploadIsTranscodedToJpeg() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void heifContentTypeIsAcceptedEquivalently() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void heicWithExifOrientation6IsDecodedUpright() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void malformedHeicIsRejectedAs400() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void opaqueLossyWebpUploadIsTranscodedToJpeg() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void losslessWebpWithAlphaIsFlattenedOverWhite() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void animatedWebpStoresFirstFrameAndReturns200() {
    }

    @Test
    @Disabled(FIXTURE_TODO)
    void webpWithExifOrientationIsDecodedUpright() {
    }

    @Test
    @Disabled(FIXTURE_TODO + " (probe sample also needed)")
    void successfulStartupProbeSetsHeicAvailableTrue() {
    }

    // -- Sanity: a JPEG upload still works end-to-end (regression guard) --

    @Test
    void jpegUploadStillWorksUnderTheNewDispatch() throws Exception {
        final ContactEntity contact = newContact();
        // Encode a 4x4 RGB JPEG in-memory so we don't ship a fixture.
        final java.awt.image.BufferedImage src =
            new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(src, "jpeg", out);
        final MockMultipartFile file = new MockMultipartFile(
            "file", "p.jpg", "image/jpeg", out.toByteArray());

        mockMvc.perform(asUser(upload(contact).file(file)))
            .andExpect(status().isOk());

        final byte[] stored = contactRepository.findByIdOrThrow(contact.getId()).getPhoto();
        assertNotNull(stored);
        assertEquals("image/jpeg",
            contactRepository.findByIdOrThrow(contact.getId()).getPhotoContentType());
    }
}
