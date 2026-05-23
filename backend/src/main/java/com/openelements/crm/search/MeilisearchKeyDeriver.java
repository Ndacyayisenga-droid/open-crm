package com.openelements.crm.search;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Exchanges the master Meilisearch API key for a scoped runtime key restricted
 * to {@code crm_*} indexes. Runs once at application startup, before
 * {@link SearchIndexSettingsConfigurer} and {@link SearchIndexBootstrap}.
 *
 * <p>If the exchange fails, the client keeps using the master key — degraded
 * but functional. A WARN is logged so operators see the regression.
 */
@Component
@Order(10)
public class MeilisearchKeyDeriver implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchKeyDeriver.class);

    private static final List<String> SCOPED_INDEXES = List.of("crm_*");
    private static final List<String> SCOPED_ACTIONS = List.of(
        "search",
        "documents.add",
        "documents.get",
        "documents.delete",
        "indexes.create",
        "indexes.get",
        "indexes.update",
        "settings.update",
        "settings.get",
        "tasks.get");

    private final MeilisearchClient client;

    public MeilisearchKeyDeriver(final MeilisearchClient client) {
        this.client = client;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (!client.isHealthy()) {
            log.warn("Meilisearch is not reachable at startup. Search will be unavailable "
                + "until the sidecar is healthy.");
            return;
        }
        try {
            final String scoped = client.createScopedKey(SCOPED_INDEXES, SCOPED_ACTIONS);
            client.useApiKey(scoped);
            log.info("Meilisearch: exchanged master key for scoped runtime key (indexes: {}).",
                SCOPED_INDEXES);
        } catch (final RuntimeException e) {
            log.warn("Meilisearch: failed to mint a scoped key — runtime calls will continue "
                + "with the master key. Error: {}", e.toString());
        }
    }
}
