package com.openelements.crm.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Wires the search package: binds {@link MeilisearchProperties}, creates the
 * shared {@link MeilisearchClient} bean, and registers the {@code
 * searchIndexExecutor} thread pool used by the bootstrap and the event
 * listener.
 */
@Configuration
@EnableConfigurationProperties(MeilisearchProperties.class)
@EnableAsync
public class SearchConfiguration {

    @Bean
    public MeilisearchClient meilisearchClient(final MeilisearchProperties props,
                                               final ObjectMapper objectMapper) {
        return new MeilisearchClient(props, objectMapper);
    }

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
}
