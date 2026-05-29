package com.openelements.crm.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openelements.spring.base.services.search.MeilisearchProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrmIndexNames}: the four CRM index names are resolved
 * against the configured index prefix.
 */
class CrmIndexNamesTest {

    private static CrmIndexNames withPrefix(final String prefix) {
        return new CrmIndexNames(
            new MeilisearchProperties("http://localhost:7700", "k", prefix, Duration.ofSeconds(5)));
    }

    @Test
    void resolvesTheFourIndexNamesWithDefaultPrefix() {
        final CrmIndexNames names = withPrefix("crm_");
        assertEquals("crm_companies", names.companies());
        assertEquals("crm_contacts", names.contacts());
        assertEquals("crm_tags", names.tags());
        assertEquals("crm_comments", names.comments());
    }

    @Test
    void honorsACustomPrefix() {
        final CrmIndexNames names = withPrefix("test_");
        assertEquals("test_companies", names.companies());
        assertEquals("test_contacts", names.contacts());
        assertEquals("test_tags", names.tags());
        assertEquals("test_comments", names.comments());
    }
}
