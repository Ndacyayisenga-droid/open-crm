package com.openelements.crm.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.SocialLinkDto;
import com.openelements.spring.base.services.tag.TagDto;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Pure unit tests for the DTO → Meilisearch document mapping. No Spring
 * context, no network — we mock the dependencies and assert on the produced
 * Maps.
 */
class SearchIndexServiceTest {

    private SearchIndexService service;
    private TagRepository tagRepository;
    private CompanyRepository companyRepository;
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        final MeilisearchClient client = Mockito.mock(MeilisearchClient.class);
        final MeilisearchProperties props =
            new MeilisearchProperties("http://localhost:7700", "k", "crm_", null);
        tagRepository = Mockito.mock(TagRepository.class);
        companyRepository = Mockito.mock(CompanyRepository.class);
        contactRepository = Mockito.mock(ContactRepository.class);
        final JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        service = new SearchIndexService(client, props, tagRepository, companyRepository,
            contactRepository, jdbc);
    }

    @Test
    void companyDocumentBuildsConcatenatedAddressAndResolvesTagNames() {
        final UUID tagId = UUID.randomUUID();
        final TagEntity tag = new TagEntity();
        tag.setName("partner");
        Mockito.when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        final CompanyDto dto = new CompanyDto(
            UUID.randomUUID(),
            "Open Elements GmbH",
            "info@open-elements.com",
            "https://open-elements.com",
            "Bonnstr.",
            "12",
            "53111",
            "Bonn",
            "DE",
            "+49",
            "desc",
            "Sparkasse",
            "BIC123",
            "DE89...",
            "DE123",
            true,
            true,
            5L,
            3L,
            List.of(tagId),
            Instant.now(),
            Instant.now());

        final Map<String, Object> doc = service.companyDocument(dto);

        assertEquals("Open Elements GmbH", doc.get("name"));
        assertEquals("Bonnstr. 12 53111 Bonn DE", doc.get("address"));
        assertEquals(List.of("partner"), doc.get("tagNames"));
        assertEquals(Boolean.TRUE, doc.get("brevo"));
        assertEquals(dto.id().toString(), doc.get("id"));
    }

    @Test
    void companyDocumentSkipsBlankAddressParts() {
        final CompanyDto dto = new CompanyDto(
            UUID.randomUUID(), "n", null, null,
            null,    // street null
            "12",   // houseNumber
            "",     // zipCode blank
            "Bonn",
            "DE",
            null, null, null, null, null, null,
            false, false, 0L, 0L, List.of(),
            Instant.now(), Instant.now());
        final Map<String, Object> doc = service.companyDocument(dto);
        assertEquals("12 Bonn DE", doc.get("address"));
    }

    @Test
    void contactDocumentExtractsSocialLinkValuesAndDenormalizesCompanyName() {
        final ContactDto dto = new ContactDto(
            UUID.randomUUID(),
            "Dr.",
            "Hendrik",
            "Ebbers",
            "hendrik@example.com",
            "Founder",
            null,
            List.of(
                new SocialLinkDto("GITHUB", "hendrikebbers", "https://github.com/hendrikebbers"),
                new SocialLinkDto("LINKEDIN", "hendrik-ebbers", "https://linkedin.com/in/hendrik-ebbers"),
                new SocialLinkDto("X", "", null)),
            "+49 123",
            "desc",
            UUID.randomUUID(),
            "Open Elements GmbH",
            0L,
            false,
            null,
            false,
            false,
            null,
            List.of(),
            Instant.now(),
            Instant.now());

        final Map<String, Object> doc = service.contactDocument(dto);

        assertEquals("Hendrik", doc.get("firstName"));
        assertEquals("Open Elements GmbH", doc.get("companyName"));
        assertEquals(List.of("hendrikebbers", "hendrik-ebbers"), doc.get("socialLinkValues"));
        assertEquals(List.of(), doc.get("tagNames"));
    }

    @Test
    void tagDocumentRoundtripsFields() {
        final TagDto dto = new TagDto(UUID.randomUUID(), "partner", "Long-term", "#ff0");
        final Map<String, Object> doc = service.tagDocument(dto);
        assertEquals("partner", doc.get("name"));
        assertEquals("Long-term", doc.get("description"));
        assertEquals("#ff0", doc.get("color"));
    }

    @Test
    void commentDocumentEmbedsOwnerRef() {
        final UUID id = UUID.randomUUID();
        final UUID ownerId = UUID.randomUUID();
        final SearchIndexService.OwnerRef owner =
            new SearchIndexService.OwnerRef("contact", ownerId, "Hendrik Ebbers");
        final var dto = new com.openelements.spring.base.services.comment.CommentDto(
            id, "renewal in March", null, Instant.now(), Instant.now());

        final Map<String, Object> doc = service.commentDocument(dto, owner);

        assertEquals("renewal in March", doc.get("text"));
        assertEquals("contact", doc.get("ownerType"));
        assertEquals(ownerId.toString(), doc.get("ownerId"));
        assertEquals("Hendrik Ebbers", doc.get("ownerLabel"));
        assertEquals(id.toString(), doc.get("id"));
    }

}
