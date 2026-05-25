package com.openelements.crm.search.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Verifies the {@code @ConfigurationProperties} prefix rename: the new
 * {@code openelements.meilisearch.*} prefix binds, while the old
 * {@code openelements.search.meilisearch.*} prefix is no longer honored.
 */
class MeilisearchPropertiesBindingTest {

    private static MeilisearchProperties bind(final Map<String, Object> properties) {
        final ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        return new Binder(source)
            .bindOrCreate("openelements.meilisearch", MeilisearchProperties.class);
    }

    @Test
    void renamedPrefixIsHonored() {
        final MeilisearchProperties props =
            bind(Map.of("openelements.meilisearch.host", "http://localhost:7700"));
        assertEquals("http://localhost:7700", props.host());
    }

    @Test
    void legacyPrefixIsNoLongerHonored() {
        final MeilisearchProperties props =
            bind(Map.of("openelements.search.meilisearch.host", "http://example:7700"));
        // Falls back to the record's default, not the legacy-prefixed value.
        assertEquals("http://localhost:7700", props.host());
    }
}
