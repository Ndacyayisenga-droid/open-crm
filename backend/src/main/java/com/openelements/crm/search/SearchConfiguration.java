package com.openelements.crm.search;

import com.openelements.crm.search.lib.IndexSettings;
import com.openelements.crm.search.lib.MeilisearchConfiguration;
import com.openelements.crm.search.lib.ScopedKeySpec;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * CRM-side wiring for global search. Activates the reusable Meilisearch lib via
 * {@link MeilisearchConfiguration} and supplies everything the lib leaves to
 * the application: the {@code searchIndexExecutor} thread pool, the scoped-key
 * specification, and the per-index settings for the four CRM indexes.
 *
 * <p>The four {@link SearchIndexBootstrapStep} beans and {@link CrmIndexNames}
 * are {@code @Component}s discovered by the application's component scan.
 */
@Configuration
@Import(MeilisearchConfiguration.class)
@EnableAsync
public class SearchConfiguration {

    @Bean(name = "searchIndexExecutor")
    public Executor searchIndexExecutor() {
        final ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(500);
        exec.setThreadNamePrefix("search-index-");
        exec.initialize();
        return exec;
    }

    @Bean
    public ScopedKeySpec crmScopedKey() {
        return new ScopedKeySpec(
            List.of("crm_*"),
            List.of(
                "search",
                "documents.add",
                "documents.get",
                "documents.delete",
                "indexes.create",
                "indexes.get",
                "indexes.update",
                "settings.update",
                "settings.get",
                "tasks.get"));
    }

    @Bean
    public IndexSettings companiesSettings(final CrmIndexNames names) {
        return new IndexSettings(names.companies(), "id",
            List.of("name", "email", "website", "address", "phoneNumber",
                "description", "bankName", "vatId", "tagNames"),
            List.of("brevo", "tagNames"),
            List.of());
    }

    @Bean
    public IndexSettings contactsSettings(final CrmIndexNames names) {
        return new IndexSettings(names.contacts(), "id",
            List.of("firstName", "lastName", "email", "position", "phoneNumber",
                "description", "socialLinkValues", "companyName", "tagNames", "title"),
            List.of("companyId", "brevo", "tagNames"),
            List.of());
    }

    @Bean
    public IndexSettings tagsSettings(final CrmIndexNames names) {
        return new IndexSettings(names.tags(), "id",
            List.of("name", "description"),
            List.of(),
            List.of());
    }

    @Bean
    public IndexSettings commentsSettings(final CrmIndexNames names) {
        return new IndexSettings(names.comments(), "id",
            List.of("text", "ownerLabel"),
            List.of("ownerType"),
            List.of());
    }
}
