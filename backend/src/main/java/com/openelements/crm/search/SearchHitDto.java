package com.openelements.crm.search;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * A single search hit. {@code ownerType} and {@code ownerId} are only populated
 * for hits coming from the comments index — they identify the entity the
 * comment is attached to so the frontend can navigate there directly.
 */
@Schema(description = "A single search hit")
public record SearchHitDto(
    @Schema(description = "Entity ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(description = "Display label (entity name, contact display name, etc.)",
        requiredMode = Schema.RequiredMode.REQUIRED) String label,
    @Schema(description = "Short context snippet from the matched field") String snippet,
    @Schema(description = "Highlighted fragment with <em> markup around the matched terms") String highlight,
    @Schema(description = "Meilisearch relevance score") double score,
    @Schema(description = "For comment hits only: type of the owning entity (company | contact | task)") String ownerType,
    @Schema(description = "For comment hits only: ID of the owning entity") UUID ownerId
) {
}
