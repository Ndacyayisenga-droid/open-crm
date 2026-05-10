package com.openelements.crm.task;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.comment.CommentService;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@Service
@Transactional
public class TaskService extends AbstractDbBackedDataService<TaskEntity, TaskDto> {

    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public TaskService(final TaskRepository taskRepository,
                       final CompanyRepository companyRepository,
                       final ContactRepository contactRepository,
                       final CommentService commentService,
                       final CommentRepository commentRepository,
                       final TagRepository tagRepository,
                       final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> list(final TaskStatus status, final List<UUID> tagIds, final Pageable pageable) {
        final Specification<TaskEntity> spec = buildSpec(status, tagIds);
        return taskRepository.findAll(spec, pageable).map(e -> toData(e));
    }

    private Specification<TaskEntity> buildSpec(final TaskStatus status, final List<UUID> tagIds) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                final Join<TaskEntity, ?> tagJoin = root.join("tags");
                predicates.add(tagJoin.get("id").in(tagIds));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Lists comments attached to a task.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> listCommentsOfTask(final UUID taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        final TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return task.getComments().stream()
            .map(c -> commentService.findById(c.getId()).orElseThrow())
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .toList();
    }

    public CommentDto addCommentToTask(final UUID taskId, final CommentCreateDto request) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        final CommentDto saved = commentService.save(new CommentDto(null, request.text(), null, null, null));
        final CommentEntity entity = commentRepository.findByIdOrThrow(saved.id());
        task.getComments().add(entity);
        taskRepository.save(task);
        return saved;
    }

    public CommentDto updateCommentOfTask(final UUID taskId, final UUID commentId, final CommentCreateDto request) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        assertCommentBelongsToTask(taskId, commentId);
        final CommentDto current = commentService.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        return commentService.save(new CommentDto(commentId, request.text(), current.author(), current.createdAt(), current.updatedAt()));
    }

    public void deleteCommentOfTask(final UUID taskId, final UUID commentId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        assertCommentBelongsToTask(taskId, commentId);
        final TaskEntity task = taskRepository.findByIdOrThrow(taskId);
        task.getComments().removeIf(c -> c.getId().equals(commentId));
        taskRepository.saveAndFlush(task);
        commentService.delete(commentId);
    }

    private void assertCommentBelongsToTask(final UUID taskId, final UUID commentId) {
        final TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        final boolean belongs = task.getComments().stream()
            .anyMatch(c -> c.getId().equals(commentId));
        if (!belongs) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found for this task");
        }
    }

    @Override
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final TaskEntity task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        final List<UUID> commentIds = new ArrayList<>(
            task.getComments().stream().map(CommentEntity::getId).toList());
        task.getComments().clear();
        taskRepository.saveAndFlush(task);
        commentIds.forEach(commentService::delete);
        taskRepository.delete(task);
    }

    @Override
    protected TaskEntity createDetachedEntity() {
        return new TaskEntity();
    }

    @Override
    protected void updateEntity(TaskEntity entity, TaskDto data) {
        entity.setAction(data.action());
        entity.setDueDate(data.dueDate());
        entity.setStatus(data.status());

        final CompanyEntity company = Optional.ofNullable(data.companyId())
            .map(id -> companyRepository.findByIdOrThrow(id))
            .orElse(null);
        entity.setCompany(company);

        final ContactEntity contact = Optional.ofNullable(data.contactId())
            .map(id -> contactRepository.findByIdOrThrow(id))
            .orElse(null);
        entity.setContact(contact);

        final Set<TagEntity> tags = Optional.ofNullable(data.tagIds()).orElse(List.of()).stream()
            .map(id -> tagRepository.findByIdOrThrow(id))
            .collect(Collectors.toSet());
        entity.setTags(tags);
    }

    @Override
    protected TaskDto toData(TaskEntity entity) {
        return new TaskDto(
            entity.getId(),
            entity.getAction(),
            entity.getDueDate(),
            entity.getStatus(),
            entity.getCompany() != null ? entity.getCompany().getId() : null,
            entity.getCompany() != null ? entity.getCompany().getName() : null,
            entity.getContact() != null ? entity.getContact().getId() : null,
            entity.getContact() != null ? (entity.getContact().getFirstName() + " " + entity.getContact().getLastName()) : null,
            entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getComments().size(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @Override
    protected EntityRepository<TaskEntity> getRepository() {
        return taskRepository;
    }

    public long countWithTag(UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return taskRepository.findAll()
            .stream()
            .filter(contact -> contact.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }
}
