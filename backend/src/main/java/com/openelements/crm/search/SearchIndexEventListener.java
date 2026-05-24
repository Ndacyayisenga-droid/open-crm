package com.openelements.crm.search;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.spring.base.events.GenericDataEvent;
import com.openelements.spring.base.events.OnObjectCreate;
import com.openelements.spring.base.events.OnObjectDelete;
import com.openelements.spring.base.events.OnObjectUpdate;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.tag.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridges Spring application events from
 * {@code com.openelements.spring.base.events} to the search index.
 *
 * <p>Listens to the abstract parent {@link GenericDataEvent} and dispatches
 * on the concrete event subtype + {@code event.getType()} at runtime — this
 * is more robust than declaring multiple {@code @EventListener} methods with
 * parameterized types, which Spring resolves via {@code ResolvableType} but
 * which can silently drop generic info during context publication.
 *
 * <p>All handling runs on the {@code searchIndexExecutor} pool so the
 * originating transaction commits without waiting on Meilisearch.
 */
@Component
public class SearchIndexEventListener {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexEventListener.class);

    private final SearchIndexService indexService;

    public SearchIndexEventListener(final SearchIndexService indexService) {
        this.indexService = indexService;
    }

    @Async("searchIndexExecutor")
    @EventListener
    public void onObjectCreate(final OnObjectCreate<?> event) {
        try {
            dispatchUpsert(event);
        } catch (final RuntimeException e) {
            log.warn("Search index upsert failed for type {} id {}: {}",
                event.getType().getSimpleName(), event.entityId(), e.toString());
        }
    }

    @Async("searchIndexExecutor")
    @EventListener
    public void onObjectUpdate(final OnObjectUpdate<?> event) {
        try {
            dispatchUpsert(event);
        } catch (final RuntimeException e) {
            log.warn("Search index update failed for type {} id {}: {}",
                event.getType().getSimpleName(), event.entityId(), e.toString());
        }
    }

    @Async("searchIndexExecutor")
    @EventListener
    public void onObjectDelete(final OnObjectDelete<?> event) {
        try {
            final Class<?> type = event.getType();
            if (CompanyDto.class.isAssignableFrom(type)) {
                indexService.deleteCompany(event.entityId());
            } else if (ContactDto.class.isAssignableFrom(type)) {
                indexService.deleteContact(event.entityId());
            } else if (TagDto.class.isAssignableFrom(type)) {
                indexService.deleteTag(event.entityId());
            } else if (CommentDto.class.isAssignableFrom(type)) {
                indexService.deleteComment(event.entityId());
            }
        } catch (final RuntimeException e) {
            log.warn("Search index delete failed for type {} id {}: {}",
                event.getType().getSimpleName(), event.entityId(), e.toString());
        }
    }

    private void dispatchUpsert(final GenericDataEvent<?> event) {
        final Class<?> type = event.getType();
        final Object data = event.getData();
        if (CompanyDto.class.isAssignableFrom(type) && data instanceof CompanyDto dto) {
            indexService.upsertCompany(dto);
        } else if (ContactDto.class.isAssignableFrom(type) && data instanceof ContactDto dto) {
            indexService.upsertContact(dto);
        } else if (TagDto.class.isAssignableFrom(type) && data instanceof TagDto dto) {
            indexService.upsertTag(dto);
        } else if (CommentDto.class.isAssignableFrom(type) && data instanceof CommentDto dto) {
            indexService.upsertComment(dto);
        }
    }
}
