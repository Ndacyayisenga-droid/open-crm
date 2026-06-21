# Design: Task Entity Backend

## GitHub Issue

—

## Summary

The CRM needs a way to track action items associated with companies and contacts. A new `Task` entity is introduced with a free-text action description, a due date (deadline), and a status (OPEN, IN_PROGRESS, DONE). Tasks follow the same XOR ownership pattern as Comments — each task belongs to exactly one company or one contact. Tasks also support tags via a many-to-many relationship, like Companies and Contacts.

This spec covers the backend only: entity, migration, DTOs, service, repository, CRUD REST endpoints, and tests. The frontend will be implemented in a separate spec.

## Goals

- Introduce a Task entity with action, due date, and status
- Associate tasks with exactly one company or contact (XOR)
- Support tags on tasks (many-to-many)
- Provide CRUD REST endpoints with pagination, status filter, and tag filter
- Sort tasks by due date (earliest first)

## Non-goals

- Frontend implementation (separate spec)
- Task assignment to users (future spec)
- Nested endpoints under companies/contacts (`GET /api/companies/{id}/tasks`) — future spec, but the data model supports it
- CSV export or print view for tasks
- Notifications or reminders for due dates

## Technical Approach

### Data Model

**TaskEntity** follows the established patterns from CommentEntity (XOR ownership) and CompanyEntity (tag support).

**Fields:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | UUID | PK, auto-generated | Task identifier |
| `action` | String | TEXT, NOT NULL | Free-text action description |
| `dueDate` | LocalDate | DATE, NOT NULL | Deadline for the task |
| `status` | TaskStatus | VARCHAR, NOT NULL, default OPEN | Current status |
| `company` | CompanyEntity | FK nullable, ManyToOne LAZY | Associated company (XOR with contact) |
| `contact` | ContactEntity | FK nullable, ManyToOne LAZY | Associated contact (XOR with company) |
| `tags` | Set\<TagEntity\> | ManyToMany via `task_tags` | Assigned tags |
| `createdAt` | Instant | TIMESTAMPTZ, NOT NULL | Auto-set on creation |
| `updatedAt` | Instant | TIMESTAMPTZ, NOT NULL | Auto-updated on modification |

**TaskStatus enum:** `OPEN`, `IN_PROGRESS`, `DONE`

**CHECK constraint:** Exactly one of `company_id` or `contact_id` must be non-null (same XOR pattern as comments).

**Cascade behavior:**
- `contact_id`: `ON DELETE CASCADE` — tasks are deleted when the contact is hard-deleted
- `company_id`: No cascade — tasks are preserved when a company is soft-deleted

**Rationale for following the Comment XOR pattern:** The existing CHECK constraint approach is proven and enforces data integrity at the database level. Using the same pattern keeps the codebase consistent.

### Database Migration (V14)

```sql
CREATE TABLE tasks (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    action      TEXT            NOT NULL,
    due_date    DATE            NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    company_id  UUID            REFERENCES companies(id),
    contact_id  UUID            REFERENCES contacts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_task_owner CHECK (
        (company_id IS NOT NULL AND contact_id IS NULL) OR
        (company_id IS NULL AND contact_id IS NOT NULL)
    )
);

CREATE INDEX idx_tasks_company_id ON tasks(company_id);
CREATE INDEX idx_tasks_contact_id ON tasks(contact_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_status ON tasks(status);

CREATE TABLE task_tags (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);
```

### DTOs

**TaskDto** (response):
- `id` (UUID)
- `action` (String)
- `dueDate` (LocalDate)
- `status` (TaskStatus)
- `companyId` (UUID, nullable)
- `companyName` (String, nullable) — denormalized for display
- `contactId` (UUID, nullable)
- `contactName` (String, nullable) — denormalized for display (firstName + lastName)
- `tagIds` (List\<UUID\>)
- `createdAt` (Instant)
- `updatedAt` (Instant)

**TaskCreateDto** (request):
- `action` (String, @NotBlank)
- `dueDate` (LocalDate, @NotNull)
- `status` (TaskStatus, nullable — defaults to OPEN if omitted)
- `companyId` (UUID, nullable)
- `contactId` (UUID, nullable)
- `tagIds` (List\<UUID\>, nullable)

