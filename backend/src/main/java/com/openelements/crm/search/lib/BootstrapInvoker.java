package com.openelements.crm.search.lib;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Indirection so {@link MeilisearchBootstrapRunner} can run the reindex on the
 * {@code searchIndexExecutor} pool via Spring's {@code @Async} proxy (a
 * self-invoked {@code @Async} method would not be proxied). Tests can replace
 * this bean with a synchronous implementation for deterministic assertions.
 */
@Component
public class BootstrapInvoker {

    @Async("searchIndexExecutor")
    public void run(final Runnable task) {
        task.run();
    }
}
