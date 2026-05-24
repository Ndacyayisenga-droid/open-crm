package com.openelements.crm.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Wire-level tests for {@link MeilisearchClient}. Uses MockWebServer to
 * verify request paths, the JSON shapes we send, and the Authorization
 * header.
 */
class MeilisearchClientTest {

    private MockWebServer server;
    private MeilisearchClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        final MeilisearchProperties props = new MeilisearchProperties(
            server.url("/").toString(), "master-key", "crm_", null);
        client = new MeilisearchClient(props, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void isHealthyReturnsTrueOnStatusAvailable() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"status\":\"available\"}"));

        assertTrue(client.isHealthy());

        final RecordedRequest req = server.takeRequest();
        assertEquals("GET", req.getMethod());
        assertEquals("/health", req.getPath());
    }

    @Test
    void isHealthyReturnsFalseOnServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));
        assertFalse(client.isHealthy());
    }

    @Test
    void addDocumentsSendsBearerAndReturnsTaskUid() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(202)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"taskUid\":42}"));

        final long taskUid = client.addDocuments("crm_companies",
            List.of(Map.of("id", "abc", "name", "Acme")));

        assertEquals(42L, taskUid);
        final RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/indexes/crm_companies/documents", req.getPath());
        assertEquals("Bearer master-key", req.getHeader("Authorization"));
        assertTrue(req.getBody().readUtf8().contains("\"name\":\"Acme\""));
    }

    @Test
    void useApiKeySwitchesAuthorizationHeader() throws InterruptedException {
        client.useApiKey("scoped-key");
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"status\":\"available\"}"));

        client.isHealthy();

        final RecordedRequest req = server.takeRequest();
        assertEquals("Bearer scoped-key", req.getHeader("Authorization"));
    }

    @Test
    void deleteDocumentSendsDeleteWithPath() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(202)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"taskUid\":7}"));

        client.deleteDocument("crm_contacts", "abc-123");

        final RecordedRequest req = server.takeRequest();
        assertEquals("DELETE", req.getMethod());
        assertEquals("/indexes/crm_contacts/documents/abc-123", req.getPath());
    }

    @Test
    void multiSearchPostsToMultiSearch() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"results\":[]}"));

        client.multiSearch(Map.of("queries", List.of()));

        final RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/multi-search", req.getPath());
    }

    @Test
    void createScopedKeyParsesKeyField() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"key\":\"derived-key-xyz\"}"));

        final String key = client.createScopedKey(List.of("crm_*"), List.of("search"));

        assertEquals("derived-key-xyz", key);
        final RecordedRequest req = server.takeRequest();
        assertEquals("/keys", req.getPath());
    }

    @Test
    void waitForTaskReturnsSucceededWhenStatusReached() {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"status\":\"enqueued\"}"));
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"status\":\"succeeded\"}"));

        assertEquals(MeilisearchClient.TaskOutcome.SUCCEEDED,
            client.waitForTask(1L, Duration.ofSeconds(2)));
    }

    @Test
    void waitForTaskReturnsTimedOutWhenStatusNeverReached() {
        for (int i = 0; i < 50; i++) {
            server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"enqueued\"}"));
        }
        assertEquals(MeilisearchClient.TaskOutcome.TIMED_OUT,
            client.waitForTask(1L, Duration.ofMillis(100)));
    }
}
