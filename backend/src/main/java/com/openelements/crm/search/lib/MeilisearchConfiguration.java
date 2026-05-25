package com.openelements.crm.search.lib;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the reusable Meilisearch building blocks. Component-scans this package
 * (so {@link MeilisearchClient}, the startup runners, and
 * {@link SearchReadinessState} are picked up) and binds
 * {@link MeilisearchProperties}.
 *
 * <p>An application activates the lib by importing this class from its own
 * {@code @Configuration} ({@code @Import(MeilisearchConfiguration.class)}). The
 * lib intentionally takes no stance on async execution or on which indexes
 * exist — the importing application supplies a {@code searchIndexExecutor},
 * an optional {@link ScopedKeySpec} bean, {@link IndexSettings} beans, and
 * {@link SearchIndexBootstrapStep} beans.
 *
 * <p>Transitional location: this package will move to
 * {@code com.openelements.spring.meilisearch} in {@code spring-services} as a
 * mechanical rename.
 */
@Configuration
@ComponentScan("com.openelements.crm.search.lib")
@EnableConfigurationProperties(MeilisearchProperties.class)
public class MeilisearchConfiguration {
}
