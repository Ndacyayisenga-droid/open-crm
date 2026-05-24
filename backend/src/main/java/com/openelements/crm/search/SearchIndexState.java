package com.openelements.crm.search;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Shared mutable flag indicating whether {@link SearchIndexBootstrap} has
 * finished the initial reindex. The bootstrap runs asynchronously and the
 * search controller short-circuits to 503 while this is {@code true}.
 */
@Component
public class SearchIndexState {

    private final AtomicBoolean bootstrapping = new AtomicBoolean(true);

    public boolean isBootstrapping() {
        return bootstrapping.get();
    }

    public void markBootstrappingStarted() {
        bootstrapping.set(true);
    }

    public void markBootstrappingFinished() {
        bootstrapping.set(false);
    }
}
