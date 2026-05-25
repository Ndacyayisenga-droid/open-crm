package com.openelements.crm.search.lib;

/** Thin runtime exception so callers can distinguish Meilisearch errors. */
public final class MeilisearchException extends RuntimeException {
    public MeilisearchException(final String message) {
        super(message);
    }
}