Validation: Exactly one of `companyId` or `contactId` must be provided — validated in the service layer with a 400 response if violated.

**TaskUpdateDto** (request):
- `action` (String, @NotBlank)
- `dueDate` (LocalDate, @NotNull)
- `status` (TaskStatus, @NotNull)
- `tagIds` (List\<UUID\>, nullable — null = no change, empty = remove all)

**Rationale for not allowing owner change on update:** A task's association to a company or contact is set at creation and cannot be changed. This avoids complex reassignment logic and matches the Comment pattern.

### API Design

**Base path:** `/api/tasks`

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| `GET` | `/api/tasks` | List tasks (paginated, filtered) | 200 |
| `POST` | `/api/tasks` | Create a task | 201 |
| `GET` | `/api/tasks/{id}` | Get a single task | 200 / 404 |
| `PUT` | `/api/tasks/{id}` | Update a task | 200 / 404 |
| `DELETE` | `/api/tasks/{id}` | Delete a task | 204 / 404 |

**GET /api/tasks query parameters:**
- `page`, `size` — pagination (default: page 0, size 20)
- `status` — filter by TaskStatus (optional)
- `tagIds` — filter by tag IDs, comma-separated (optional)
- Sort: `dueDate` ascending (earliest first) by default

**Rationale for a standalone TaskController (not nested):** Tasks are a first-class entity with their own CRUD lifecycle. The global list endpoint is the primary access pattern. Nested endpoints under companies/contacts can be added later without breaking changes.

### Service Layer

**TaskService** follows the same patterns as CompanyService/ContactService:
- `create(TaskCreateDto)` — validates XOR, resolves company/contact and tags, saves entity
- `getById(UUID)` — returns task or 404
- `update(UUID, TaskUpdateDto)` — updates action, dueDate, status, tags (does not change owner)
- `delete(UUID)` — deletes task or 404
- `list(TaskStatus, List<UUID> tagIds, Pageable)` — paginated list with optional filters

Tag resolution uses `TagService.resolveTagIds()` (existing utility).

### Repository

**TaskRepository** extends `JpaRepository<TaskEntity, UUID>`:
- Uses Spring Data Specifications for dynamic filtering (status, tags)
- `findByCompanyId(UUID, Pageable)` — prepared for future nested endpoint
- `findByContactId(UUID, Pageable)` — prepared for future nested endpoint
- `countByCompanyId(UUID)` — for future task count display
- `countByContactId(UUID)` — for future task count display

## Security Considerations

- All endpoints require authentication (JWT via Spring Security, same as all existing endpoints)
- No authorization model — any authenticated user can CRUD any task (consistent with current CRM design)
- Task action is free text stored as TEXT — no HTML rendering, no injection risk

## GDPR Considerations

Tasks store action descriptions that may reference individuals. However, tasks are not personal data themselves — they are action items linked to business entities. The existing company/contact deletion mechanisms (soft-delete for companies, hard-delete with cascade for contacts) ensure tasks are cleaned up appropriately when the associated entity is removed.

## Key Files (to create)

| File | Purpose |
|------|---------|
| `backend/.../task/TaskEntity.java` | JPA entity |
| `backend/.../task/TaskStatus.java` | Enum: OPEN, IN_PROGRESS, DONE |
| `backend/.../task/TaskDto.java` | Response DTO |
| `backend/.../task/TaskCreateDto.java` | Create request DTO |
| `backend/.../task/TaskUpdateDto.java` | Update request DTO |
| `backend/.../task/TaskService.java` | Business logic |
| `backend/.../task/TaskRepository.java` | Data access |
| `backend/.../task/TaskController.java` | REST controller |
| `backend/.../db/migration/V14__create_tasks.sql` | Database migration |

## Key Files (existing, to reference)

| File | Pattern to follow |
|------|-------------------|
| `CommentEntity.java` | XOR FK pattern |
| `V3__create_comments.sql` | CHECK constraint, cascade |
| `CompanyEntity.java` | Tag ManyToMany mapping |
| `V12__create_tags.sql` | Join table pattern |
| `TagService.resolveTagIds()` | Tag resolution |
| `CompanyController.java` | CRUD endpoint patterns |
