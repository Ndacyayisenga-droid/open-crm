package com.openelements.crm.contact;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.comment.CommentService;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling contact business logic including CRUD operations and company validation.
 */
@Service
@Transactional
public class ContactService extends AbstractDbBackedDataService<ContactEntity, ContactDto> {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public ContactService(final ContactRepository contactRepository,
                          final CompanyRepository companyRepository,
                          final CommentService commentService,
                          final CommentRepository commentRepository,
                          final TagRepository tagRepository,
                          final ApplicationEventPublisher eventPublisher) {
        super((eventPublisher));
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
    }

    /**
     * Lists contacts with pagination, filtering, and sorting.
     *
     * @param search    multi-word search across firstName, lastName, email, and company name (case-insensitive)
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param pageable  pagination and sorting parameters
     * @return a page of contact responses
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> list(final String search,
                                 final UUID companyId,
                                 final boolean noCompany,
                                 final String language,
                                 final Boolean brevo,
                                 final List<UUID> tagIds,
                                 final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (companyId != null && noCompany) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot combine companyId and noCompany filters");
        }
        Specification<ContactEntity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            final String[] words = search.trim().split("\\s+");
            for (final String word : words) {
                final String pattern = "%" + word.toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> {
                    final Join<ContactEntity, CompanyEntity> companyJoin = root.join("company", JoinType.LEFT);
                    final Join<ContactEntity, SocialLinkEntity> socialLinksJoin = root.join("socialLinks", JoinType.LEFT);
                    query.distinct(true);
                    return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern),
                        cb.like(cb.lower(socialLinksJoin.get("value")), pattern)
                    );
                });
            }
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("company").get("id"), companyId));
        }
        if (noCompany) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("company")));
        }
        if (language != null && !language.isBlank()) {
            if ("UNKNOWN".equalsIgnoreCase(language)) {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("language")));
            } else {
                final Language lang = Language.valueOf(language.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("language"), lang));
            }
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoId")));
            }
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            for (final UUID tagId : tagIds) {
                spec = spec.and((root, query, cb) -> {
                    final var tagsJoin = root.join("tags");
                    return cb.equal(tagsJoin.get("id"), tagId);
                });
            }
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.conjunction();
            });
        }

        return contactRepository.findAll(spec, pageable).map(e -> toData(e));
    }

    /**
     * Lists all contacts matching the given filters without pagination.
     *
     * @param search    multi-word search across firstName, lastName, email, and company name
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin
     * @return list of all matching contact DTOs
     */
    @Transactional(readOnly = true)
    public List<ContactDto> listAll(final String search,
                                    final UUID companyId,
                                    final boolean noCompany,
                                    final String language,
                                    final Boolean brevo,
                                    final List<UUID> tagIds) {
        if (companyId != null && noCompany) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot combine companyId and noCompany filters");
        }
        Specification<ContactEntity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            final String[] words = search.trim().split("\\s+");
            for (final String word : words) {
                final String pattern = "%" + word.toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> {
                    final Join<ContactEntity, CompanyEntity> companyJoin = root.join("company", JoinType.LEFT);
                    final Join<ContactEntity, SocialLinkEntity> socialLinksJoin = root.join("socialLinks", JoinType.LEFT);
                    query.distinct(true);
                    return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern),
                        cb.like(cb.lower(socialLinksJoin.get("value")), pattern)
                    );
                });
            }
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("company").get("id"), companyId));
        }
        if (noCompany) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("company")));
        }
        if (language != null && !language.isBlank()) {
            if ("UNKNOWN".equalsIgnoreCase(language)) {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("language")));
            } else {
                final Language lang = Language.valueOf(language.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("language"), lang));
            }
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoId")));
            }
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            for (final UUID tagId : tagIds) {
                spec = spec.and((root, query, cb) -> {
                    final var tagsJoin = root.join("tags");
                    return cb.equal(tagsJoin.get("id"), tagId);
                });
            }
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.conjunction();
            });
        }

        return contactRepository.findAll(spec, Sort.by("lastName", "firstName")).stream()
            .map(e -> toData(e))
            .toList();
    }

    /**
     * Uploads or replaces the photo for a contact.
     *
     * @param id          the contact ID
     * @param data        the image bytes
     * @param contentType the MIME content type
     * @throws ResponseStatusException with 404 if not found, 400 if content type is not JPEG
     */
    public void uploadPhoto(final UUID id, final byte[] data, final String contentType) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (!"image/jpeg".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid content type: " + contentType + ". Only image/jpeg is allowed.");
        }
        final ContactEntity entity = contactRepository.findByIdOrThrow(id);
        entity.setPhoto(data);
        entity.setPhotoContentType(contentType);
        contactRepository.saveAndFlush(entity);
    }

    /**
     * Returns the photo data for a contact.
     *
     * @param id the contact ID
     * @return the image data
     * @throws ResponseStatusException with 404 if not found or no photo exists
     */
    @Transactional(readOnly = true)
    public Optional<ImageData> getPhoto(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return contactRepository.findByIdOrThrow(id)
            .imageData();
    }

    /**
     * Removes the photo from a contact.
     *
     * @param id the contact ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void deletePhoto(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity entity = contactRepository.findByIdOrThrow(id);
        entity.setImageData(null);
        contactRepository.saveAndFlush(entity);
    }

    /**
     * Lists comments attached to a contact.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> listCommentsOfContact(final UUID contactId) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        final ContactEntity contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        return contact.getComments().stream()
            .map(c -> commentService.findById(c.getId()).orElseThrow())
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .toList();
    }

    public CommentDto addCommentToContact(final UUID contactId, final CommentCreateDto request) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final ContactEntity contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        final CommentDto saved = commentService.save(new CommentDto(null, request.text(), null, null, null));
        final CommentEntity entity = commentRepository.findByIdOrThrow(saved.id());
        contact.getComments().add(entity);
        contactRepository.save(contact);
        return saved;
    }

    public CommentDto updateCommentOfContact(final UUID contactId, final UUID commentId, final CommentCreateDto request) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        assertCommentBelongsToContact(contactId, commentId);
        final CommentDto current = commentService.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        return commentService.save(new CommentDto(commentId, request.text(), current.author(), current.createdAt(), current.updatedAt()));
    }

    public void deleteCommentOfContact(final UUID contactId, final UUID commentId) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        assertCommentBelongsToContact(contactId, commentId);
        final ContactEntity contact = contactRepository.findByIdOrThrow(contactId);
        contact.getComments().removeIf(c -> c.getId().equals(commentId));
        contactRepository.saveAndFlush(contact);
        commentService.delete(commentId);
    }

    private void assertCommentBelongsToContact(final UUID contactId, final UUID commentId) {
        final ContactEntity contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        final boolean belongs = contact.getComments().stream()
            .anyMatch(c -> c.getId().equals(commentId));
        if (!belongs) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found for this contact");
        }
    }

    @Override
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity contact = contactRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));
        final List<UUID> commentIds = new ArrayList<>(
            contact.getComments().stream().map(CommentEntity::getId).toList());
        contact.getComments().clear();
        contactRepository.saveAndFlush(contact);
        commentIds.forEach(commentService::delete);
        super.delete(toData(contact));
    }

    @Override
    protected ContactEntity createDetachedEntity() {
        return new ContactEntity();
    }

    @Override
    protected EntityRepository<ContactEntity> getRepository() {
        return contactRepository;
    }

    @Override
    protected void updateEntity(ContactEntity entity, ContactDto data) {
        entity.setTitle(data.title());
        entity.setFirstName(data.firstName());
        entity.setLastName(data.lastName());
        entity.setEmail(data.email());
        entity.setPosition(data.position());
        entity.setGender(data.gender());
        entity.setPhoneNumber(data.phoneNumber());
        entity.setDescription(data.description());
        entity.setBirthday(data.birthday());
        entity.setLanguage(data.language());

        final List<SocialLinkEntity> socialLinks = Optional.ofNullable(data.socialLinks()).orElse(List.of())
            .stream().map(linkDto -> {
                final SocialNetworkType networkType = SocialNetworkType.valueOf(linkDto.networkType());
                final SocialNetworkType.ResolvedLink resolved = networkType.resolve(linkDto.value());
                final SocialLinkEntity linkEntity = new SocialLinkEntity();
                linkEntity.setNetworkType(networkType);
                linkEntity.setValue(resolved.value());
                linkEntity.setUrl(resolved.url());
                return linkEntity;
            }).toList();
        entity.getSocialLinks().clear();
        entity.getSocialLinks().addAll(socialLinks);

        final CompanyEntity company = Optional.ofNullable(data.companyId())
            .map(id -> companyRepository.findByIdOrThrow(id))
            .orElse(null);
        entity.setCompany(company);


        final Set<TagEntity> tags = Optional.ofNullable(data.tagIds()).orElse(List.of()).stream()
            .map(id -> tagRepository.findByIdOrThrow(id))
            .collect(Collectors.toSet());
        entity.setTags(tags);
    }

    @Override
    protected ContactDto toData(ContactEntity entity) {
        final long commentCount = entity.getComments().size();
        final UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        final String companyName = entity.getCompany() != null ? entity.getCompany().getName() : null;
        final List<UUID> tagIds = entity.getTags().stream()
            .map(TagEntity::getId)
            .toList();
        return new ContactDto(
            entity.getId(),
            entity.getTitle(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getEmail(),
            entity.getPosition(),
            entity.getGender(),
            entity.getSocialLinks().stream().map(e -> socialLinkFromEntity(e)).toList(),
            entity.getPhoneNumber(),
            entity.getDescription(),
            companyId,
            companyName,
            commentCount,
            entity.getPhoto() != null,
            entity.getBirthday(),
            entity.getBrevoId() != null,
            entity.isReceivesNewsletter(),
            entity.getLanguage(),
            tagIds,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @NonNull
    public List<ContactDto> getForCompany(@NonNull final UUID companyId) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        return contactRepository.findByCompanyId(companyId).stream()
            .map(e -> toData(e))
            .toList();
    }

    public long countWithTag(UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return contactRepository.findAll()
            .stream()
            .filter(contact -> contact.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }

    private static SocialLinkDto socialLinkFromEntity(final SocialLinkEntity entity) {
        return new SocialLinkDto(
            entity.getNetworkType().name(),
            entity.getValue(),
            entity.getUrl()
        );
    }
}
