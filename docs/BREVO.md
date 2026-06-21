# Brevo Integration Reference

Technical reference for integrating with Brevo (formerly Sendinblue). This document covers the REST API, webhooks, SDKs, and product features relevant to the Open CRM project.

**Homepage:** https://www.brevo.com
**API Docs:** https://developers.brevo.com/docs/getting-started
**API Reference:** https://developers.brevo.com/reference
**OpenAPI Spec:** https://api.brevo.com/v3/swagger_definition_v3.yml (OpenAPI 3.1)

---

## Authentication

All API requests require an `api-key` header:

```
api-key: YOUR_API_KEY
```

API keys are created in the Brevo dashboard. There is also a `partner-key` header for reseller integrations.

---

## Base URL

```
https://api.brevo.com/v3
```

All endpoints below are relative to this base URL.

---

## API Categories

| Category | Description |
|----------|-------------|
| **Contacts** | CRUD, import/export, attributes, lists, folders, segments |
| **Transactional Email (SMTP)** | Send emails, templates, statistics, blocked contacts |
| **Transactional SMS** | Send SMS, statistics, events |
| **WhatsApp** | Send messages, event reporting |
| **Email Campaigns** | CRUD, scheduling, A/B testing, statistics |
| **SMS Campaigns** | CRUD, scheduling, reporting |
| **CRM** | Companies, Deals, Tasks, Notes, Files, Pipelines |
| **eCommerce** | Orders, Products, Categories, Coupons |
| **Conversations** | Live chat, automated messaging |
| **Custom Events** | User event/behavior tracking |
| **Account Management** | Senders, Domains, Webhooks, Users |
| **Loyalty** | Programs, Subscriptions, Tiers, Rewards |

---

## Contact Management API

### Create Contact

**`POST /contacts`**

```json
{
  "email": "john.doe@example.com",
  "ext_id": "custom-id-123",
  "attributes": {
    "FNAME": "John",
    "LNAME": "Doe",
    "SMS": "+4917612345678",
    "COUNTRIES": ["Germany", "France"]
  },
  "listIds": [11, 25],
  "emailBlacklisted": false,
  "smsBlacklisted": false,
  "updateEnabled": false
}
```

- At least one of `email`, `ext_id`, or `SMS` attribute is required
- Attribute names **must be UPPERCASE**
- `updateEnabled: true` enables upsert behavior (update if exists, create if not)

**Response 201:**

```json
{ "id": 123456789 }
```

### Get Contact

**`GET /contacts/{identifier}?identifierType=email_id`**

The `identifier` can be: email (URL-encoded), contact ID, `ext_id`, SMS, or WhatsApp value.
Disambiguate with `identifierType`: `email_id`, `contact_id`, `ext_id`, `phone_id`, `whatsapp_id`, `landline_number_id`.

**Response 200:**

```json
{
  "id": 123,
  "email": "john.doe@example.com",
  "attributes": { "FNAME": "John", "LNAME": "Doe" },
  "emailBlacklisted": false,
  "smsBlacklisted": false,
  "createdAt": "2024-01-15T10:30:00.000Z",
  "modifiedAt": "2024-06-20T14:22:00.000Z",
  "listIds": [11],
  "listUnsubscribed": [],
  "statistics": {
    "messagesSent": [{ "campaignId": 1, "eventTime": "..." }],
    "delivered": [],
    "opened": [{ "campaignId": 1, "count": 3, "eventTime": "...", "ip": "..." }],
    "clicked": [{ "campaignId": 1, "links": [{ "url": "...", "count": 1 }] }],
    "softBounces": [],
    "hardBounces": [],
    "complaints": [],
    "unsubscriptions": { "userUnsubscription": [], "adminUnsubscription": [] }
  }
}
```

Statistics cover the last 90 days by default; use `startDate`/`endDate` query params to change the window.

### List Contacts

**`GET /contacts`**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | int64 | 50 | Results per page |
| `offset` | int64 | 0 | Starting index |
| `sort` | enum | desc | `asc` or `desc` |
| `modifiedSince` | string | — | UTC datetime filter (`YYYY-MM-DDTHH:mm:ss.SSSZ`) |
| `createdSince` | string | — | UTC datetime filter |
| `listIds` | array[int64] | — | Filter by list IDs |
| `segmentId` | int64 | — | Filter by segment (mutually exclusive with `listIds`) |
| `filter` | string | — | Attribute-based equals filter |

**Response 200:**

