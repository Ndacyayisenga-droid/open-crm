package com.openelements.crm.updates;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import com.openelements.spring.base.services.audit.AuditLogDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the existing audit log and projects it into the user-facing Updates feed.
 * <p>
 * The service filters the audit stream to four entity types (company / contact / company-comment /
 * contact-comment), merges consecutive updates by the same user on the same entity, and resolves
 * current display names by querying the company / contact repositories.
 */
@Service
@Transactional(readOnly = true)
public class UpdatesService {

    private static final String COMPANY_ENTITY_TYPE = "CompanyDto";
    private static final String CONTACT_ENTITY_TYPE = "ContactDto";

    private static final Set<String> RELEVANT_ENTITY_TYPES = Set.of(
        COMPANY_ENTITY_TYPE,
        CONTACT_ENTITY_TYPE,
        CompanyService.COMMENT_ENTITY_TYPE,
        ContactService.COMMENT_ENTITY_TYPE
    );

    private static final int MIN_FETCH_PAGE_SIZE = 50;
    private static final int MAX_FETCH_PAGE_SIZE = 400;

    private final AuditLogDataService auditLogDataService;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;

    public UpdatesService(final AuditLogDataService auditLogDataService,
                          final CompanyRepository companyRepository,
                          final ContactRepository contactRepository) {
        this.auditLogDataService = Objects.requireNonNull(auditLogDataService, "auditLogDataService must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
    }

    /**
     * Loads the most recent updates, up to the given size, after filtering and dedupe.
     *
     * @param size maximum number of entries to return
     * @return the latest update entries, newest first
     */
    public List<UpdateEntryDto> load(final int size) {
        if (size <= 0) {
            return List.of();
        }
        final List<AuditLogDto> kept = collectKept(size);
        return resolveNames(kept);
    }

    private List<AuditLogDto> collectKept(final int size) {
        final List<AuditLogDto> kept = new ArrayList<>(size);
        final int fetchPageSize = Math.max(MIN_FETCH_PAGE_SIZE, Math.min(2 * size, MAX_FETCH_PAGE_SIZE));
        final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        int pageIndex = 0;
        while (kept.size() < size) {
            final Pageable pageable = PageRequest.of(pageIndex, fetchPageSize, sort);
            final Page<AuditLogDto> page = auditLogDataService.findAll(pageable);
            if (page.isEmpty()) {
                break;
            }
            for (final AuditLogDto entry : page.getContent()) {
                if (!RELEVANT_ENTITY_TYPES.contains(entry.entityType())) {
                    continue;
                }
                mergeOrAdd(kept, entry);
                if (kept.size() >= size) {
                    break;
                }
            }
            if (!page.hasNext()) {
                break;
            }
            pageIndex++;
        }
        // Trim if dedupe pushed past size right before the break check.
        if (kept.size() > size) {
            return new ArrayList<>(kept.subList(0, size));
        }
        return kept;
    }

    private static void mergeOrAdd(final List<AuditLogDto> kept, final AuditLogDto entry) {
        if (!kept.isEmpty() && entry.action() == AuditAction.UPDATE) {
            final AuditLogDto last = kept.get(kept.size() - 1);
            if (last.action() == AuditAction.UPDATE
                && Objects.equals(last.entityType(), entry.entityType())
                && Objects.equals(last.entityId(), entry.entityId())
                && sameUser(last, entry)) {
                // Iteration is newest-first, so `last` is the newer UPDATE and the current `entry` is
                // older. Keep the newer one (already in `kept`), drop the older one.
                return;
            }
        }
        kept.add(entry);
    }

    private static boolean sameUser(final AuditLogDto a, final AuditLogDto b) {
        if (a.user() == null || b.user() == null) {
            return a.user() == b.user();
        }
        return Objects.equals(a.user().id(), b.user().id());
    }

    private List<UpdateEntryDto> resolveNames(final List<AuditLogDto> entries) {
        final Set<UUID> companyIds = new HashSet<>();
        final Set<UUID> contactIds = new HashSet<>();
        for (final AuditLogDto e : entries) {
            if (e.entityId() == null) {
                continue;
            }
            switch (e.entityType()) {
                case COMPANY_ENTITY_TYPE -> {
                    if (e.action() != AuditAction.DELETE) {
                        companyIds.add(e.entityId());
                    }
                }
                case CONTACT_ENTITY_TYPE -> {
                    if (e.action() != AuditAction.DELETE) {
                        contactIds.add(e.entityId());
                    }
                }
                default -> {
                    if (CompanyService.COMMENT_ENTITY_TYPE.equals(e.entityType())) {
                        companyIds.add(e.entityId());
                    } else if (ContactService.COMMENT_ENTITY_TYPE.equals(e.entityType())) {
                        contactIds.add(e.entityId());
                    }
                }
            }
        }

        final Map<UUID, String> companyNames = new HashMap<>();
        for (final CompanyEntity c : companyRepository.findAllById(companyIds)) {
            companyNames.put(c.getId(), c.getName());
        }
        final Map<UUID, String> contactNames = new HashMap<>();
        for (final ContactEntity c : contactRepository.findAllById(contactIds)) {
            contactNames.put(c.getId(), buildContactDisplayName(c));
        }

        final List<UpdateEntryDto> result = new ArrayList<>(entries.size());
        for (final AuditLogDto e : entries) {
            final UpdateType type = toUpdateType(e);
            final UUID entityId;
            final String entityName;
            if (type == UpdateType.COMPANY_DELETED || type == UpdateType.CONTACT_DELETED) {
                entityId = null;
                entityName = null;
            } else if (type == UpdateType.COMPANY_CREATED
                || type == UpdateType.COMPANY_UPDATED
                || type == UpdateType.COMPANY_COMMENT_CREATED
                || type == UpdateType.COMPANY_COMMENT_UPDATED
                || type == UpdateType.COMPANY_COMMENT_DELETED) {
                entityId = e.entityId();
                entityName = companyNames.get(e.entityId());
            } else {
                entityId = e.entityId();
                entityName = contactNames.get(e.entityId());
            }
            result.add(new UpdateEntryDto(e.id(), type, entityId, entityName, e.user(), e.createdAt()));
        }
        return result;
    }

    private static String buildContactDisplayName(final ContactEntity c) {
        final StringBuilder b = new StringBuilder();
        if (c.getTitle() != null && !c.getTitle().isBlank()) {
            b.append(c.getTitle()).append(' ');
        }
        if (c.getFirstName() != null && !c.getFirstName().isBlank()) {
            b.append(c.getFirstName()).append(' ');
        }
        if (c.getLastName() != null && !c.getLastName().isBlank()) {
            b.append(c.getLastName());
        }
        final String name = b.toString().trim();
        return name.isEmpty() ? null : name;
    }

    private static UpdateType toUpdateType(final AuditLogDto entry) {
        final String entityType = entry.entityType();
        final AuditAction action = entry.action();
        return switch (entityType) {
            case COMPANY_ENTITY_TYPE -> switch (action) {
                case INSERT -> UpdateType.COMPANY_CREATED;
                case UPDATE -> UpdateType.COMPANY_UPDATED;
                case DELETE -> UpdateType.COMPANY_DELETED;
            };
            case CONTACT_ENTITY_TYPE -> switch (action) {
                case INSERT -> UpdateType.CONTACT_CREATED;
                case UPDATE -> UpdateType.CONTACT_UPDATED;
                case DELETE -> UpdateType.CONTACT_DELETED;
            };
            default -> {
                if (CompanyService.COMMENT_ENTITY_TYPE.equals(entityType)) {
                    yield switch (action) {
                        case INSERT -> UpdateType.COMPANY_COMMENT_CREATED;
                        case UPDATE -> UpdateType.COMPANY_COMMENT_UPDATED;
                        case DELETE -> UpdateType.COMPANY_COMMENT_DELETED;
                    };
                }
                if (ContactService.COMMENT_ENTITY_TYPE.equals(entityType)) {
                    yield switch (action) {
                        case INSERT -> UpdateType.CONTACT_COMMENT_CREATED;
                        case UPDATE -> UpdateType.CONTACT_COMMENT_UPDATED;
                        case DELETE -> UpdateType.CONTACT_COMMENT_DELETED;
                    };
                }
                throw new IllegalStateException("Unexpected entityType: " + entityType);
            }
        };
    }

}
