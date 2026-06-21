# Behaviors: Webhook Support (Backend)

## Webhook CRUD

### Create webhook with valid URL

- **Given** no webhooks exist
- **When** POST `/api/webhooks` with `{ "url": "https://example.com/hook" }`
- **Then** response is 201 with the webhook DTO containing the URL, `active: true`, and generated `id`, `createdAt`, `updatedAt`

### Create webhook persists to database

- **Given** no webhooks exist
- **When** POST `/api/webhooks` with `{ "url": "https://example.com/hook" }`
- **Then** GET `/api/webhooks` returns a list containing the created webhook

### Create webhook with missing URL

- **Given** no webhooks exist
- **When** POST `/api/webhooks` with `{}` (no url field)
- **Then** response is 400 Bad Request

### Create webhook with blank URL

- **Given** no webhooks exist
- **When** POST `/api/webhooks` with `{ "url": "" }`
- **Then** response is 400 Bad Request

### List webhooks with pagination

- **Given** 25 webhooks exist
- **When** GET `/api/webhooks?page=0&size=20`
- **Then** response is 200 with 20 webhooks and `totalElements: 25`

### List webhooks when none exist

- **Given** no webhooks exist
- **When** GET `/api/webhooks`
- **Then** response is 200 with empty content and `totalElements: 0`

### Get webhook by ID

- **Given** a webhook with ID `abc-123` exists
- **When** GET `/api/webhooks/abc-123`
- **Then** response is 200 with the webhook DTO

### Get webhook with unknown ID

- **Given** no webhook with ID `unknown-id` exists
- **When** GET `/api/webhooks/unknown-id`
- **Then** response is 404 Not Found

### Update webhook URL

- **Given** a webhook with URL `https://old.com/hook` exists
- **When** PUT `/api/webhooks/{id}` with `{ "url": "https://new.com/hook", "active": true }`
- **Then** response is 200 with updated URL `https://new.com/hook`

### Deactivate webhook

- **Given** an active webhook exists
- **When** PUT `/api/webhooks/{id}` with `{ "url": "https://example.com/hook", "active": false }`
- **Then** response is 200 with `active: false`

### Update webhook with unknown ID

- **Given** no webhook with ID `unknown-id` exists
- **When** PUT `/api/webhooks/unknown-id` with valid body
- **Then** response is 404 Not Found

### Delete webhook

- **Given** a webhook with ID `abc-123` exists
- **When** DELETE `/api/webhooks/abc-123`
- **Then** response is 204 No Content
- **And** GET `/api/webhooks/abc-123` returns 404

### Delete webhook with unknown ID

- **Given** no webhook with ID `unknown-id` exists
- **When** DELETE `/api/webhooks/unknown-id`
- **Then** response is 404 Not Found

## Event Type Model

### All 18 event types are defined

- **Given** the `WebhookEventType` enum exists
- **When** listing all enum values
- **Then** it contains exactly: `COMPANY_CREATED`, `COMPANY_UPDATED`, `COMPANY_DELETED`, `CONTACT_CREATED`, `CONTACT_UPDATED`, `CONTACT_DELETED`, `COMMENT_CREATED`, `COMMENT_UPDATED`, `COMMENT_DELETED`, `TAG_CREATED`, `TAG_UPDATED`, `TAG_DELETED`, `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`, `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`

## Webhook Event Firing — Company

### Company created fires webhook

- **Given** an active webhook with URL `https://receiver.test/hook` exists
- **When** a new company "Acme Corp" is created via `CompanyService.create()`
- **Then** an HTTP POST is sent to `https://receiver.test/hook` with a JSON body containing `eventType: "COMPANY_CREATED"`, a non-null `eventId`, a `timestamp`, the company's `entityId`, and `data` matching the `CompanyDto`

### Company updated fires webhook

- **Given** an active webhook exists
- **When** a company is updated via `CompanyService.update()`
- **Then** an HTTP POST is sent with `eventType: "COMPANY_UPDATED"` and `data` containing the updated `CompanyDto`

### Company deleted fires webhook

- **Given** an active webhook exists
- **When** a company is deleted via `CompanyService.delete()`
- **Then** an HTTP POST is sent with `eventType: "COMPANY_DELETED"`, the company's `entityId`, and `data: null`

## Webhook Event Firing — Contact

### Contact created fires webhook

- **Given** an active webhook exists
- **When** a new contact is created via `ContactService.create()`
- **Then** an HTTP POST is sent with `eventType: "CONTACT_CREATED"` and `data` matching the `ContactDto`

### Contact updated fires webhook

- **Given** an active webhook exists
- **When** a contact is updated via `ContactService.update()`
- **Then** an HTTP POST is sent with `eventType: "CONTACT_UPDATED"` and `data` containing the updated `ContactDto`

### Contact deleted fires webhook

- **Given** an active webhook exists
- **When** a contact is deleted via `ContactService.delete()`
- **Then** an HTTP POST is sent with `eventType: "CONTACT_DELETED"`, the contact's `entityId`, and `data: null`

## Webhook Event Firing — Comment

### Comment created on company fires webhook

- **Given** an active webhook exists
- **When** a comment is added to a company via `CommentService.addToCompany()`
- **Then** an HTTP POST is sent with `eventType: "COMMENT_CREATED"` and `data` matching the `CommentDto`

### Comment created on contact fires webhook

