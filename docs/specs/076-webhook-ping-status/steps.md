# Implementation Steps: Webhook PING & Status Tracking

## Step 1: Database migration and entity changes

- [x] Create `V21__add_webhook_status.sql` with `ALTER TABLE webhooks ADD COLUMN last_status INTEGER; ALTER TABLE webhooks ADD COLUMN last_called_at TIMESTAMP;`
- [x] Add `lastStatus` (Integer, nullable) and `lastCalledAt` (Instant, nullable) fields to `WebhookEntity.java` with getters/setters
- [x] Add `lastStatus` (Integer) and `lastCalledAt` (Instant) fields to `WebhookDto.java` and update `fromEntity()`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] New fields appear in webhook API responses as `null` for existing webhooks

**Related behaviors:** Never-called webhook has null status, Status is visible in webhook DTO

---

## Step 2: Add PING to WebhookEventType enum

- [x] Add `PING` as 19th value in `WebhookEventType.java`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Enum contains exactly 19 values

**Related behaviors:** PING is a 19th event type

---

## Step 3: Create WebhookSender with @Async + @Transactional

- [x] Create `WebhookSender.java` as `@Component` with `sendAndTrack(WebhookEntity, WebhookEventPayload)` method
- [x] Annotate method with `@Async` and `@Transactional`
- [x] Make HTTP POST via injected `RestClient` (`webhookRestClient` bean)
- [x] Capture HTTP status code on success
- [x] Catch `HttpClientErrorException`/`HttpServerErrorException` — extract status code
- [x] Catch `ResourceAccessException` — distinguish timeout (`-1`) from connection error (`0`)
- [x] Catch generic `Exception` — store `0`
- [x] Update `webhook.lastStatus` and `webhook.lastCalledAt`, then `webhookRepository.save(webhook)`
- [x] Log failures at WARN level

**Acceptance criteria:**
- [x] Project builds successfully

**Related behaviors:** Successful call stores HTTP status 200, Successful call stores HTTP status 201, HTTP 4xx error stores status code, HTTP 5xx error stores status code, Connection error stores status 0, Timeout stores status -1, Error does not prevent status persistence

---

## Step 4: Refactor WebhookEventListener to use WebhookSender

- [x] Inject `WebhookSender` into `WebhookEventListener`
- [x] Replace `CompletableFuture.runAsync(() -> sendWebhook(...))` loop with `webhookSender.sendAndTrack(webhook, payload)` calls
- [x] Remove `CompletableFuture.allOf(futures).join()` — no longer needed
- [x] Remove the private `sendWebhook()` method

**Acceptance criteria:**
- [x] Project builds successfully
- [x] All existing tests still pass

**Related behaviors:** Webhook calls use Spring @Async instead of CompletableFuture, Failed webhook does not affect other webhooks, Failed webhook does not affect business operation, No webhooks registered, Webhook calls are parallel

---

## Step 5: Add PING endpoint and service method

- [x] Add `ping(UUID id)` method to `WebhookService` — loads entity via `findOrThrow` (no active check), creates PING payload, calls `webhookSender.sendAndTrack(entity, payload)`
- [x] Add `POST /api/webhooks/{id}/ping` endpoint to `WebhookController` — returns `202 Accepted`, with OpenAPI annotations
- [x] Inject `WebhookSender` into `WebhookService`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] PING endpoint visible in Swagger UI

**Related behaviors:** PING triggers async call to webhook URL, PING payload has correct structure, PING works for inactive webhooks, PING returns 404 for unknown webhook, PING is never fired by domain events

---

## Step 6: WebhookSender unit tests

- [x] Create `WebhookSenderTest.java` with mocked `RestClient` and `WebhookRepository`
- [x] Test: successful 200 response stores status 200 and updates lastCalledAt
- [x] Test: successful 201 response stores status 201
- [x] Test: 400 response stores status 400
- [x] Test: 500 response stores status 500
- [x] Test: connection error stores status 0
- [x] Test: timeout stores status -1
- [x] Test: error does not throw exception
- [x] Test: status overwrites previous value
- [x] Test: PING payload structure (eventType PING, entityId null, data null)

**Acceptance criteria:**
- [x] All sender tests pass
- [x] Project builds successfully

**Related behaviors:** Successful call stores HTTP status 200, Successful call stores HTTP status 201, HTTP 4xx error stores status code, HTTP 5xx error stores status code, Connection error stores status 0, Timeout stores status -1, Error does not prevent status persistence, Status updated on every call (overwriting previous), PING payload has correct structure

---

## Step 7: WebhookController and WebhookService tests

- [x] Add PING tests to `WebhookControllerTest.java`: POST returns 202, unknown ID returns 404, inactive webhook returns 202
- [x] Add PING test to `WebhookServiceTest.java`: ping calls sender, unknown ID throws 404
- [x] Update existing tests to verify `lastStatus` and `lastCalledAt` fields in DTO (null for new webhooks)

**Acceptance criteria:**
- [x] All controller and service tests pass
- [x] Project builds successfully

**Related behaviors:** PING triggers async call to webhook URL, PING works for inactive webhooks, PING returns 404 for unknown webhook, Never-called webhook has null status, Status is visible in webhook DTO

---

## Step 8: Update WebhookEventType test

- [x] Update `WebhookEventTypeTest.java` — verify 19 values including PING

**Acceptance criteria:**
- [x] Test passes
- [x] Project builds successfully

**Related behaviors:** PING is a 19th event type

---

## Step 9: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — update webhook feature with PING and status tracking
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add WebhookSender.java, update migration count to V21

**Acceptance criteria:**
- [x] Documentation reflects the new PING and status tracking features
- [x] All tests pass

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| PING triggers async call to webhook URL | Backend | Steps 5, 7 |
| PING payload has correct structure | Backend | Steps 5, 6 |
| PING works for inactive webhooks | Backend | Steps 5, 7 |
| PING returns 404 for unknown webhook | Backend | Steps 5, 7 |
| PING is a 19th event type | Backend | Steps 2, 8 |
| PING is never fired by domain events | Backend | Step 6 |
| Successful call stores HTTP status 200 | Backend | Step 6 |
| Successful call stores HTTP status 201 | Backend | Step 6 |
| Status is visible in webhook DTO | Backend | Steps 1, 7 |
| Never-called webhook has null status | Backend | Steps 1, 7 |
| HTTP 4xx error stores status code | Backend | Step 6 |
| HTTP 5xx error stores status code | Backend | Step 6 |
| Connection error stores status 0 | Backend | Step 6 |
| Timeout stores status -1 | Backend | Step 6 |
| Error does not prevent status persistence | Backend | Step 6 |
| Domain event updates status on all active webhooks | Backend | Step 6 |
| Status updated on every call (overwriting previous) | Backend | Step 6 |
| Webhook calls use Spring @Async instead of CompletableFuture | Backend | Step 4 |
| Failed webhook does not affect other webhooks | Backend | Step 4 |
| Failed webhook does not affect business operation | Backend | Step 4 |
| No webhooks registered | Backend | Step 4 |
| Webhook calls are parallel | Backend | Step 4 |
