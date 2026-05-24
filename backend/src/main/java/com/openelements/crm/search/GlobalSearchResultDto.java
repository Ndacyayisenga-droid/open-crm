package com.openelements.crm.search;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Result envelope for {@code GET /api/search} — one section per index, plus
 * the echoed query.
 */
@Schema(description = "Grouped global search result")
public record GlobalSearchResultDto(
    @Schema(description = "The query string that produced this result",
        requiredMode = Schema.RequiredMode.REQUIRED) String query,
    @Schema(description = "Matching companies", requiredMode = Schema.RequiredMode.REQUIRED) List<SearchHitDto> companies,
    @Schema(description = "Matching contacts", requiredMode = Schema.RequiredMode.REQUIRED) List<SearchHitDto> contacts,
    @Schema(description = "Matching tags", requiredMode = Schema.RequiredMode.REQUIRED) List<SearchHitDto> tags,
    @Schema(description = "Matching comments (each carries owner reference)",
        requiredMode = Schema.RequiredMode.REQUIRED) List<SearchHitDto> comments
) {
    public static GlobalSearchResultDto empty(final String query) {
        return new GlobalSearchResultDto(query == null ? "" : query,
            List.of(), List.of(), List.of(), List.of());
    }
}
