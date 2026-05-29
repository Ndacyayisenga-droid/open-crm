package com.openelements.crm.search;

import com.openelements.spring.base.services.search.MeilisearchProperties;
import org.springframework.stereotype.Component;

/**
 * The four CRM-specific Meilisearch index names, resolved against the
 * configured index prefix. This is the CRM-side replacement for the
 * {@code *Index()} accessors that previously lived on {@code MeilisearchProperties}
 * — the lib's properties type is now app-agnostic.
 */
@Component
public class CrmIndexNames {

    private final MeilisearchProperties props;

    public CrmIndexNames(final MeilisearchProperties props) {
        this.props = props;
    }

    public String companies() {
        return props.resolveIndex("companies");
    }

    public String contacts() {
        return props.resolveIndex("contacts");
    }

    public String tags() {
        return props.resolveIndex("tags");
    }

    public String comments() {
        return props.resolveIndex("comments");
    }
}
