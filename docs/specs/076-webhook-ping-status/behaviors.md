# Behaviors: Webhook PING & Status Tracking

## PING Event

### PING triggers async call to webhook URL

- **Given** a webhook with URL `https://receiver.test/hook` exists
- **When** POST `/api/webhooks/{id}/ping`
- **Then** response is 202 Accepted immediately
- **And** an HTTP POST is sent to `https://receiver.test/hook` asynchronously

### PING payload has correct structure

- **Given** a webhook exists
- **When** POST `/api/webhooks/{id}/ping`
- **Then** the HTTP POST to the webhook URL contains a JSON body with `eventType: "PING"`, `entityId: null`, `data: null`, a non-null `eventId` (UUID), and a `timestamp`

### PING works for inactive webhooks

- **Given** a webhook with `active: false` exists
- **When** POST `/api/webhooks/{id}/ping`
- **Then** response is 202 Accepted
- **And** the PING call is sent to the webhook URL

### PING returns 404 for unknown webhook

- **Given** no webhook with ID `unknown-id` exists
- **When** POST `/api/webhooks/unknown-id/ping`
- **Then** response is 404 Not Found

### PING is a 19th event type

- **Given** the `WebhookEventType` enum exists
- **When** listing all enum values
- **Then** it contains exactly 19 values including `PING`

### PING is never fired by domain events

- **Given** an active webhook exists
- **When** a company is created via `CompanyService.create()`
- **Then** the webhook receives `COMPANY_CREATED`, not `PING`

## Status Tracking — Successful Calls

### Successful call stores HTTP status 200

- **Given** a webhook exists pointing to a URL that returns 200 OK
- **When** a domain event or PING triggers a webhook call
- **Then** the webhook's `lastStatus` is updated to `200`
- **And** the webhook's `lastCalledAt` is updated to approximately the current time

### Successful call stores HTTP status 201

- **Given** a webhook exists pointing to a URL that returns 201 Created
- **When** a webhook call is made
- **Then** the webhook's `lastStatus` is updated to `201`

### Status is visible in webhook DTO

- **Given** a webhook has been called and received a 200 response
- **When** GET `/api/webhooks/{id}`
- **Then** the response contains `lastStatus: 200` and a non-null `lastCalledAt`

### Never-called webhook has null status

- **Given** a newly created webhook that has never been called
- **When** GET `/api/webhooks/{id}`
- **Then** the response contains `lastStatus: null` and `lastCalledAt: null`

## Status Tracking — Error Cases

### HTTP 4xx error stores status code

- **Given** a webhook exists pointing to a URL that returns 400 Bad Request
- **When** a webhook call is made
- **Then** the webhook's `lastStatus` is updated to `400`

### HTTP 5xx error stores status code

- **Given** a webhook exists pointing to a URL that returns 500 Internal Server Error
- **When** a webhook call is made
- **Then** the webhook's `lastStatus` is updated to `500`

### Connection error stores status 0

- **Given** a webhook exists pointing to `https://does-not-exist.invalid/hook`
- **When** a webhook call is made
- **Then** the webhook's `lastStatus` is updated to `0`

### Timeout stores status -1

- **Given** a webhook exists pointing to a URL that does not respond within 10 seconds
- **When** a webhook call is made
- **Then** the webhook's `lastStatus` is updated to `-1`

### Error does not prevent status persistence

- **Given** a webhook exists pointing to a failing URL
- **When** a webhook call fails
- **Then** the error status is still persisted to the database
- **And** no exception propagates to the caller

## Status Tracking — Domain Events

### Domain event updates status on all active webhooks

- **Given** two active webhooks exist, one pointing to a URL returning 200, one returning 500
- **When** a company is created
- **Then** the first webhook's `lastStatus` is `200`
- **And** the second webhook's `lastStatus` is `500`

### Status updated on every call (overwriting previous)

- **Given** a webhook exists with `lastStatus: 200` from a previous call
- **When** a new webhook call fails with a timeout
- **Then** the webhook's `lastStatus` is updated to `-1`
- **And** `lastCalledAt` is updated to the current time

## Architecture — @Async Refactoring

### Webhook calls use Spring @Async instead of CompletableFuture

- **Given** the `WebhookSender` component exists
- **When** a domain event fires and triggers webhook calls
- **Then** each webhook call runs in a Spring-managed async thread (not `CompletableFuture.runAsync()`)

### Failed webhook does not affect other webhooks

- **Given** three active webhooks exist, the second points to a failing URL
- **When** a domain event occurs
- **Then** the first and third webhooks still receive the event and have their status updated

### Failed webhook does not affect business operation

- **Given** an active webhook exists pointing to a failing URL
- **When** a company is created via the REST API
- **Then** the company is successfully created and returned with 201
- **And** the webhook failure status is persisted but does not cause an error response

### No webhooks registered

- **Given** no webhooks exist in the database
- **When** a domain event occurs
- **Then** no HTTP calls are made and no errors are logged

### Webhook calls are parallel

- **Given** two active webhooks exist
- **When** a domain event occurs
- **Then** both webhooks are called concurrently via separate `@Async` method calls