```json
{
  "contacts": [
    {
      "id": 123,
      "email": "john@example.com",
      "attributes": { "FNAME": "John" },
      "emailBlacklisted": false,
      "smsBlacklisted": false,
      "createdAt": "2024-01-15T10:30:00.000Z",
      "modifiedAt": "2024-06-20T14:22:00.000Z",
      "listIds": [11],
      "listUnsubscribed": []
    }
  ],
  "count": 1
}
```

### Update Contact

**`PUT /contacts/{identifier}?identifierType=email_id`**

```json
{
  "attributes": {
    "FNAME": "Jane",
    "LNAME": "Smith"
  },
  "listIds": [25],
  "unlinkListIds": [11],
  "emailBlacklisted": false,
  "smsBlacklisted": false,
  "ext_id": "new-external-id"
}
```

**Response:** 204 No Content

### Delete Contact

**`DELETE /contacts/{identifier}?identifierType=email_id`**

**Response:** 204 No Content. Returns 405 if trying to delete a registered email contact.

### Bulk Import

**`POST /contacts/import`**

Supports three input sources (one required):
- `fileBody`: CSV content (max 10MB, recommended 8MB)
- `fileUrl`: Remote .txt/.csv/.json file URL
- `jsonBody`: Array of `{email, attributes}` objects (max 10MB)

```json
{
  "jsonBody": [
    { "email": "john@example.com", "attributes": { "FNAME": "John" } },
    { "email": "jane@example.com", "attributes": { "FNAME": "Jane" } }
  ],
  "listIds": [11],
  "updateExistingContacts": true,
  "emptyContactsAttributes": false
}
```

**Response 202:** `{ "processId": 78910 }` (asynchronous processing)

Attributes that do not exist in the Brevo account are silently ignored during import.

---

## Contact Attributes

### List Attributes

**`GET /contacts/attributes`**

**Response:**

```json
{
  "attributes": [
    {
      "name": "FIRSTNAME",
      "category": "normal",
      "type": "text"
    }
  ]
}
```

**Attribute types:** `text`, `date`, `float`, `id`, `boolean`, `multiple-choice`, `user`

**Attribute categories:** `normal`, `transactional`, `category`, `calculated`, `global`

**Standard attributes:** `EMAIL`, `FNAME`, `LNAME`, `SMS`, `BIRTHDATE`

### Create Attribute

**`POST /contacts/attributes/{attributeCategory}/{attributeName}`**

```json
{
  "type": "text"
}
```

**Response:** 201 with `{}`

All custom attributes must be pre-created in the Brevo account before use. Names must be UPPERCASE.

---

## Contact Lists

### List All Lists

**`GET /contacts/lists`**

| Parameter | Type | Default |
|-----------|------|---------|
| `limit` | int64 | 10 |
| `offset` | int64 | 0 |
| `sort` | enum | desc |

**Response:**

```json
{
  "count": 5,
  "lists": [
    {
      "id": 1,
      "name": "Newsletter",
      "totalBlacklisted": 0,
      "totalSubscribers": 150,
      "uniqueSubscribers": 148,
      "folderId": 1
    }
  ]
}
```

---

## Transactional Email API

**`POST /smtp/email`**

### Static HTML

```json
{
  "sender": { "name": "My App", "email": "noreply@myapp.com" },
  "to": [{ "email": "john@example.com", "name": "John Doe" }],
  "subject": "Order Confirmation #12345",
  "htmlContent": "<html><body><h1>Thank you!</h1></body></html>"
}
```

### Template with Parameters

```json
{
  "to": [{ "email": "john@example.com", "name": "John Doe" }],
  "templateId": 8,
  "params": {
    "name": "John",
    "trackingCode": "JD014600003",
    "estimatedArrival": "Tomorrow"
  }
}
```

Template variables use `{{params.variableName}}` syntax in the template.

### Optional Fields

| Field | Type | Description |
|-------|------|-------------|
| `cc` | array[{email, name?}] | Carbon copy |
| `bcc` | array[{email, name?}] | Blind carbon copy |
| `replyTo` | {email, name?} | Reply-to address |
| `attachment` | array | `{url}` or `{content (base64), name}` |
| `headers` | object | Custom headers (e.g., `{"Idempotency-Key": "abc-123"}`) |
| `tags` | array[string] | Categorization tags |
| `scheduledAt` | string (datetime) | UTC scheduling time |
| `messageVersions` | array | Per-recipient customization (max 2000 total) |

**Response 201:**