- **Given** an active webhook exists
- **When** a comment is added to a contact via `CommentService.addToContact()`
- **Then** an HTTP POST is sent with `eventType: "COMMENT_CREATED"`

### Comment created on task fires webhook

- **Given** an active webhook exists
- **When** a comment is added to a task via `CommentService.addToTask()`
- **Then** an HTTP POST is sent with `eventType: "COMMENT_CREATED"`

### Comment updated fires webhook

- **Given** an active webhook exists
- **When** a comment is updated via `CommentService.update()`
- **Then** an HTTP POST is sent with `eventType: "COMMENT_UPDATED"`

### Comment deleted fires webhook

- **Given** an active webhook exists
- **When** a comment is deleted via `CommentService.delete()`
- **Then** an HTTP POST is sent with `eventType: "COMMENT_DELETED"` and `data: null`

## Webhook Event Firing — Tag

### Tag created fires webhook

- **Given** an active webhook exists
- **When** a tag is created via `TagService.create()`
- **Then** an HTTP POST is sent with `eventType: "TAG_CREATED"`

### Tag updated fires webhook

- **Given** an active webhook exists
- **When** a tag is updated via `TagService.update()`
- **Then** an HTTP POST is sent with `eventType: "TAG_UPDATED"`

### Tag deleted fires webhook

- **Given** an active webhook exists
- **When** a tag is deleted via `TagService.delete()`
- **Then** an HTTP POST is sent with `eventType: "TAG_DELETED"` and `data: null`

## Webhook Event Firing — Task

### Task created fires webhook

- **Given** an active webhook exists
- **When** a task is created via `TaskService.create()`
- **Then** an HTTP POST is sent with `eventType: "TASK_CREATED"`

### Task updated fires webhook

- **Given** an active webhook exists
- **When** a task is updated via `TaskService.update()`
- **Then** an HTTP POST is sent with `eventType: "TASK_UPDATED"`

### Task deleted fires webhook

- **Given** an active webhook exists
- **When** a task is deleted via `TaskService.delete()`
- **Then** an HTTP POST is sent with `eventType: "TASK_DELETED"` and `data: null`

## Webhook Event Firing — User

### User created fires webhook

- **Given** an active webhook exists
- **When** a new user is auto-created on first OIDC login via `UserService.getCurrentUser()`
- **Then** an HTTP POST is sent with `eventType: "USER_CREATED"`

## Webhook Delivery Behavior

### Inactive webhooks are not called

- **Given** a webhook with `active: false` exists
- **When** any domain event occurs
- **Then** no HTTP POST is sent to that webhook's URL

### Multiple active webhooks receive the same event

- **Given** three active webhooks exist with URLs A, B, C
- **When** a company is created
- **Then** all three URLs receive an HTTP POST with identical `eventId`, `eventType`, `timestamp`, `entityId`, and `data`

### Webhook calls are parallel

- **Given** two active webhooks exist, one with a slow endpoint (responds after 5 seconds)
- **When** a domain event occurs
- **Then** both webhooks are called concurrently — the fast one does not wait for the slow one

### Webhook call timeout

- **Given** an active webhook exists pointing to an unresponsive URL
- **When** a domain event occurs
- **Then** the HTTP call times out after 10 seconds
- **And** a WARN log entry is written with the webhook URL and error details

### Webhook call to unreachable URL

- **Given** an active webhook exists pointing to `https://does-not-exist.invalid/hook`
- **When** a domain event occurs
- **Then** the HTTP call fails with a connection error
- **And** a WARN log entry is written
- **And** no exception propagates to the caller

### Failed webhook does not affect business operation

- **Given** an active webhook exists pointing to a failing URL
- **When** a company is created via the REST API
- **Then** the company is successfully created and returned with 201
- **And** the webhook failure is logged but does not cause an error response

### Failed webhook does not affect other webhooks

- **Given** three active webhooks exist, the second one points to a failing URL
- **When** a domain event occurs
- **Then** the first and third webhooks still receive the event successfully

### No webhooks registered

- **Given** no webhooks exist in the database
- **When** a domain event occurs
- **Then** no HTTP calls are made and no errors are logged

### Event fires only after transaction commit

- **Given** an active webhook exists
- **When** a service method throws an exception after `saveAndFlush()` but before returning
- **Then** the database change is rolled back
- **And** no webhook event is fired

## Webhook Event Payload Structure

### Create/update payload contains full DTO

- **Given** an active webhook exists
- **When** a company named "Acme Corp" is created
- **Then** the payload `data` field contains the full `CompanyDto` JSON including `id`, `name`, `email`, `website`, `tagIds`, `createdAt`, `updatedAt`, etc.

### Delete payload has null data

- **Given** an active webhook exists
- **When** a company with ID `abc-123` is deleted
- **Then** the payload contains `entityId: "abc-123"`, `eventType: "COMPANY_DELETED"`, and `data: null`

### Payload contains unique event ID

- **Given** an active webhook exists
- **When** two companies are created in sequence
- **Then** each webhook call has a different `eventId` (UUID)

### Payload contains timestamp

- **Given** an active webhook exists
- **When** a company is created
- **Then** the payload `timestamp` is an ISO-8601 instant close to the current time

### Payload does not contain user information

- **Given** an active webhook exists
- **When** any domain event occurs
- **Then** the payload does not contain any field identifying the acting user
