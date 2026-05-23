package com.openelements.crm.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.spring.base.events.OnObjectCreate;
import com.openelements.spring.base.events.OnObjectDelete;
import com.openelements.spring.base.events.OnObjectUpdate;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.tag.TagDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit-tests the dispatch table in {@link SearchIndexEventListener}. Skips
 * Spring entirely — the listener is a plain bean with two collaborators.
 */
class SearchIndexEventListenerTest {

    private SearchIndexService indexService;
    private SearchIndexState state;
    private SearchIndexEventListener listener;

    @BeforeEach
    void setUp() {
        indexService = Mockito.mock(SearchIndexService.class);
        state = Mockito.mock(SearchIndexState.class);
        when(state.isBootstrapping()).thenReturn(false);
        listener = new SearchIndexEventListener(indexService, state);
    }

    @Test
    void onObjectCreateDispatchesContactToUpsertContact() {
        final ContactDto dto = contactDto();
        listener.onObjectCreate(new OnObjectCreate<>(dto));
        verify(indexService).upsertContact(dto);
        verify(indexService, never()).upsertCompany(any());
    }

    @Test
    void onObjectCreateDispatchesCompanyToUpsertCompany() {
        final CompanyDto dto = companyDto();
        listener.onObjectCreate(new OnObjectCreate<>(dto));
        verify(indexService).upsertCompany(dto);
    }

    @Test
    void onObjectUpdateDispatchesTagToUpsertTag() {
        final TagDto dto = new TagDto(UUID.randomUUID(), "n", null, null);
        listener.onObjectUpdate(new OnObjectUpdate<>(dto));
        verify(indexService).upsertTag(dto);
    }

    @Test
    void onObjectDeleteDispatchesContactToDeleteContact() {
        final ContactDto dto = contactDto();
        listener.onObjectDelete(new OnObjectDelete<>(dto));
        verify(indexService).deleteContact(dto.id());
    }

    @Test
    void bootstrappingFlagSuppressesAllDispatch() {
        when(state.isBootstrapping()).thenReturn(true);
        listener.onObjectCreate(new OnObjectCreate<>(contactDto()));
        listener.onObjectUpdate(new OnObjectUpdate<>(companyDto()));
        listener.onObjectDelete(new OnObjectDelete<>(contactDto()));
        verify(indexService, times(0)).upsertContact(any());
        verify(indexService, times(0)).upsertCompany(any());
        verify(indexService, times(0)).deleteContact(any());
    }

    @Test
    void commentDeleteDispatchesToDeleteComment() {
        final CommentDto dto = new CommentDto(UUID.randomUUID(), "t", null, Instant.now(), Instant.now());
        listener.onObjectDelete(new OnObjectDelete<>(dto));
        verify(indexService).deleteComment(dto.id());
    }

    private static ContactDto contactDto() {
        return new ContactDto(UUID.randomUUID(), null, "A", "B", null, null, null, List.of(),
            null, null, null, null, 0L, false, null, false, false, null, List.of(),
            Instant.now(), Instant.now());
    }

    private static CompanyDto companyDto() {
        return new CompanyDto(UUID.randomUUID(), "N", null, null, null, null, null, null, null,
            null, null, null, null, null, null, false, false, 0L, 0L, List.of(),
            Instant.now(), Instant.now());
    }
}
