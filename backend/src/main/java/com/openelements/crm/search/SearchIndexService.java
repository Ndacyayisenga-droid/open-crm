package com.openelements.crm.search;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.SocialLinkDto;
import com.openelements.crm.search.lib.MeilisearchClient;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.tag.TagDto;
import com.openelements.spring.base.services.tag.TagRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Maps domain DTOs to Meilisearch documents and dispatches the writes to
 * {@link MeilisearchClient}. The service is invoked from the four
 * {@code *BootstrapStep} beans (batched, startup) and
 * {@link SearchIndexEventListener} (per-event, runtime).
 *
 * <p>Comment owner resolution: {@code CommentEntity} itself does not carry
 * an owner FK — the link lives in {@code company_comments / contact_comments
 * / task_comments}. The service queries these three join tables to find which
 * Company / Contact / Task currently owns each comment and writes {@code
 * ownerType / ownerId / ownerLabel} into the document.
 */
@Service
public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private final MeilisearchClient client;
    private final CrmIndexNames indexNames;
    private final TagRepository tagRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final JdbcTemplate jdbcTemplate;

    public SearchIndexService(final MeilisearchClient client,
                              final CrmIndexNames indexNames,
                              final TagRepository tagRepository,
                              final CompanyRepository companyRepository,
                              final ContactRepository contactRepository,
                              final JdbcTemplate jdbcTemplate) {
        this.client = client;
        this.indexNames = indexNames;
        this.tagRepository = tagRepository;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // -- Companies --

    public void upsertCompany(final CompanyDto dto) {
        client.addDocuments(indexNames.companies(), List.of(companyDocument(dto)));
    }

    public void deleteCompany(final UUID id) {
        client.deleteDocument(indexNames.companies(), id.toString());
    }

    public Map<String, Object> companyDocument(final CompanyDto dto) {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", dto.id().toString());
        doc.put("name", nullSafe(dto.name()));
        doc.put("email", nullSafe(dto.email()));
        doc.put("website", nullSafe(dto.website()));
        doc.put("address", buildAddress(dto));
        doc.put("phoneNumber", nullSafe(dto.phoneNumber()));
        doc.put("description", nullSafe(dto.description()));
        doc.put("bankName", nullSafe(dto.bankName()));
        doc.put("vatId", nullSafe(dto.vatId()));
        doc.put("brevo", dto.brevo());
        doc.put("tagNames", resolveTagNames(dto.tagIds()));
        return doc;
    }

    private static String buildAddress(final CompanyDto dto) {
        final StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, dto.street());
        appendIfPresent(sb, dto.houseNumber());
        appendIfPresent(sb, dto.zipCode());
        appendIfPresent(sb, dto.city());
        appendIfPresent(sb, dto.country());
        return sb.toString().trim();
    }

    private static void appendIfPresent(final StringBuilder sb, final String part) {
        if (part != null && !part.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part);
        }
    }

    // -- Contacts --

    public void upsertContact(final ContactDto dto) {
        client.addDocuments(indexNames.contacts(), List.of(contactDocument(dto)));
    }

    public void deleteContact(final UUID id) {
        client.deleteDocument(indexNames.contacts(), id.toString());
    }

    public Map<String, Object> contactDocument(final ContactDto dto) {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", dto.id().toString());
        doc.put("title", nullSafe(dto.title()));
        doc.put("firstName", nullSafe(dto.firstName()));
        doc.put("lastName", nullSafe(dto.lastName()));
        doc.put("email", nullSafe(dto.email()));
        doc.put("position", nullSafe(dto.position()));
        doc.put("phoneNumber", nullSafe(dto.phoneNumber()));
        doc.put("description", nullSafe(dto.description()));
        doc.put("socialLinkValues", socialLinkValues(dto.socialLinks()));
        doc.put("companyId", dto.companyId() == null ? null : dto.companyId().toString());
        doc.put("companyName", nullSafe(dto.companyName()));
        doc.put("brevo", dto.brevo());
        doc.put("tagNames", resolveTagNames(dto.tagIds()));
        return doc;
    }

    private static List<String> socialLinkValues(final List<SocialLinkDto> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
            .map(SocialLinkDto::value)
            .filter(Objects::nonNull)
            .filter(v -> !v.isBlank())
            .toList();
    }

    // -- Tags --

    public void upsertTag(final TagDto dto) {
        client.addDocuments(indexNames.tags(), List.of(tagDocument(dto)));
    }

    public void deleteTag(final UUID id) {
        client.deleteDocument(indexNames.tags(), id.toString());
    }

    public Map<String, Object> tagDocument(final TagDto dto) {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", dto.id().toString());
        doc.put("name", nullSafe(dto.name()));
        doc.put("description", nullSafe(dto.description()));
        doc.put("color", nullSafe(dto.color()));
        return doc;
    }

    // -- Comments --

    public void upsertComment(final CommentDto dto) {
        final Optional<OwnerRef> owner = resolveCommentOwner(dto.id());
        if (owner.isEmpty()) {
            // Comment exists in the comments table but is not yet attached to
            // any owner — happens during the brief window between INSERT comment
            // and INSERT join-row. The matching update event will land later.
            log.debug("Skipping index of orphan comment {}", dto.id());
            return;
        }
        client.addDocuments(indexNames.comments(), List.of(commentDocument(dto, owner.get())));
    }

    public void deleteComment(final UUID id) {
        client.deleteDocument(indexNames.comments(), id.toString());
    }

    public Map<String, Object> commentDocument(final CommentDto dto, final OwnerRef owner) {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", dto.id().toString());
        doc.put("text", nullSafe(dto.text()));
        doc.put("ownerType", owner.type);
        doc.put("ownerId", owner.id.toString());
        doc.put("ownerLabel", owner.label);
        return doc;
    }

    /** Looks up a comment's owner across the three join tables. */
    public Optional<OwnerRef> resolveCommentOwner(final UUID commentId) {
        final List<UUID> companyOwner = jdbcTemplate.queryForList(
            "SELECT company_id FROM company_comments WHERE comment_id = ?",
            UUID.class, commentId);
        if (!companyOwner.isEmpty()) {
            final UUID companyId = companyOwner.get(0);
            final String label = companyRepository.findById(companyId)
                .map(CompanyEntity::getName).orElse("");
            return Optional.of(new OwnerRef("company", companyId, label));
        }
        final List<UUID> contactOwner = jdbcTemplate.queryForList(
            "SELECT contact_id FROM contact_comments WHERE comment_id = ?",
            UUID.class, commentId);
        if (!contactOwner.isEmpty()) {
            final UUID contactId = contactOwner.get(0);
            final String label = contactRepository.findById(contactId)
                .map(this::contactDisplayName).orElse("");
            return Optional.of(new OwnerRef("contact", contactId, label));
        }
        final List<UUID> taskOwner = jdbcTemplate.queryForList(
            "SELECT task_id FROM task_comments WHERE comment_id = ?",
            UUID.class, commentId);
        if (!taskOwner.isEmpty()) {
            // We do not currently load the task entity into a JPA repository
            // local to this service; the label is left to the bare ID for v1.
            // The frontend uses ownerType + ownerId to navigate.
            return Optional.of(new OwnerRef("task", taskOwner.get(0), ""));
        }
        return Optional.empty();
    }

    private String contactDisplayName(final ContactEntity entity) {
        final String first = entity.getFirstName() == null ? "" : entity.getFirstName();
        final String last = entity.getLastName() == null ? "" : entity.getLastName();
        return (first + " " + last).trim();
    }

    // -- Helpers --

    private List<String> resolveTagNames(final List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagIds.stream()
            .map(tagRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(t -> t.getName() == null ? "" : t.getName())
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static String nullSafe(final String value) {
        return value == null ? "" : value;
    }

    /** Lightweight value object for a comment's owner reference. */
    public record OwnerRef(String type, UUID id, String label) {
    }
}