```json
{
  "messageId": "<unique-message-identifier>",
  "messageIds": ["<id1>", "<id2>"]
}
```

**Docs:** https://www.brevo.com/products/transactional-email/

---

## Webhooks

**Docs:** https://help.brevo.com/hc/en-us/articles/27824932835474-Create-outbound-webhooks-to-send-real-time-data-from-Brevo-to-an-external-app

**Limit:** Maximum 40 combined marketing + transactional webhooks per account.

### Webhook API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/webhooks` | Create webhook |
| `GET` | `/webhooks` | List webhooks (query: `type`, `sort`) |
| `GET` | `/webhooks/{webhookId}` | Get single webhook |
| `PUT` | `/webhooks/{webhookId}` | Update webhook |
| `DELETE` | `/webhooks/{webhookId}` | Delete webhook |

### Create Webhook

**`POST /webhooks`**

```json
{
  "url": "https://your-endpoint.com/webhook",
  "type": "transactional",
  "channel": "email",
  "events": ["delivered", "hardBounce", "softBounce", "click", "opened"],
  "description": "My webhook",
  "batched": false,
  "auth": {
    "type": "bearer",
    "token": "your-secret-token"
  },
  "headers": [
    { "key": "X-Custom-Header", "value": "custom-value" }
  ]
}
```

**Types:** `transactional` (default), `marketing`, `inbound`
**Channels:** `email` (default), `sms`

**Response 201:** `{ "id": 5 }`

### Event Types

**Transactional Email:**
`sent`, `request`, `delivered`, `opened`, `uniqueOpened`, `click`, `softBounce`, `hardBounce`, `invalid`, `deferred`, `blocked`, `spam`, `unsubscribed`, `error`, `proxy_open`, `unique_proxy_open`

**Marketing Email:**
`delivered`, `opened`, `click`, `hardBounce`, `softBounce`, `spam`, `unsubscribed`, `listAddition`, `contactUpdated`, `contactDeleted`, `proxy_open`

**Transactional SMS:**
`sent`, `accepted`, `delivered`, `softBounce`, `hardBounce`, `unsubscribe`, `reply`, `subscribe`, `skip`, `blacklisted`

**Marketing SMS:**
`sent`, `delivered`, `softBounce`, `hardBounce`, `unsubscribe`, `reply`, `subscribe`, `skip`

**Inbound:**
`inboundEmailProcessed`

### Payload Format (Email Events)

```json
{
  "event": "delivered",
  "email": "example@example.com",
  "id": 26224,
  "date": "2024-08-28 18:08:29",
  "ts": 1598634509,
  "ts_event": 1604933654,
  "ts_epoch": 1604933654,
  "message-id": "<xxxxxxxxxxxx@domain.com>",
  "subject": "Subject Line",
  "X-Mailin-custom": "some_custom_header",
  "sending_ip": "185.41.28.109",
  "template_id": 22,
  "contact_id": 8,
  "mirror_link": "https://app-smtp.brevo.com/log/preview/...",
  "tags": ["transac_messages"]
}
```

**Event-specific additional fields:**
- **Click/Open/Unsubscribe:** `link`, `user_agent`, `device_used`
- **Bounce:** `reason`
- **Marketing:** `camp_id`, `campaign name`, `date_sent`, `date_event`, `ts_sent`, `segment_ids`
- **Contact Updated:** `content` (array with updated fields)
- **Contact Added to List:** `list_id` (array)

### Payload Format (SMS Events)

```json
{
  "id": 26527,
  "status": "OK",
  "msg_status": "delivered",
  "description": "delivered",
  "to": "919102600071",
  "ts_event": 1728459617,
  "date": "2024-10-09 13:10:17",
  "messageId": 123,
  "reference": { "1": "1hi8srff049z0qzvgt3f" },
  "tag": ["tag1name"],
  "type": "transactional"
}
```

### Webhook Security

Four authentication methods (no built-in HMAC signature verification):

1. **IP Whitelisting** — Brevo sends from:
   - `1.179.112.0/20` (1.179.112.0 – 1.179.127.255)
   - `172.246.240.0/20` (172.246.240.0 – 172.246.255.255)

2. **HTTP Basic Auth** — Credentials in URL:
   `https://username:password@your-endpoint.com/webhook`

3. **Bearer Token** — Via `auth` object:
   ```json
   { "auth": { "type": "bearer", "token": "your-secret-token" } }
   ```

