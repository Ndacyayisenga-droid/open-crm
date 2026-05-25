package com.openelements.crm.search.lib;

/** Terminal state of a Meilisearch async task as observed by polling. */
public enum TaskOutcome {
    SUCCEEDED,
    FAILED,
    TIMED_OUT
}
