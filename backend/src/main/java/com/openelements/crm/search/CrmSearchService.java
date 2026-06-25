package com.openelements.crm.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.spring.base.services.search.Highlighter;
import com.openelements.spring.base.services.search.MeilisearchClient;
import com.openelements.spring.base.services.search.SearchReadinessState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Global search over the four CRM Meilisearch indexes (companies, contacts,
 * tags, comments). Extracted from {@code SearchController} so both the REST
 * endpoint and the MCP {@code search} tool (spec 108) share one implementation.
 *
 * <p>Fans out a single {@code POST /multi-search} and groups the hits per
 * section. The readiness check is exposed separately ({@link #isBootstrapping()})
 * so each caller maps the "index still warming up" state to its own protocol
 * (HTTP 503 for REST, a JSON-RPC error for MCP).
 */
@Service
public class CrmSearchService {

    /** Minimum query length; shorter queries return empty sections. */
    public static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final MeilisearchClient client;
    private final CrmIndexNames indexNames;
    private final SearchReadinessState state;

    public CrmSearchService(final MeilisearchClient client,
                            final CrmIndexNames indexNames,
                            final SearchReadinessState state) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.indexNames = Objects.requireNonNull(indexNames, "indexNames must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    /**
     * @return {@code true} while the startup index bootstrap (spec 104) is still running
     */
    public boolean isBootstrapping() {
        return state.isBootstrapping();
    }

    /**
     * Runs a grouped global search. Callers must check {@link #isBootstrapping()}
     * first and surface the unavailable state in their own protocol.
     *
     * @param q     the raw query (trimmed internally); shorter than {@value #MIN_QUERY_LENGTH} returns empty sections
     * @param limit maximum hits per section (non-positive → default {@value #DEFAULT_LIMIT}; capped at {@value #MAX_LIMIT})
     * @return the grouped result
     */
    public GlobalSearchResultDto search(final String q, final int limit) {
        final String query = q == null ? "" : q.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            return GlobalSearchResultDto.empty(query);
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

        return new GlobalSearchResultDto(
            query,
            extractHits(response, indexNames.companies(), this::companyHit),
            extractHits(response, indexNames.contacts(), this::contactHit),
            extractHits(response, indexNames.tags(), this::tagHit),
            extractHits(response, indexNames.comments(), this::commentHit));
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
     * Pulls hits for a specific index from the multi-search response by matching
     * {@code indexUid} rather than positional order — Meilisearch may reorder
     * errored sections.
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