4. **Custom Headers** — Via `headers` array:
   ```json
   { "headers": [{ "key": "X-Auth", "value": "secret" }] }
   ```

### Retry Behavior

- **Total attempts:** 5 (1 initial + 4 retries)
- **Backoff schedule (exponential):**
  - 1st retry: 10 minutes after failure
  - 2nd retry: 1 hour after 1st retry
  - 3rd retry: 2 hours after 2nd retry
  - 4th retry: 8 hours after 3rd retry
- **4xx (except 429):** Permanently discards the event, no retries
- **429 / 5xx:** Triggers retries
- **After all retries exhausted:** Event is discarded

### Timestamps

- `ts_epoch` and `ts_event`: **UTC** (Unix seconds)
- `date`: **CET/CEST** timezone (string format)

---

## Rate Limits

| Endpoint | Hourly (RPH) | Per Second (RPS) |
|----------|-------------|-----------------|
| `POST /smtp/email`, `GET /smtp/blockedContacts` | 3,600,000 | 1,000 |
| `POST /transactionalSMS/send` | 540,000 | 150 |
| `POST /events` | 36,000 | 10 |
| `GET /contacts/*` | 36,000 | 10 |
| `POST /orders/status` | 18,000 | 5 |
| `POST /products` | 7,200 | 2 |
| `GET /loyalty/*` | 600 | — |
| All other endpoints | 100 | — |

Professional and Enterprise plans get significantly increased limits (up to 6,000 RPS for email, 60 RPS for contacts on Enterprise).

When rate-limited, the API returns **HTTP 429**. Rate limit headers: `x-sib-ratelimit-limit`, `x-sib-ratelimit-remaining`, `x-sib-ratelimit-reset`.

---

## Error Handling

**Standard error response:**

```json
{
  "code": "invalid_parameter",
  "message": "Human-readable error description"
}
```

**Common error codes:**

| Code | Description |
|------|-------------|
| `invalid_parameter` | Invalid parameter value |
| `missing_parameter` | Required parameter missing |
| `out_of_range` | Value outside acceptable range |
| `document_not_found` | Resource not found |
| `duplicate_parameter` | Duplicate value |
| `duplicate_request` | Request already processed (idempotency) |
| `unauthorized` | Authentication failure |
| `permission_denied` | Insufficient permissions |
| `not_enough_credits` | Insufficient account credits |

**HTTP status codes:** 200, 201, 202, 204, 400, 401, 402, 403, 404, 405, 406, 429

---

## Java SDK

**Maven dependency:**

```xml
<dependency>
    <groupId>com.brevo</groupId>
    <artifactId>brevo</artifactId>
    <version>1.1.1</version>
</dependency>
```

**Third-party alternative:** `software.xdev:brevo-java-client`

**Setup:**

```java
ApiClient defaultClient = Configuration.getDefaultApiClient();
ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
apiKey.setApiKey("YOUR_API_KEY");

ContactsApi contactsApi = new ContactsApi();
```

**Key API classes:** `AccountApi`, `ContactsApi`, `EmailCampaignsApi`, `CompaniesApi`, `DealsApi`, `EcommerceApi`, `EventsApi`, `DomainsApi`

**All official SDKs** (under `github.com/getbrevo`):

| Language | Package |
|----------|---------|
| Java | `com.brevo:brevo:1.1.1` |
| Node.js | v4 |
| Python | v4 |
| PHP | v4 |
| Ruby | — |
| Go | — |
| C# | — |

---

## Pricing Tiers (API-relevant)

| Plan | Emails | Key API Differences |
|------|--------|---------------------|
| **Free** | 300/day | Full API access |
| **Starter** | 5,000+/mo | Standard rate limits |
| **Standard** | Scalable | + Marketing automation |
| **Professional** | 150,000+/mo | + WhatsApp, push notifications |
| **Enterprise** | Custom (1M+ contacts) | + Custom objects, SSO, increased rate limits |

All plans include transactional email and contact management API access.

---

## Key Technical Notes

- All datetime values use UTC ISO 8601: `YYYY-MM-DDTHH:mm:ss.SSSZ`
- Contact attribute keys must be **UPPERCASE**
- Pagination uses `limit`/`offset` (not cursor-based)
- Content-Type: `application/json` for all POST/PUT requests
- The `updateEnabled: true` flag on `POST /contacts` enables upsert behavior
- Bulk import is asynchronous — returns a `processId`
- The `modifiedSince` filter on `GET /contacts` enables efficient incremental sync
