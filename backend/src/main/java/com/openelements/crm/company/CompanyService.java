package com.openelements.crm.company;

import com.openelements.crm.contact.ContactRepository;
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
 * Service handling company business logic including CRUD operations and hard-delete.
 */
@Service
@Transactional
public class CompanyService extends AbstractDbBackedDataService<CompanyEntity, CompanyDto> {

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public CompanyService(final CompanyRepository companyRepository,
                          final ContactRepository contactRepository,
                          final CommentService commentService,
                          final CommentRepository commentRepository,
                          final TagRepository tagRepository,
                          final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
    }

    /**
     * Lists companies with pagination, filtering, and sorting.
     *
     * @param name     partial name filter (case-insensitive)
     * @param brevo    filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param tagIds   filter by tag IDs (AND semantics)
     * @param pageable pagination and sorting parameters
     * @return a page of company responses
     */
    @Transactional(readOnly = true)
    public Page<CompanyDto> list(final String name,
                                 final Boolean brevo,
                                 final List<UUID> tagIds,
                                 final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<CompanyEntity> spec = Specification.where(null);

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoCompanyId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoCompanyId")));
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

        return companyRepository.findAll(spec, pageable).map(e -> toData(e));
    }

    /**
     * Lists all companies matching the given filters without pagination.
     *
     * @param name   partial name filter (case-insensitive)
     * @param brevo  filter by Brevo origin
     * @param tagIds filter by tag IDs (AND semantics)
     * @return list of all matching company DTOs
     */
    @Transactional(readOnly = true)
    public List<CompanyDto> listAll(final String name,
                                    final Boolean brevo,
                                    final List<UUID> tagIds) {
        Specification<CompanyEntity> spec = Specification.where(null);

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoCompanyId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoCompanyId")));
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

        return companyRepository.findAll(spec, Sort.by("name")).stream()
            .map(e -> toData(e))
            .toList();
    }

    public void updateLogo(final UUID id, final ImageData imageData) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(imageData, "imageData must not be null");
        final CompanyEntity entity = companyRepository.findByIdOrThrow(id);
        entity.setImageData(imageData);
        companyRepository.saveAndFlush(entity);
    }

    /**
     * Returns the logo data for a company.
     *
     * @param id the company ID
     * @return the image data
     * @throws ResponseStatusException with 404 if not found or no logo exists
     */
    @Transactional(readOnly = true)
    public Optional<ImageData> getLogo(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return companyRepository.findByIdOrThrow(id)
            .imageData();
    }

    /**
     * Removes the logo from a company.
     *
     * @param id the company ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void deleteLogo(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = companyRepository.findByIdOrThrow(id);
        entity.setImageData(null);
        companyRepository.saveAndFlush(entity);
    }

    /**
     * Lists comments attached to a company.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> listCommentsOfCompany(final UUID companyId) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        final CompanyEntity company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        return company.getComments().stream()
            .map(c -> commentService.findById(c.getId()).orElseThrow())
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .toList();
    }

    /**
     * Adds a comment to a company. The comment row and the {@code company_comments} join row
     * are inserted in the same transaction.
     */
    public CommentDto addCommentToCompany(final UUID companyId, final CommentCreateDto request) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CompanyEntity company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        final CommentDto saved = commentService.save(new CommentDto(null, request.text(), null, null, null));
        final CommentEntity entity = commentRepository.findByIdOrThrow(saved.id());
        company.getComments().add(entity);
        companyRepository.save(company);
        return saved;
    }

    /**
     * Updates a comment that is attached to the given company. Returns 404 if the comment is not
     * attached to this company.
     */
    public CommentDto updateCommentOfCompany(final UUID companyId, final UUID commentId, final CommentCreateDto request) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        assertCommentBelongsToCompany(companyId, commentId);
        final CommentDto current = commentService.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        return commentService.save(new CommentDto(commentId, request.text(), current.author(), current.createdAt(), current.updatedAt()));
    }

    /**
     * Deletes a comment that is attached to the given company. Removes both the join row and
     * the comment row.
     */
    public void deleteCommentOfCompany(final UUID companyId, final UUID commentId) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        assertCommentBelongsToCompany(companyId, commentId);
        final CompanyEntity company = companyRepository.findByIdOrThrow(companyId);
        company.getComments().removeIf(c -> c.getId().equals(commentId));
        companyRepository.saveAndFlush(company);
        commentService.delete(commentId);
    }

    private void assertCommentBelongsToCompany(final UUID companyId, final UUID commentId) {
        final CompanyEntity company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        final boolean belongs = company.getComments().stream()
            .anyMatch(c -> c.getId().equals(commentId));
        if (!belongs) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found for this company");
        }
    }

    @Override
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity company = companyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
        final List<UUID> commentIds = new ArrayList<>(
            company.getComments().stream().map(CommentEntity::getId).toList());
        company.getComments().clear();
        companyRepository.saveAndFlush(company);
        commentIds.forEach(commentService::delete);
        companyRepository.delete(company);
    }

    @Override
    protected CompanyEntity createDetachedEntity() {
        return new CompanyEntity();
    }

    @Override
    protected void updateEntity(CompanyEntity entity, CompanyDto data) {
        entity.setName(data.name());
        entity.setEmail(data.email());
        entity.setWebsite(data.website());
        entity.setStreet(data.street());
        entity.setHouseNumber(data.houseNumber());
        entity.setZipCode(data.zipCode());
        entity.setCity(data.city());
        entity.setCountry(data.country());
        entity.setPhoneNumber(data.phoneNumber());
        entity.setDescription(data.description());
        entity.setBankName(data.bankName());
        entity.setBic(data.bic());
        entity.setIban(data.iban());
        entity.setVatId(data.vatId());
        final Set<TagEntity> tags = Optional.ofNullable(data.tagIds())
            .orElse(List.of()).stream()
            .map(id -> tagRepository.findByIdOrThrow(id))
            .collect(Collectors.toSet());
        entity.setTags(tags);
    }

    @Override
    protected CompanyDto toData(CompanyEntity entity) {
        return new CompanyDto(entity.getId(),
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
            contactRepository.countByCompanyId(entity.getId()),
            entity.getComments().size(),
            entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    @Override
    protected EntityRepository<CompanyEntity> getRepository() {
        return companyRepository;
    }

    public long countWithTag(UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return companyRepository.findAll()
            .stream()
            .filter(contact -> contact.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }
}
