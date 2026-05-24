package com.openelements.crm.search;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.tag.TagDto;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full reindex on every backend startup. Streams entities from Postgres and
 * pushes them in batches to Meilisearch. Runs in the {@code searchIndexExecutor}
 * thread pool so the HTTP listener is up immediately. While the bootstrap is
 * in flight, {@link SearchIndexState#isBootstrapping()} stays {@code true}
 * and {@link SearchController} returns 503.
 */
@Component
@Order(30)
public class SearchIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexBootstrap.class);
    private static final int BATCH_SIZE = 500;
    private static final Duration TASK_WAIT = Duration.ofSeconds(10);

    private final MeilisearchClient client;
    private final MeilisearchProperties props;
    private final SearchIndexService indexService;
    private final SearchIndexState state;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final BootstrapInvoker invoker;

    public SearchIndexBootstrap(final MeilisearchClient client,
                                final MeilisearchProperties props,
                                final SearchIndexService indexService,
                                final SearchIndexState state,
                                final CompanyRepository companyRepository,
                                final ContactRepository contactRepository,
                                final TagRepository tagRepository,
                                final CommentRepository commentRepository,
                                final BootstrapInvoker invoker) {
        this.client = client;
        this.props = props;
        this.indexService = indexService;
        this.state = state;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
        this.invoker = invoker;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (!client.isHealthy()) {
            log.warn("Skipping search-index bootstrap — Meilisearch is not reachable.");
            state.markBootstrappingFinished();
            return;
        }
        state.markBootstrappingStarted();
        invoker.run(this::doBootstrap);
    }

    void doBootstrap() {
        final long started = System.nanoTime();
        try {
            indexEntities("companies", props.companiesIndex(),
                companyRepository.findAll(), this::mapCompany, indexService::companyDocument);
            indexEntities("contacts", props.contactsIndex(),
                contactRepository.findAll(), this::mapContact, indexService::contactDocument);
            indexEntities("tags", props.tagsIndex(),
                tagRepository.findAll(), this::mapTag, indexService::tagDocument);
            indexComments();
            log.info("Search-index bootstrap finished in {} ms.",
                (System.nanoTime() - started) / 1_000_000L);
        } catch (final RuntimeException e) {
            log.error("Search-index bootstrap failed; search will operate against a "
                + "partial index. Restart the backend to retry.", e);
        } finally {
            state.markBootstrappingFinished();
        }
    }

    private <E, D> void indexEntities(final String label,
                                      final String indexUid,
                                      final List<E> entities,
                                      final Function<E, D> toDto,
                                      final Function<D, Map<String, Object>> toDocument) {
        if (entities.isEmpty()) {
            log.info("Bootstrap: 0 {} to index.", label);
            return;
        }
        final List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
        int pushed = 0;
        for (final E entity : entities) {
            final D dto = toDto.apply(entity);
            if (dto == null) {
                continue;
            }
            batch.add(toDocument.apply(dto));
            if (batch.size() >= BATCH_SIZE) {
                pushed += flushBatch(indexUid, batch);
            }
        }
        if (!batch.isEmpty()) {
            pushed += flushBatch(indexUid, batch);
        }
        log.info("Bootstrap: pushed {} {} to {}.", pushed, label, indexUid);
    }

    private int flushBatch(final String indexUid, final List<Map<String, Object>> batch) {
        final int n = batch.size();
        final long taskUid = client.addDocuments(indexUid, List.copyOf(batch));
        batch.clear();
        final MeilisearchClient.TaskOutcome outcome = client.waitForTask(taskUid, TASK_WAIT);
        if (outcome != MeilisearchClient.TaskOutcome.SUCCEEDED) {
            log.warn("Meilisearch addDocuments task {} for {} ended with status {} — "
                + "batch of {} documents may not be searchable until next restart.",
                taskUid, indexUid, outcome, n);
        }
        return n;
    }

    private void indexComments() {
        final List<CommentEntity> all = commentRepository.findAll();
        if (all.isEmpty()) {
            log.info("Bootstrap: 0 comments to index.");
            return;
        }
        final List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
        int pushed = 0;
        int skipped = 0;
        for (final CommentEntity entity : all) {
            final Optional<SearchIndexService.OwnerRef> owner =
                indexService.resolveCommentOwner(entity.getId());
            if (owner.isEmpty()) {
                skipped++;
                continue;
            }
            final CommentDto dto = mapComment(entity);
            batch.add(indexService.commentDocument(dto, owner.get()));
            if (batch.size() >= BATCH_SIZE) {
                pushed += flushBatch(props.commentsIndex(), batch);
            }
        }
        if (!batch.isEmpty()) {
            pushed += flushBatch(props.commentsIndex(), batch);
        }
        log.info("Bootstrap: pushed {} comments to {} ({} orphan(s) skipped).",
            pushed, props.commentsIndex(), skipped);
    }

    // -- Entity → DTO mappers for the bootstrap path. The runtime event path
    // already receives DTOs from the upstream services. --

    private CompanyDto mapCompany(final CompanyEntity entity) {
        Objects.requireNonNull(entity);
        return new CompanyDto(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getWebsite(),
            entity.getStreet(),
            entity.getHouseNumber(),
            entity.getZipCode(),
            entity.getCity(),
            entity.getCountry(),
            entity.getPhoneNumber(),
            entity.getDescription(),
            entity.getBankName(),
            entity.getBic(),
            entity.getIban(),
            entity.getVatId(),
            entity.getLogo() != null,
            entity.getBrevoCompanyId() != null,
            0L,
            0L,
            entity.getTags() == null ? List.of()
                : entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    private ContactDto mapContact(final ContactEntity entity) {
        Objects.requireNonNull(entity);
        final CompanyEntity company = entity.getCompany();
        return new ContactDto(
            entity.getId(),
            entity.getTitle(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getEmail(),
            entity.getPosition(),
            entity.getGender(),
            entity.getSocialLinks() == null ? List.of()
                : entity.getSocialLinks().stream()
                    .map(l -> new com.openelements.crm.contact.SocialLinkDto(
                        l.getNetworkType().name(), l.getValue(), l.getUrl()))
                    .toList(),
            entity.getPhoneNumber(),
            entity.getDescription(),
            company == null ? null : company.getId(),
            company == null ? null : company.getName(),
            0L,
            entity.getPhoto() != null,
            entity.getBirthday(),
            entity.getBrevoId() != null,
            entity.isReceivesNewsletter(),
            entity.getLanguage(),
            entity.getTags() == null ? List.of()
                : entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    private TagDto mapTag(final TagEntity entity) {
        Objects.requireNonNull(entity);
        return new TagDto(entity.getId(), entity.getName(), entity.getDescription(), entity.getColor());
    }

    private CommentDto mapComment(final CommentEntity entity) {
        return new CommentDto(entity.getId(), entity.getText(), null, entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    /**
     * Indirection so {@link #doBootstrap()} can run on the {@code searchIndexExecutor}
     * pool via Spring's {@code @Async} proxy. Tests can replace this bean with
     * a synchronous implementation for deterministic assertions.
     */
    @Component
    public static class BootstrapInvoker {
        @Async("searchIndexExecutor")
        public void run(final Runnable task) {
            task.run();
        }
    }
}
