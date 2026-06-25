package com.openelements.crm.search;

import com.openelements.crm.AbstractDbTest;
import com.openelements.spring.base.services.search.MeilisearchClient;
import com.openelements.spring.base.services.search.MeilisearchProperties;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Integration test base for the search package. Adds a singleton Meilisearch
 * container on top of {@link AbstractDbTest}'s Postgres container. The
 * {@code @AfterEach} hook clears the four {@code crm_*} indexes so each test
 * starts from an empty Meilisearch state.
 *
 * <p>Tests can call {@link #waitForIndex()} to block until the latest
 * Meilisearch write task reaches a terminal state.
 */
public abstract class AbstractSearchTest extends AbstractDbTest {

    private static final String MEILI_MASTER_KEY = "test-master-key";

    @SuppressWarnings("resource")
    static final GenericContainer<?> MEILI =
        new GenericContainer<>("getmeili/meilisearch:v1.10")
            .withExposedPorts(7700)
            .withEnv("MEILI_MASTER_KEY", MEILI_MASTER_KEY)
            .withEnv("MEILI_NO_ANALYTICS", "true")
            .withEnv("MEILI_ENV", "development")
            .waitingFor(Wait.forHttp("/health").forPort(7700));

    static {
        MEILI.start();
    }

    @DynamicPropertySource
    static void registerMeilisearchProperties(final DynamicPropertyRegistry registry) {
        registry.add("openelements.meilisearch.host",
            () -> "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700));
        registry.add("openelements.meilisearch.master-key", () -> MEILI_MASTER_KEY);
    }

    @Autowired
    private MeilisearchClient meilisearchClient;

    @Autowired
    private MeilisearchProperties meilisearchProperties;

    @Autowired
    private CrmIndexNames crmIndexNames;

    @Autowired
    @Qualifier("searchIndexExecutor")
    private Executor searchIndexExecutor;

    @AfterEach
    void resetIndexes() {
        // Delete all documents from each index but keep the index itself —
        // dropping the index loses settings and would 400 on the next search.
        for (final String idx : List.of(
            crmIndexNames.companies(),
            crmIndexNames.contacts(),
            crmIndexNames.tags(),
            crmIndexNames.comments())) {
            deleteAllDocuments(idx);
        }
    }

    private void deleteAllDocuments(final String indexUid) {
        try {
            org.springframework.web.client.RestClient.create()
                .delete()
                .uri(meilisearchProperties.host() + "/indexes/{u}/documents", indexUid)
                .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                    "Bearer " + MEILI_MASTER_KEY)
                .retrieve()
                .toBodilessEntity();
        } catch (final RuntimeException ignored) {
            // Index may not exist on the very first test; that's fine.
        }
    }

    /**
     * Blocks until a just-triggered write is fully indexed and searchable.
     *
     * <p>Indexing is asynchronous: an entity change publishes an event handled on
     * the {@code searchIndexExecutor} pool ({@code @Async}), which only then
     * submits the Meilisearch write task. Waiting on {@code waitForLatestTask}
     * alone races with that hand-off — under load the "latest task" may still be
     * an older one, and the query runs before the new document is indexed (the
     * cause of the intermittent typo-tolerance failure in CI).
     *
     * <p>This therefore first drains the async executor (so the write task has
     * been submitted to Meilisearch), then waits for that task to reach a
     * terminal state. Both steps poll observable state with a timeout — no fixed
     * "sleep and hope".
     */
    protected void waitForIndex() {
        drainSearchIndexExecutor();
        meilisearchClient.waitForLatestTask(Duration.ofSeconds(10));
    }

    private void drainSearchIndexExecutor() {
        final ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) searchIndexExecutor;
        final long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline
            && (executor.getActiveCount() > 0 || !executor.getThreadPoolExecutor().getQueue().isEmpty())) {
            try {
                Thread.sleep(20);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    protected MeilisearchClient meilisearchClient() {
        return meilisearchClient;
    }

    protected MeilisearchProperties meilisearchProperties() {
        return meilisearchProperties;
    }
}
