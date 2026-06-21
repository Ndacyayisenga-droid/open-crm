# Implementation Steps: Webhook Support (Backend)

## Step 1: Database migration and entity

- [x] Create `V20__add_webhooks.sql` with the `webhooks` table (id UUID PK, url VARCHAR(2048) NOT NULL, active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)
- [x] Create `WebhookEntity.java` in `com.openelements.crm.webhook` package with JPA annotations, UUID primary key, `@CreationTimestamp`, `@UpdateTimestamp`, getters/setters, equals/hashCode on id
- [x] Create `WebhookRepository.java` extending `JpaRepository<WebhookEntity, UUID>` with `findAllByActiveTrue()` method

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Application starts without Flyway migration errors
- [x] `webhooks` table exists with correct columns and defaults

**Related behaviors:** (foundation for all CRUD and firing behaviors)

---

## Step 2: DTOs and webhook service (CRUD)

- [x] Create `WebhookDto.java` record with fields: id, url, active, createdAt, updatedAt and `fromEntity()` factory method
- [x] Create `WebhookCreateDto.java` record with `@NotBlank` url field
- [x] Create `WebhookUpdateDto.java` record with `@NotBlank` url and `@NotNull` active fields
- [x] Create `WebhookService.java` with CRUD methods: `create()`, `getById()`, `update()`, `delete()`, `list(Pageable)`
- [x] Service follows existing patterns: `@Service`, `@Transactional`, `Objects.requireNonNull()`, `ResponseStatusException` for 404

**Acceptance criteria:**
- [x] Project builds successfully
- [ ] Unit tests pass (added in Step 7)

**Related behaviors:** Create webhook with valid URL, Create webhook persists to database, List webhooks with pagination, List webhooks when none exist, Get webhook by ID, Get webhook with unknown ID, Update webhook URL, Deactivate webhook, Update webhook with unknown ID, Delete webhook, Delete webhook with unknown ID

---

## Step 3: Webhook REST controller

- [x] Create `WebhookController.java` at `/api/webhooks` with OpenAPI annotations
- [x] POST `/api/webhooks` — create (201)
- [x] GET `/api/webhooks` — list with pagination (200)
- [x] GET `/api/webhooks/{id}` — get by ID (200)
- [x] PUT `/api/webhooks/{id}` — update (200)
- [x] DELETE `/api/webhooks/{id}` — delete (204)
- [x] All endpoints require OIDC security (`@SecurityRequirement(name = "oidc")`)

**Acceptance criteria:**
- [x] Project builds successfully
- [ ] Endpoints visible in Swagger UI
- [ ] Manual smoke test via Swagger or curl works

**Related behaviors:** Create webhook with valid URL, Create webhook with missing URL, Create webhook with blank URL, List webhooks with pagination, Get webhook by ID, Get webhook with unknown ID, Update webhook URL, Deactivate webhook, Delete webhook

---

## Step 4: Event model

- [x] Create `WebhookEventType.java` enum with 18 values: COMPANY_CREATED, COMPANY_UPDATED, COMPANY_DELETED, CONTACT_CREATED, CONTACT_UPDATED, CONTACT_DELETED, COMMENT_CREATED, COMMENT_UPDATED, COMMENT_DELETED, TAG_CREATED, TAG_UPDATED, TAG_DELETED, TASK_CREATED, TASK_UPDATED, TASK_DELETED, USER_CREATED, USER_UPDATED, USER_DELETED
- [x] Create `WebhookEvent.java` record (internal Spring event): eventType (WebhookEventType), entityId (UUID), data (Object — the DTO or null)
- [x] Create `WebhookEventPayload.java` record (JSON sent to URLs): eventId (UUID), eventType (WebhookEventType), timestamp (Instant), entityId (UUID), data (Object)

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Enum contains exactly 18 values

**Related behaviors:** All 18 event types are defined

---

## Step 5: Async event listener and webhook firing

- [x] Add `@EnableAsync` to `CrmApplication.java`
- [x] Create `WebhookEventListener.java` with `@Component`
- [x] Add method annotated with `@TransactionalEventListener(phase = AFTER_COMMIT)` and `@Async` that receives `WebhookEvent`
- [x] In the listener: load all active webhooks via `WebhookRepository.findAllByActiveTrue()`
- [x] Build `WebhookEventPayload` with random UUID eventId, current timestamp, and data from the event
- [x] Fire HTTP POST to each webhook URL in parallel using `CompletableFuture` + `RestClient`
- [x] Configure `RestClient` with 10-second connect and read timeout
- [x] Catch exceptions per-webhook and log at WARN level (URL + error details)
- [x] Never rethrow exceptions

