# Design: Tag Task Count

## GitHub Issue

—

## Summary

The tag list table shows company and contact counts with navigation links to filtered list views. Since tasks now support tags (Spec 062), a third count column for tasks is added following the same pattern. The task count only includes active tasks (status OPEN or IN_PROGRESS) — completed tasks (DONE) are excluded from the count.

## Goals

- Show task count per tag in the tag list table
- Link the count to the task list filtered by that tag
- Only count active tasks (OPEN, IN_PROGRESS)

## Non-goals

- Filtering the linked task list by status (link goes to `/tasks?tagIds=x` without status pre-filter)
- Changing the existing company/contact count behavior

## Technical Approach

### Backend

**TagRepository** — add a count query for active tasks:

```java
@Query("SELECT COUNT(t) FROM TaskEntity t JOIN t.tags tag WHERE tag.id = :tagId AND t.status <> 'DONE'")
long countActiveTasksByTagId(@Param("tagId") UUID tagId);
```

**Rationale for excluding DONE:** The count represents actionable items — showing completed tasks would inflate the number and reduce its usefulness as a quick indicator.

**TagDto** — add `taskCount` field:

```java
public record TagDto(UUID id, String name, String description, String color,
                     Instant createdAt, Instant updatedAt,
                     Long companyCount, Long contactCount, Long taskCount)
```

Update both factory methods — one with counts, one without (sets `taskCount` to `null`).

**TagService** — compute task count alongside existing counts:

In the `findAll()` method, when `includeCounts` is `true`, also call `tagRepository.countActiveTasksByTagId(entity.getId())` and pass the result to the DTO factory method.

### Frontend

**tag-list.tsx** — add a third count column after the contact count column:

- Column header: i18n key for "Tasks" / "Aufgaben"
- Cell: shows `tag.taskCount ?? 0`
- When count > 0: render a link to `/tasks?tagIds=${tag.id}` with `ArrowUpRight` icon (same pattern as company/contact counts)

### i18n

| Key | EN | DE |
|-----|----|----|
| `tags.columns.tasks` | "Tasks" | "Aufgaben" |

## Key Files

| File | Change |
|------|--------|
| `backend/.../tag/TagRepository.java` | Add `countActiveTasksByTagId` query |
| `backend/.../tag/TagDto.java` | Add `taskCount` field, update factory methods |
| `backend/.../tag/TagService.java` | Compute task count when `includeCounts` is true |
| `frontend/src/components/tag-list.tsx` | Add task count column with navigation link |
| `frontend/src/lib/i18n/en.ts` | Add `tags.columns.tasks` |
| `frontend/src/lib/i18n/de.ts` | Add `tags.columns.tasks` |
