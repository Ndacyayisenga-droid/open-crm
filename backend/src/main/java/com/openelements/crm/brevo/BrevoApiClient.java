package com.openelements.crm.brevo;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.openelements.spring.base.services.settings.SettingsDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client for the Brevo v3 REST API. Handles pagination, rate limiting, and retry logic.
 */
@Component
public class BrevoApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoApiClient.class);
    private static final String BREVO_API_KEY = "brevo.api-key";
    private static final String API_KEY_HEADER = "api-key";
    private static final long RATE_LIMIT_MS = 100;
    private static final int MAX_RETRIES = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final long MAX_RATE_LIMIT_WAIT_MS = 30_000L;
    private static final int MAX_PAGES = 1_000;

    private final SettingsDataService settingsService;
    private final RestClient restClient;
    private Instant lastRequestTime = Instant.EPOCH;

    /**
     * Creates a new BrevoApiClient.
     *
     * @param settingsService the settings service for reading the API key
     */
    public BrevoApiClient(final SettingsDataService settingsService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Validates the given API key by calling the Brevo account endpoint.
     *
     * @param apiKey the API key to validate
     * @throws ResponseStatusException with status 400 if the key is invalid or the call fails
     */
    public void validateApiKey(final String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        try {
            restClient.get()
                    .uri("/account")
                    .header(API_KEY_HEADER, apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (final Exception e) {
            LOG.warn("Brevo API key validation failed: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(400), "Invalid Brevo API key");
        }
    }

    /**
     * Fetches all companies from Brevo using pagination.
     *
     * @return the list of all Brevo companies
     */
    public List<BrevoCompany> fetchAllCompanies() {
        final String apiKey = getApiKey();
        final List<BrevoCompany> result = new ArrayList<>();
        final Set<String> seenIds = new HashSet<>();
        final int limit = 50;
        // Brevo /companies uses 1-indexed `page`, NOT `offset` like /contacts.
        int page = 1;
        int pageCount = 0;
        final long startNanos = System.nanoTime();
        LOG.info("Fetching companies from Brevo (page size {})", limit);

        while (true) {
            if (++pageCount > MAX_PAGES) {
                LOG.error("Aborting Brevo companies fetch: exceeded {} pages (page={}). "
                        + "Likely API quirk returning duplicate pages.", MAX_PAGES, page);
                throw new IllegalStateException("Brevo companies pagination exceeded "
                        + MAX_PAGES + " pages");
            }
            final JsonNode body = executeWithRetry(
                    "/companies?limit=" + limit + "&page=" + page, apiKey);
            final JsonNode items = body.get("items");
            if (items == null || !items.isArray() || items.isEmpty()) {
                break;
            }
            int newInPage = 0;
            for (final JsonNode item : items) {
                final String id = item.get("id").asText();
                if (!seenIds.add(id)) {
                    continue;
                }
                newInPage++;
                final JsonNode attrs = item.get("attributes");
                final String name = attrs != null && attrs.has("name")
                        ? attrs.get("name").asText(null) : null;
                final String domain = attrs != null && attrs.has("domain")
                        ? attrs.get("domain").asText(null) : null;
                final List<Long> linkedContactsIds = new ArrayList<>();
                final JsonNode contactIds = item.get("linkedContactsIds");
                if (contactIds != null && contactIds.isArray()) {
                    for (final JsonNode cid : contactIds) {
                        linkedContactsIds.add(cid.asLong());
                    }
                }
                result.add(new BrevoCompany(id, name, domain, linkedContactsIds));
            }
            LOG.info("Fetched Brevo companies page: page={}, size={}, new={}, running total={}",
                    page, items.size(), newInPage, result.size());
            if (newInPage == 0) {
                LOG.warn("Brevo /companies returned only duplicate IDs at page={}, stopping pagination",
                        page);
                break;
            }
            if (items.size() < limit) {
                break;
            }
            page++;
        }
        LOG.info("Finished fetching {} companies from Brevo in {} ms",
                result.size(), (System.nanoTime() - startNanos) / 1_000_000);
        return result;
    }

    /**
     * Fetches all contacts from Brevo using pagination.
     *
     * @return the list of all Brevo contacts
     */
    @SuppressWarnings("unchecked")
    public List<BrevoContact> fetchAllContacts() {
        final String apiKey = getApiKey();
        final List<BrevoContact> result = new ArrayList<>();
        final Set<Long> seenIds = new HashSet<>();
        final int limit = 1000;
        int offset = 0;
        int pageCount = 0;
        final long startNanos = System.nanoTime();
        LOG.info("Fetching contacts from Brevo (page size {})", limit);

        while (true) {
            if (++pageCount > MAX_PAGES) {
                LOG.error("Aborting Brevo contacts fetch: exceeded {} pages (offset={}). "
                        + "Likely API quirk returning duplicate pages.", MAX_PAGES, offset);
                throw new IllegalStateException("Brevo contacts pagination exceeded "
                        + MAX_PAGES + " pages");
            }
            final JsonNode body = executeWithRetry(
                    "/contacts?limit=" + limit + "&offset=" + offset, apiKey);
            final JsonNode contacts = body.get("contacts");
            if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
                break;
            }
            int newInPage = 0;
            for (final JsonNode contact : contacts) {
                final long id = contact.get("id").asLong();
                if (!seenIds.add(id)) {
                    continue;
                }
                newInPage++;
                final String email = contact.has("email") ? contact.get("email").asText(null) : null;
                final JsonNode attrsNode = contact.get("attributes");
                final Map<String, Object> attributes;
                if (attrsNode != null && attrsNode.isObject()) {
                    attributes = new com.fasterxml.jackson.databind.ObjectMapper()
                            .convertValue(attrsNode, Map.class);
                } else {
                    attributes = Collections.emptyMap();
                }
                final boolean emailBlacklisted = contact.has("emailBlacklisted") && contact.get("emailBlacklisted").asBoolean(false);
                result.add(new BrevoContact(id, email, attributes, emailBlacklisted));
            }
            LOG.info("Fetched Brevo contacts page: offset={}, size={}, new={}, running total={}",
                    offset, contacts.size(), newInPage, result.size());
            if (newInPage == 0) {
                LOG.warn("Brevo /contacts returned only duplicate IDs at offset={}, stopping pagination",
                        offset);
                break;
            }
            if (contacts.size() < limit) {
                break;
            }
            offset += limit;
        }
        LOG.info("Finished fetching {} contacts from Brevo in {} ms",
                result.size(), (System.nanoTime() - startNanos) / 1_000_000);
        return result;
    }

    private String getApiKey() {
        return settingsService.get(BREVO_API_KEY)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(400), "Brevo API key not configured"));
    }

    private JsonNode executeWithRetry(final String uri, final String apiKey) {
        int attempt = 0;
        long backoffMs = 1000;

        while (true) {
            final long currentBackoffMs = backoffMs;
            try {
                waitForRateLimit();
                final JsonNode body = restClient.get()
                        .uri(uri)
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (request, response) -> {
                            final String resetHeader = response.getHeaders().getFirst("x-sib-ratelimit-reset");
                            long waitMs = currentBackoffMs;
                            if (resetHeader != null) {
                                try {
                                    waitMs = Long.parseLong(resetHeader) * 1000;
                                } catch (final NumberFormatException ignored) {
                                    // use default backoff
                                }
                            }
                            if (waitMs > MAX_RATE_LIMIT_WAIT_MS) {
                                LOG.warn("Brevo rate-limit reset of {}ms exceeds cap, capping at {}ms",
                                        waitMs, MAX_RATE_LIMIT_WAIT_MS);
                                waitMs = MAX_RATE_LIMIT_WAIT_MS;
                            }
                            throw new RateLimitException(waitMs);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                            throw new ServerErrorException(response.getStatusCode().value());
                        })
                        .body(JsonNode.class);
                lastRequestTime = Instant.now();
                return body;
            } catch (final RateLimitException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException(
                            "Brevo API rate limit exceeded after " + MAX_RETRIES + " retries");
                }
                LOG.warn("Rate limited by Brevo API, waiting {}ms before retry {}/{}",
                        e.getWaitMs(), attempt, MAX_RETRIES);
                sleep(e.getWaitMs());
            } catch (final ServerErrorException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException(
                            "Brevo API server error (" + e.getStatusCode() + ") after "
                                    + MAX_RETRIES + " retries");
                }
                LOG.warn("Brevo API server error {}, retrying {}/{} after {}ms",
                        e.getStatusCode(), attempt, MAX_RETRIES, backoffMs);
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
    }

    private void waitForRateLimit() {
        final long elapsed = Instant.now().toEpochMilli() - lastRequestTime.toEpochMilli();
        if (elapsed < RATE_LIMIT_MS) {
            sleep(RATE_LIMIT_MS - elapsed);
        }
    }

    private void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting", e);
        }
    }

    private static class RateLimitException extends RuntimeException {

        private final long waitMs;

        RateLimitException(final long waitMs) {
            this.waitMs = waitMs;
        }

        long getWaitMs() {
            return waitMs;
        }
    }

    private static class ServerErrorException extends RuntimeException {

        private final int statusCode;

        ServerErrorException(final int statusCode) {
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }
    }
}