**Acceptance criteria:**
- [x] Project builds successfully
- [x] `@EnableAsync` is present on the main application class

**Related behaviors:** Inactive webhooks are not called, Multiple active webhooks receive the same event, Webhook calls are parallel, Webhook call timeout, Webhook call to unreachable URL, Failed webhook does not affect business operation, Failed webhook does not affect other webhooks, No webhooks registered, Event fires only after transaction commit

---

## Step 6: Integrate event publishing into all services

- [x] Inject `ApplicationEventPublisher` into `CompanyService` — publish events in `create()`, `update()`, `delete()`
- [x] Inject `ApplicationEventPublisher` into `ContactService` — publish events in `create()`, `update()`, `delete()`
- [x] Inject `ApplicationEventPublisher` into `CommentService` — publish events in `addToCompany()`, `addToContact()`, `addToTask()`, `update()`, `delete()`
- [x] Inject `ApplicationEventPublisher` into `TagService` — publish events in `create()`, `update()`, `delete()`
- [x] Inject `ApplicationEventPublisher` into `TaskService` — publish events in `create()`, `update()`, `delete()`
- [x] Inject `ApplicationEventPublisher` into `UserService` — publish event in `getCurrentUser()` when a new user is created
- [x] For create/update: publish event with the returned DTO as data
- [x] For delete: publish event with entityId and null data (capture ID before deletion)

**Acceptance criteria:**
- [x] Project builds successfully
- [x] All existing tests still pass (365 tests pass)
- [x] Event publishing calls are present in all 18 methods listed in the design

**Related behaviors:** Company created/updated/deleted fires webhook, Contact created/updated/deleted fires webhook, Comment created on company/contact/task fires webhook, Comment updated/deleted fires webhook, Tag created/updated/deleted fires webhook, Task created/updated/deleted fires webhook, User created fires webhook

---

## Step 7: Webhook service tests

