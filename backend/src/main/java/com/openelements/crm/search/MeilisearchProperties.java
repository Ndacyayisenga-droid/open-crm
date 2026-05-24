package com.openelements.crm.search;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Meilisearch sidecar. Bound from
 * {@code openelements.search.meilisearch.*} application properties.
 */
@ConfigurationProperties("openelements.search.meilisearch")
public record MeilisearchProperties(
    String host,
    String masterKey,
    String indexPrefix,
    Duration requestTimeout) {

    public MeilisearchProperties {
        if (host == null || host.isBlank()) {
            host = "http://localhost:7700";
        }
        if (indexPrefix == null || indexPrefix.isBlank()) {
            indexPrefix = "crm_";
        }
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(5);
        }
    }

    public String companiesIndex() {
        return indexPrefix + "companies";
    }

    public String contactsIndex() {
        return indexPrefix + "contacts";
    }

    public String tagsIndex() {
        return indexPrefix + "tags";
    }

    public String commentsIndex() {
        return indexPrefix + "comments";
    }
}
