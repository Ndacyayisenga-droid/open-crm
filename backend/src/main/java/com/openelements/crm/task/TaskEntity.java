package com.openelements.crm.task;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.spring.base.data.AbstractEntity;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.tag.TagEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class TaskEntity extends AbstractEntity {

    @Column(columnDefinition = "TEXT", nullable = false)
    private String action;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private ContactEntity contact;

    @ManyToMany
    @JoinTable(
        name = "task_tags",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "task_comments",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "comment_id", unique = true)
    )
    private Set<CommentEntity> comments = new HashSet<>();


    public TaskEntity() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        Objects.requireNonNull(action, "action must not be null");
        this.action = action;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(final LocalDate dueDate) {
        Objects.requireNonNull(dueDate, "dueDate must not be null");
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(final TaskStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        this.status = status;
    }

    public CompanyEntity getCompany() {
        return company;
    }

    public void setCompany(final CompanyEntity company) {
        this.company = company;
    }

    public ContactEntity getContact() {
        return contact;
    }

    public void setContact(final ContactEntity contact) {
        this.contact = contact;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(final Set<TagEntity> tags) {
        this.tags = tags;
    }

    public Set<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(final Set<CommentEntity> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "TaskEntity[id=" + id() + ", action=" + action + ", status=" + status + "]";
    }
}
