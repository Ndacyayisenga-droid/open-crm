package com.openelements.crm.search;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.SocialLinkDto;
import com.openelements.spring.base.services.search.SearchIndexBootstrapStep;
import com.openelements.spring.base.services.tag.TagEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bootstrap step that streams all contacts into the contacts index.
 */
@Component
@Order(20)
public class ContactsBootstrapStep implements SearchIndexBootstrapStep {

    private final CrmIndexNames indexNames;
    private final ContactRepository contactRepository;
    private final SearchIndexService indexService;

    public ContactsBootstrapStep(final CrmIndexNames indexNames,
                                 final ContactRepository contactRepository,
                                 final SearchIndexService indexService) {
        this.indexNames = indexNames;
        this.contactRepository = contactRepository;
        this.indexService = indexService;
    }

    @Override
    public String indexUid() {
        return indexNames.contacts();
    }

    @Override
    public Stream<Map<String, Object>> documents() {
        return contactRepository.findAll().stream()
            .map(ContactsBootstrapStep::mapContact)
            .map(indexService::contactDocument);
    }

    private static ContactDto mapContact(final ContactEntity entity) {
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
                    .map(l -> new SocialLinkDto(
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
}
