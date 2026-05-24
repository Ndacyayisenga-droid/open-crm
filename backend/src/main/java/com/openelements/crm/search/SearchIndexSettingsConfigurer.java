package com.openelements.crm.search;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Idempotently writes the per-index Meilisearch settings (searchable /
 * filterable / sortable attributes) for all four CRM indexes. Runs once at
 * startup, after {@link MeilisearchKeyDeriver}. Idempotent: re-running the
 * same settings is a no-op for Meilisearch.
 */
@Component
@Order(20)
public class SearchIndexSettingsConfigurer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexSettingsConfigurer.class);

    private final MeilisearchClient client;
    private final MeilisearchProperties props;

    public SearchIndexSettingsConfigurer(final MeilisearchClient client,
                                         final MeilisearchProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (!client.isHealthy()) {
            log.warn("Skipping index-settings configuration — Meilisearch is not reachable.");
            return;
        }
        try {
            configureCompanies();
            configureContacts();
            configureTags();
            configureComments();
            log.info("Meilisearch: index settings configured for {} indexes.", 4);
        } catch (final RuntimeException e) {
            log.error("Meilisearch: failed to configure index settings — search may behave "
                + "unexpectedly. Error: {}", e.toString(), e);
        }
    }

    private void configureCompanies() {
        client.ensureIndex(props.companiesIndex(), "id");
        client.updateSettings(props.companiesIndex(), Map.of(
            "searchableAttributes", List.of("name", "email", "website", "address",
                "phoneNumber", "description", "bankName", "vatId", "tagNames"),
            "filterableAttributes", List.of("brevo", "tagNames")));
    }

    private void configureContacts() {
        client.ensureIndex(props.contactsIndex(), "id");
        client.updateSettings(props.contactsIndex(), Map.of(
            "searchableAttributes", List.of("firstName", "lastName", "email", "position",
                "phoneNumber", "description", "socialLinkValues", "companyName",
                "tagNames", "title"),
            "filterableAttributes", List.of("companyId", "brevo", "tagNames")));
    }

    private void configureTags() {
        client.ensureIndex(props.tagsIndex(), "id");
        client.updateSettings(props.tagsIndex(), Map.of(
            "searchableAttributes", List.of("name", "description")));
    }

    private void configureComments() {
        client.ensureIndex(props.commentsIndex(), "id");
        client.updateSettings(props.commentsIndex(), Map.of(
            "searchableAttributes", List.of("text", "ownerLabel"),
            "filterableAttributes", List.of("ownerType")));
    }
}
