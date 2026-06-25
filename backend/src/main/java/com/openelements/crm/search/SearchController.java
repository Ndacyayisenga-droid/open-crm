package com.openelements.crm.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global search endpoint. Delegates to {@link CrmSearchService} (shared with the
 * MCP {@code search} tool) and maps the "index warming up" state to
 * {@code 503 SERVICE_UNAVAILABLE} + {@code Retry-After: 30}.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Global typo-tolerant search via Meilisearch")
@SecurityRequirement(name = "oidc")
public class SearchController {

    private static final int RETRY_AFTER_SECONDS = 30;

    private final CrmSearchService searchService;

    public SearchController(final CrmSearchService searchService) {
        this.searchService = searchService;
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

        if (searchService.isBootstrapping()) {
            return ResponseEntity.status(503)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS))
                .body(Map.of("error", "search index is initializing"));
        }
        return ResponseEntity.ok(searchService.search(q, limit));
    }
}
