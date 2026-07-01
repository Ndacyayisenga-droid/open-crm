package com.openelements.crm.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * End-to-end tests for the MCP endpoint (spec 108, step 7), driving a real MCP
 * Streamable-HTTP client (authenticating with {@code X-API-Key}) against a
 * running server on a random port. Covers tool discovery, tool calls, the
 * pagination envelope, invalid-argument and not-found errors, and audit-by-key.
 *
 * <p>The {@code search} tool's happy path needs Meilisearch and is covered by
 * {@code SearchIntegrationTest} (the search implementation is shared); here it is
 * only asserted to be present in the catalog.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "openelements.mcp.enabled=true")
class McpEndpointIntegrationTest extends AbstractDbTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    private static final byte[] PHOTO_BYTES = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final byte[] LOGO_BYTES = {10, 20, 30, 40, 50};

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String rawKey;
    private UUID contactId;
    private UUID contactWithPhotoId;
    private UUID companyWithLogoId;
    private UUID companyNoLogoId;

    @BeforeEach
    void seed() {
        seedSystemUser();
        rawKey = "crm_" + "e2ekey".repeat(6) + "tail1234";
        final Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO api_keys (id, name, key_hash, key_prefix, created_by, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), "onyx-e2e", McpTestSupport.sha256Hex(rawKey), rawKey.substring(0, 11), "test",
            java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));

        final ContactEntity contact = new ContactEntity();
        contact.setFirstName("Erika");
        contact.setLastName("Musterfrau");
        contact.setEmail("erika@example.com");
        contactId = contactRepository.saveAndFlush(contact).getId();

        final ContactEntity withPhoto = new ContactEntity();
        withPhoto.setFirstName("Max");
        withPhoto.setLastName("Mustermann");
        withPhoto.setEmail("max@example.com");
        withPhoto.setPhoto(PHOTO_BYTES);
        withPhoto.setPhotoContentType("image/jpeg");
        contactWithPhotoId = contactRepository.saveAndFlush(withPhoto).getId();

        final CompanyEntity withLogo = new CompanyEntity();
        withLogo.setName("Logo GmbH");
        withLogo.setLogo(LOGO_BYTES);
        withLogo.setLogoContentType("image/png");
        companyWithLogoId = companyRepository.saveAndFlush(withLogo).getId();

        final CompanyEntity noLogo = new CompanyEntity();
        noLogo.setName("Plain AG");
        companyNoLogoId = companyRepository.saveAndFlush(noLogo).getId();
    }

    private McpSyncClient newClient(final String apiKey) {
        final HttpClientStreamableHttpTransport transport =
            HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .jsonMapper(new JacksonMcpJsonMapper(MAPPER))
                .customizeRequest(builder -> builder.header("X-API-Key", apiKey))
                .build();
        final McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(20))
            .build();
        client.initialize();
        return client;
    }

    @Test
    void listToolsReturnsThePhase1Catalog() {
        try (McpSyncClient client = newClient(rawKey)) {
            final var tools = client.listTools().tools();
            final var names = tools.stream().map(McpSchema.Tool::name).toList();
            assertEquals(11, names.size());
            assertTrue(names.contains("search"));
            assertTrue(names.contains("list_contacts"));
            assertTrue(names.contains("list_company_comments"));
            assertTrue(names.contains("get_contact_photo"));
            assertTrue(names.contains("get_company_logo"));

            for (final String imageTool : List.of("get_contact_photo", "get_company_logo")) {
                final McpSchema.Tool tool = tools.stream()
                    .filter(t -> t.name().equals(imageTool)).findFirst().orElseThrow();
                assertTrue(tool.inputSchema().required().contains("id"),
                    imageTool + " must declare a required id property");
                assertTrue(tool.inputSchema().properties().containsKey("id"),
                    imageTool + " must expose an id input property");
            }
        }
    }

    @Test
    void listContactsReturnsPaginatedEnvelope() throws Exception {
        try (McpSyncClient client = newClient(rawKey)) {
            final JsonNode json = callOk(client, "list_contacts", Map.of());
            assertTrue(json.has("items") && json.get("items").isArray());
            assertTrue(json.get("totalCount").asInt() >= 1, "seeded contact must be counted");
            assertTrue(json.has("hasMore"));
        }
    }

    @Test
    void sizeAboveMaxIsClampedTo50() throws Exception {
        try (McpSyncClient client = newClient(rawKey)) {
            final JsonNode json = callOk(client, "list_contacts", Map.of("size", 200));
            assertEquals(50, json.get("size").asInt(), "size clamps to max-page-size");
        }
    }

    @Test
    void getContactReturnsTheContact() throws Exception {
        try (McpSyncClient client = newClient(rawKey)) {
            final JsonNode json = callOk(client, "get_contact", Map.of("id", contactId.toString()));
            assertEquals("Erika", json.get("firstName").asText());
        }
    }

    @Test
    void getContactWithUnknownIdIsError() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact", Map.of("id", UUID.randomUUID().toString())));
            assertTrue(Boolean.TRUE.equals(result.isError()), "unknown id must be a tool error");
        }
    }

    @Test
    void invalidSizeIsError() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("list_contacts", Map.of("size", 0)));
            assertTrue(Boolean.TRUE.equals(result.isError()), "size=0 must be rejected");
        }
    }

    @Test
    void contactCommentsReturnEmptyEnvelopeWhenNone() throws Exception {
        try (McpSyncClient client = newClient(rawKey)) {
            final JsonNode json = callOk(client, "list_contact_comments", Map.of("contactId", contactId.toString()));
            assertEquals(0, json.get("totalCount").asInt());
            assertFalse(json.get("hasMore").asBoolean());
        }
    }

    @Test
    void getContactPhotoReturnsJpegImageContent() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact_photo", Map.of("id", contactWithPhotoId.toString())));
            assertFalse(Boolean.TRUE.equals(result.isError()), "photo fetch must not error");
            final McpSchema.ImageContent image = (McpSchema.ImageContent) result.content().get(0);
            assertEquals("image/jpeg", image.mimeType());
            assertEquals(Base64.getEncoder().encodeToString(PHOTO_BYTES), image.data());
        }
    }

    @Test
    void getContactPhotoErrorsWhenContactHasNoPhoto() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact_photo", Map.of("id", contactId.toString())));
            assertTrue(Boolean.TRUE.equals(result.isError()), "contact without a photo must be a tool error");
        }
    }

    @Test
    void getContactPhotoErrorsWhenContactUnknown() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact_photo", Map.of("id", UUID.randomUUID().toString())));
            assertTrue(Boolean.TRUE.equals(result.isError()), "unknown contact must be a tool error");
        }
    }

    @Test
    void getContactPhotoErrorsOnMalformedId() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact_photo", Map.of("id", "not-a-uuid")));
            assertTrue(Boolean.TRUE.equals(result.isError()), "malformed id must be a tool error");
        }
    }

    @Test
    void getContactPhotoErrorsOnMissingId() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_contact_photo", Map.of()));
            assertTrue(Boolean.TRUE.equals(result.isError()), "missing id must be a tool error");
        }
    }

    @Test
    void getCompanyLogoPreservesStoredPngContentType() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_company_logo", Map.of("id", companyWithLogoId.toString())));
            assertFalse(Boolean.TRUE.equals(result.isError()), "logo fetch must not error");
            final McpSchema.ImageContent image = (McpSchema.ImageContent) result.content().get(0);
            assertEquals("image/png", image.mimeType(), "logos are stored as-uploaded, not transcoded to jpeg");
            assertEquals(Base64.getEncoder().encodeToString(LOGO_BYTES), image.data());
        }
    }

    @Test
    void getCompanyLogoErrorsWhenCompanyHasNoLogo() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_company_logo", Map.of("id", companyNoLogoId.toString())));
            assertTrue(Boolean.TRUE.equals(result.isError()), "company without a logo must be a tool error");
        }
    }

    @Test
    void getCompanyLogoErrorsWhenCompanyUnknown() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_company_logo", Map.of("id", UUID.randomUUID().toString())));
            assertTrue(Boolean.TRUE.equals(result.isError()), "unknown company must be a tool error");
        }
    }

    @Test
    void getCompanyLogoErrorsOnMissingId() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_company_logo", Map.of()));
            assertTrue(Boolean.TRUE.equals(result.isError()), "missing id must be a tool error");
        }
    }

    @Test
    void getCompanyLogoErrorsOnMalformedId() {
        try (McpSyncClient client = newClient(rawKey)) {
            final McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("get_company_logo", Map.of("id", "not-a-uuid")));
            assertTrue(Boolean.TRUE.equals(result.isError()), "malformed id must be a tool error");
        }
    }

    @Test
    void imageToolsDoNotWriteAnAuditLogRow() {
        final long before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
        try (McpSyncClient client = newClient(rawKey)) {
            client.callTool(new McpSchema.CallToolRequest(
                "get_contact_photo", Map.of("id", contactWithPhotoId.toString())));   // success
            client.callTool(new McpSchema.CallToolRequest(
                "get_company_logo", Map.of("id", UUID.randomUUID().toString())));      // error
        }
        final long after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
        assertEquals(before, after, "MCP image reads must not write an audit-log row");
    }

    private JsonNode callOk(final McpSyncClient client, final String tool, final Map<String, Object> args)
        throws Exception {
        final McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(tool, args));
        assertFalse(Boolean.TRUE.equals(result.isError()), "tool " + tool + " unexpectedly errored");
        final McpSchema.TextContent text = (McpSchema.TextContent) result.content().get(0);
        return MAPPER.readTree(text.text());
    }
}
