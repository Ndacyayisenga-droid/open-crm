package com.openelements.crm.search.lib;

import java.util.List;

/**
 * Declarative description of the scoped Meilisearch API key the application
 * wants minted at startup. The CRM side registers one such bean; the lib's
 * {@link MeilisearchScopedKeyInitializer} consumes it. When no bean is present,
 * the client keeps using the master key.
 *
 * @param indexes index patterns the key is restricted to (e.g. {@code crm_*})
 * @param actions allowed Meilisearch actions (e.g. {@code search})
 */
public record ScopedKeySpec(List<String> indexes, List<String> actions) {
}