- [x] Create `WebhookServiceTest.java` following existing patterns (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Nested`)
- [x] Test create: valid URL returns DTO with active=true, id, timestamps
- [x] Test create: persists to database (retrievable via getById)
- [x] Test getById: existing webhook returns DTO
- [x] Test getById: unknown ID throws 404
- [x] Test update: changes URL and active flag
- [x] Test update: unknown ID throws 404
- [x] Test delete: removes webhook
- [x] Test delete: unknown ID throws 404
- [x] Test list: pagination works correctly
- [x] Test list: empty result when no webhooks

**Acceptance criteria:**
- [x] All service tests pass
- [x] Project builds successfully

**Related behaviors:** Create webhook with valid URL, Create webhook persists to database, Get webhook by ID, Get webhook with unknown ID, Update webhook URL, Deactivate webhook, Update webhook with unknown ID, Delete webhook, Delete webhook with unknown ID, List webhooks with pagination, List webhooks when none exist

---

## Step 8: Webhook controller tests

- [x] Create `WebhookControllerTest.java` following existing patterns (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, MockMvc)
- [x] Test POST with valid URL returns 201 with correct JSON
- [x] Test POST with missing URL returns 400
- [x] Test POST with blank URL returns 400
- [x] Test GET list returns 200 with paginated results
- [x] Test GET by ID returns 200
- [x] Test GET unknown ID returns 404
- [x] Test PUT update returns 200 with updated fields
- [x] Test PUT unknown ID returns 404
- [x] Test DELETE returns 204
- [x] Test DELETE unknown ID returns 404
- [x] All requests include JWT via `testJwt()`

**Acceptance criteria:**
- [x] All controller tests pass
- [x] Project builds successfully

**Related behaviors:** Create webhook with valid URL, Create webhook with missing URL, Create webhook with blank URL, List webhooks with pagination, List webhooks when none exist, Get webhook by ID, Get webhook with unknown ID, Update webhook URL, Deactivate webhook, Update webhook with unknown ID, Delete webhook, Delete webhook with unknown ID

---

## Step 9: Webhook event listener tests

- [x] Create `WebhookEventListenerTest.java` with mocked `RestClient` and `WebhookRepository`
- [x] Test: active webhook receives HTTP POST with correct payload structure (eventId, eventType, timestamp, entityId, data)
- [x] Test: inactive webhooks are not called (findAllByActiveTrue returns only active)
- [x] Test: multiple webhooks all receive the event
- [x] Test: failed webhook call is logged, no exception propagates
- [x] Test: failed webhook does not prevent other webhooks from receiving the event
- [x] Test: no webhooks registered — no HTTP calls, no errors
- [x] Test: payload for create/update contains full DTO in data field
- [x] Test: payload for delete has null data
- [x] Test: each event has unique eventId
- [x] Test: payload contains ISO-8601 timestamp
- [x] Test: payload does not contain user information

**Acceptance criteria:**
- [x] All listener tests pass
- [x] Project builds successfully

**Related behaviors:** Inactive webhooks are not called, Multiple active webhooks receive the same event, Webhook call timeout, Webhook call to unreachable URL, Failed webhook does not affect business operation, Failed webhook does not affect other webhooks, No webhooks registered, Create/update payload contains full DTO, Delete payload has null data, Payload contains unique event ID, Payload contains timestamp, Payload does not contain user information

---

## Step 10: Event type enum test

- [x] Create `WebhookEventTypeTest.java` — verify enum contains exactly 18 values with the expected names

**Acceptance criteria:**
- [x] Test passes
- [x] Project builds successfully

**Related behaviors:** All 18 event types are defined

---

## Step 11: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — add webhook support feature description
- [x] Update `.claude/conventions/project-specific/project-tech.md` — add Spring async, RestClient, ApplicationEventPublisher
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add webhook package
- [x] Update `.claude/conventions/project-specific/project-architecture.md` — add webhook event flow
- [ ] Update `README.md` if webhook API usage or configuration details are relevant (skipped — backend-only, no user-facing changes)

**Acceptance criteria:**
- [x] Documentation reflects the new webhook feature
- [x] Project builds successfully
- [x] All tests pass (400 tests)

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create webhook with valid URL | Backend | Steps 7, 8 |
| Create webhook persists to database | Backend | Step 7 |
| Create webhook with missing URL | Backend | Step 8 |
| Create webhook with blank URL | Backend | Step 8 |
| List webhooks with pagination | Backend | Steps 7, 8 |
| List webhooks when none exist | Backend | Steps 7, 8 |
| Get webhook by ID | Backend | Steps 7, 8 |
| Get webhook with unknown ID | Backend | Steps 7, 8 |
| Update webhook URL | Backend | Steps 7, 8 |
| Deactivate webhook | Backend | Steps 7, 8 |
| Update webhook with unknown ID | Backend | Steps 7, 8 |
| Delete webhook | Backend | Steps 7, 8 |
| Delete webhook with unknown ID | Backend | Steps 7, 8 |
| All 18 event types are defined | Backend | Step 10 |
| Company created fires webhook | Backend | Step 9 |
| Company updated fires webhook | Backend | Step 9 |
| Company deleted fires webhook | Backend | Step 9 |
| Contact created fires webhook | Backend | Step 9 |
| Contact updated fires webhook | Backend | Step 9 |
| Contact deleted fires webhook | Backend | Step 9 |
| Comment created on company fires webhook | Backend | Step 9 |
| Comment created on contact fires webhook | Backend | Step 9 |
| Comment created on task fires webhook | Backend | Step 9 |
| Comment updated fires webhook | Backend | Step 9 |
| Comment deleted fires webhook | Backend | Step 9 |
| Tag created fires webhook | Backend | Step 9 |
| Tag updated fires webhook | Backend | Step 9 |
| Tag deleted fires webhook | Backend | Step 9 |
| Task created fires webhook | Backend | Step 9 |
| Task updated fires webhook | Backend | Step 9 |
| Task deleted fires webhook | Backend | Step 9 |
| User created fires webhook | Backend | Step 9 |
| Inactive webhooks are not called | Backend | Step 9 |
| Multiple active webhooks receive the same event | Backend | Step 9 |
| Webhook calls are parallel | Backend | Step 9 |
| Webhook call timeout | Backend | Step 9 |
| Webhook call to unreachable URL | Backend | Step 9 |
| Failed webhook does not affect business operation | Backend | Step 9 |
| Failed webhook does not affect other webhooks | Backend | Step 9 |
| No webhooks registered | Backend | Step 9 |
| Event fires only after transaction commit | Backend | Step 9 |
| Create/update payload contains full DTO | Backend | Step 9 |
| Delete payload has null data | Backend | Step 9 |
| Payload contains unique event ID | Backend | Step 9 |
| Payload contains timestamp | Backend | Step 9 |
| Payload does not contain user information | Backend | Step 9 |
