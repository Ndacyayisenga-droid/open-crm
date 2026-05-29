package com.openelements.crm.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.spring.base.services.search.Highlighter;
import com.openelements.spring.base.services.search.MeilisearchClient;
import com.openelements.spring.base.services.search.SearchReadinessState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global search endpoint. Fans out one Meilisearch {@code POST /multi-search}
 * call to four CRM indexes (companies / contacts / tags / comments) and
 * returns the results grouped per section.
 *
 * <p>Until {@link com.openelements.spring.base.services.search.MeilisearchBootstrapRunner}
 * reports finished, responds with {@code 503 SERVICE_UNAVAILABLE} +
 * {@code Retry-After: 30} so clients can back off cleanly.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Global typo-tolerant search via Meilisearch")
@SecurityRequirement(name = "oidc")
public class SearchController {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final int RETRY_AFTER_SECONDS = 30;

    private final MeilisearchClient client;
    private final CrmIndexNames indexNames;
    private final SearchReadinessState state;

    public SearchController(final MeilisearchClient client,
                            final CrmIndexNames indexNames,
                            final SearchReadinessState state) {
        this.client = client;
        this.indexNames = indexNames;
        this.state = state;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Global search", description =
        "Searches all four indexes (companies, contacts, tags, comments) for the given "
        + "query. Returns 503 with Retry-After while the startup bootstrap is still "
        + "running.")
    @ApiResponse(responseCode = "200", description = "Grouped search result")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "503", description = "Search index is initializing")
    public ResponseEntity<?> search(
        @Parameter(description = "Search query — must be at least 2 characters; shorter queries return empty sections")
        @RequestParam(required = false, defaultValue = "") final String q,
        @Parameter(description = "Maximum hits per section (default 5, capped at 20)")
        @RequestParam(required = false, defaultValue = "5") final int limit) {

        if (state.isBootstrapping()) {
            return ResponseEntity.status(503)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS))
                .body(Map.of("error", "search index is initializing"));
        }

        final String query = q == null ? "" : q.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.ok(GlobalSearchResultDto.empty(query));
        }
        final int sectionLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        final JsonNode response = client.multiSearch(Map.of("queries", List.of(
            queryForIndex(indexNames.companies(), query, sectionLimit,
                List.of("name", "email", "address")),
            queryForIndex(indexNames.contacts(), query, sectionLimit,
                List.of("firstName", "lastName", "email", "companyName")),
            queryForIndex(indexNames.tags(), query, sectionLimit,
                List.of("name", "description")),
            queryForIndex(indexNames.comments(), query, sectionLimit,
                List.of("text", "ownerLabel"))
        )));

        final GlobalSearchResultDto result = new GlobalSearchResultDto(
            query,
            extractHits(response, indexNames.companies(), this::companyHit),
            extractHits(response, indexNames.contacts(), this::contactHit),
            extractHits(response, indexNames.tags(), this::tagHit),
            extractHits(response, indexNames.comments(), this::commentHit));
        return ResponseEntity.ok(result);
    }

    private static Map<String, Object> queryForIndex(final String indexUid,
                                                     final String q,
                                                     final int limit,
                                                     final List<String> highlightAttrs) {
        return Map.of(
            "indexUid", indexUid,
            "q", q,
            "limit", limit,
            "attributesToHighlight", highlightAttrs,
            "highlightPreTag", Highlighter.PRE_MARK,
            "highlightPostTag", Highlighter.POST_MARK);
    }

    /**
     * Pulls hits for a specific index from the multi-search response by
     * matching {@code indexUid} rather than relying on positional order —
     * Meilisearch may reorder errored sections, so positional access is
     * brittle.
     */
    private List<SearchHitDto> extractHits(final JsonNode response, final String indexUid,
                                           final HitMapper mapper) {
        final JsonNode results = response.path("results");
        if (!results.isArray()) {
            return List.of();
        }
        JsonNode section = null;
        for (final JsonNode r : results) {
            if (indexUid.equals(r.path("indexUid").asText())) {
                section = r;
                break;
            }
        }
        if (section == null) {
            return List.of();
        }
        final JsonNode hits = section.path("hits");
        if (!hits.isArray()) {
            return List.of();
        }
        final List<SearchHitDto> out = new ArrayList<>(hits.size());
        for (final JsonNode hit : hits) {
            final SearchHitDto mapped = mapper.map(hit);
            if (mapped != null) {
                out.add(mapped);
            }
        }
        return out;
    }

    private SearchHitDto companyHit(final JsonNode hit) {
        final UUID id = readId(hit);
        if (id == null) {
            return null;
        }
        final String name = hit.path("name").asText("");
        final String highlight = Highlighter.safeHighlight(hit.path("_formatted").path("name").asText(name));
        final String snippet = hit.path("email").asText("");
        return new SearchHitDto(id, name, snippet, highlight, scoreOf(hit), null, null);
    }

    private SearchHitDto contactHit(final JsonNode hit) {
        final UUID id = readId(hit);
        if (id == null) {
            return null;
        }
        final String first = hit.path("firstName").asText("");
        final String last = hit.path("lastName").asText("");
        final String label = (first + " " + last).trim();
        final JsonNode fmt = hit.path("_formatted");
        final String highlightFirst = Highlighter.safeHighlight(fmt.path("firstName").asText(first));
        final String highlightLast = Highlighter.safeHighlight(fmt.path("lastName").asText(last));
        final String highlight = (highlightFirst + " " + highlightLast).trim();
        final String snippet = hit.path("email").asText("");
        return new SearchHitDto(id, label, snippet, highlight, scoreOf(hit), null, null);
    }

    private SearchHitDto tagHit(final JsonNode hit) {
        final UUID id = readId(hit);
        if (id == null) {
            return null;
        }
        final String name = hit.path("name").asText("");
        final String highlight = Highlighter.safeHighlight(hit.path("_formatted").path("name").asText(name));
        final String snippet = hit.path("description").asText("");
        return new SearchHitDto(id, name, snippet, highlight, scoreOf(hit), null, null);
    }

    private SearchHitDto commentHit(final JsonNode hit) {
        final UUID id = readId(hit);
        if (id == null) {
            return null;
        }
        final String text = hit.path("text").asText("");
        final String highlight = Highlighter.safeHighlight(hit.path("_formatted").path("text").asText(text));
        final String ownerType = hit.path("ownerType").asText(null);
        final String ownerIdStr = hit.path("ownerId").asText(null);
        UUID ownerId = null;
        try {
            ownerId = ownerIdStr == null ? null : UUID.fromString(ownerIdStr);
        } catch (final IllegalArgumentException ignored) {
            // Skip malformed owner refs — defensive; should not happen.
        }
        final String ownerLabel = hit.path("ownerLabel").asText("");
        final String label = ownerLabel.isBlank() ? truncate(text, 60) : ownerLabel;
        return new SearchHitDto(id, label, truncate(text, 120), highlight, scoreOf(hit),
            ownerType, ownerId);
    }

    private static UUID readId(final JsonNode hit) {
        final String idStr = hit.path("id").asText(null);
        if (idStr == null) {
            return null;
        }
        try {
            return UUID.fromString(idStr);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static double scoreOf(final JsonNode hit) {
        return hit.path("_rankingScore").asDouble(0.0);
    }

    private static String truncate(final String s, final int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @FunctionalInterface
    private interface HitMapper {
        SearchHitDto map(JsonNode hit);
    }
}
