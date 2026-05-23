package com.openelements.crm.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin HTTP wrapper around the Meilisearch REST API. Uses Spring's
 * {@link RestClient} (already on the classpath via {@code spring-web}).
 *
 * <p>Authentication: this client starts with the master key, but after
 * {@link MeilisearchKeyDeriver} successfully exchanges it for a scoped key
 * the runtime calls use the scoped key. The exchange is one-shot at startup
 * and the master key is not used again.
 */
public class MeilisearchClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MeilisearchProperties props;
    private final AtomicReference<String> apiKey;

    public MeilisearchClient(final MeilisearchProperties props, final ObjectMapper objectMapper) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.apiKey = new AtomicReference<>(props.masterKey());
        this.restClient = RestClient.builder()
            .baseUrl(props.host())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /** Swap the master key out for a scoped runtime key. */
    public void useApiKey(final String key) {
        Objects.requireNonNull(key, "key must not be null");
        apiKey.set(key);
    }

    /** Returns true if Meilisearch reports {@code status == "available"}. */
    public boolean isHealthy() {
        try {
            final JsonNode body = exchange(restClient.get().uri("/health"), JsonNode.class);
            return body != null && "available".equals(body.path("status").asText());
        } catch (final RestClientException | MeilisearchException e) {
            return false;
        }
    }

    /**
     * Mints a scoped API key tied to {@code crm_*} indexes via
     * {@code POST /keys}. Caller must hold the master key.
     */
    public String createScopedKey(final List<String> indexPatterns, final List<String> actions) {
        try {
            // Use a HashMap rather than Map.of because expiresAt is intentionally
            // null (no expiry) and Map.of rejects null values.
            final java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
            payload.put("description", "open-crm runtime key");
            payload.put("actions", actions);
            payload.put("indexes", indexPatterns);
            payload.put("expiresAt", null);
            final String body = objectMapper.writeValueAsString(payload);
            final JsonNode response = exchange(
                restClient.post().uri("/keys").body(body),
                JsonNode.class);
            return response.path("key").asText();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to encode createScopedKey body", e);
        }
    }

    /** Pushes (upsert) a batch of documents into the given index. */
    public long addDocuments(final String indexUid, final List<Map<String, Object>> docs) {
        try {
            final String body = objectMapper.writeValueAsString(docs);
            final JsonNode response = exchange(
                restClient.post().uri("/indexes/{u}/documents", indexUid).body(body),
                JsonNode.class);
            return response.path("taskUid").asLong();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to encode addDocuments body", e);
        }
    }

    /** Deletes a single document by primary key. */
    public long deleteDocument(final String indexUid, final String id) {
        final JsonNode response = exchange(
            restClient.delete().uri("/indexes/{u}/documents/{id}", indexUid, id),
            JsonNode.class);
        return response.path("taskUid").asLong();
    }

    /** Writes index settings (searchable / filterable / sortable / etc). Idempotent. */
    public long updateSettings(final String indexUid, final Map<String, Object> settings) {
        try {
            final String body = objectMapper.writeValueAsString(settings);
            final JsonNode response = exchange(
                restClient.patch().uri("/indexes/{u}/settings", indexUid).body(body),
                JsonNode.class);
            return response.path("taskUid").asLong();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to encode updateSettings body", e);
        }
    }

    /** Ensures the named index exists. Idempotent — 200 or 202 on duplicate. */
    public void ensureIndex(final String indexUid, final String primaryKey) {
        try {
            final String body = objectMapper.writeValueAsString(Map.of(
                "uid", indexUid,
                "primaryKey", primaryKey));
            exchange(restClient.post().uri("/indexes").body(body), JsonNode.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to encode ensureIndex body", e);
        }
        // Meilisearch returns 4xx if the index already exists; the exchange()
        // wrapper does not throw on those because we don't really care.
    }

    /** Polls {@code GET /tasks/{id}} until the task reaches a terminal state or the timeout elapses. */
    public TaskOutcome waitForTask(final long taskUid, final Duration timeout) {
        final long deadline = System.nanoTime() + timeout.toNanos();
        long backoffMs = 25L;
        while (System.nanoTime() < deadline) {
            final JsonNode task = exchange(
                restClient.get().uri("/tasks/{id}", taskUid), JsonNode.class);
            final String status = task.path("status").asText();
            if ("succeeded".equals(status)) {
                return TaskOutcome.SUCCEEDED;
            }
            if ("failed".equals(status) || "canceled".equals(status)) {
                return TaskOutcome.FAILED;
            }
            try {
                Thread.sleep(backoffMs);
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
                return TaskOutcome.FAILED;
            }
            backoffMs = Math.min(backoffMs * 2, 250L);
        }
        return TaskOutcome.TIMED_OUT;
    }

    /** Issues a {@code POST /multi-search}; the {@code queries} body is opaque (caller-built JSON). */
    public JsonNode multiSearch(final Map<String, Object> queries) {
        try {
            final String body = objectMapper.writeValueAsString(queries);
            return exchange(restClient.post().uri("/multi-search").body(body), JsonNode.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to encode multiSearch body", e);
        }
    }

    private <T> T exchange(final RestClient.RequestBodySpec spec, final Class<T> type) {
        return spec
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.get())
            .retrieve()
            .onStatus(s -> s.isError() && s.value() != 404, (req, resp) -> {
                // 404 on ensureIndex/delete is benign; treat others as errors.
                throw new MeilisearchException("Meilisearch HTTP " + resp.getStatusCode()
                    + " for " + req.getURI() + ": " + new String(resp.getBody().readAllBytes()));
            })
            .body(type);
    }

    private <T> T exchange(final RestClient.RequestHeadersSpec<?> spec, final Class<T> type) {
        return spec
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.get())
            .retrieve()
            .onStatus(s -> s.isError() && s.value() != 404, (req, resp) -> {
                throw new MeilisearchException("Meilisearch HTTP " + resp.getStatusCode()
                    + " for " + req.getURI() + ": " + new String(resp.getBody().readAllBytes()));
            })
            .body(type);
    }

    public enum TaskOutcome { SUCCEEDED, FAILED, TIMED_OUT }

    /** Thin runtime exception so callers can distinguish Meilisearch errors. */
    public static final class MeilisearchException extends RuntimeException {
        public MeilisearchException(final String message) {
            super(message);
        }
    }
}
