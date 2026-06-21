# Behaviors: Role-Based Access Control

The four user types referenced throughout this file:

- **User-None** — authenticated, has neither `ADMIN` nor `IT-ADMIN`.
- **User-Admin** — authenticated, has `ADMIN` only.
- **User-ItAdmin** — authenticated, has `IT-ADMIN` only.
- **User-Both** — authenticated, has both `ADMIN` and `IT-ADMIN`.

## Backend — Role mapping

### JWT with roles claim "ADMIN" grants ROLE_ADMIN authority

- **Given** a valid JWT with the claim `roles: ["ADMIN"]`
- **When** the request is authenticated
- **Then** the resulting `Authentication` has authority `ROLE_ADMIN`

### JWT with roles claim "IT-ADMIN" grants ROLE_IT-ADMIN authority

- **Given** a valid JWT with the claim `roles: ["IT-ADMIN"]`
- **When** the request is authenticated
- **Then** the resulting `Authentication` has authority `ROLE_IT-ADMIN`

### JWT with multiple roles grants multiple authorities

- **Given** a valid JWT with the claim `roles: ["ADMIN", "IT-ADMIN"]`
- **When** the request is authenticated
- **Then** the resulting `Authentication` has authorities `ROLE_ADMIN` and `ROLE_IT-ADMIN`

### JWT without a roles claim grants no role authorities

- **Given** a valid JWT without a `roles` claim
- **When** the request is authenticated
- **Then** no `ROLE_*` authorities are granted and the request is rejected with 403 on any role-guarded endpoint

### JWT with empty roles claim grants no role authorities

- **Given** a valid JWT with `roles: []`
- **When** the request is authenticated
- **Then** no `ROLE_*` authorities are granted

### Unknown role values are mapped through unchanged

- **Given** a valid JWT with `roles: ["SUPPORT"]`
- **When** the request is authenticated
- **Then** the resulting `Authentication` has authority `ROLE_SUPPORT` but this authority does not satisfy any `@PreAuthorize` check in the CRM

## Backend — Delete endpoints (ADMIN required)

Each scenario below applies identically to:
- `DELETE /api/companies/{id}`
- `DELETE /api/companies/{id}/logo`
- `DELETE /api/contacts/{id}`
- `DELETE /api/contacts/{id}/photo`
- `DELETE /api/tasks/{id}`
- `DELETE /api/tags/{id}`
- `DELETE /api/comments/{id}`

### User-None cannot delete

- **Given** a User-None and an existing entity
- **When** the user sends the DELETE request
- **Then** the response is 403 and the entity still exists

### User-Admin can delete

- **Given** a User-Admin and an existing entity
- **When** the user sends the DELETE request
- **Then** the response is 204 and the entity is removed

### User-ItAdmin cannot delete (CRM entities)

- **Given** a User-ItAdmin and an existing company/contact/task/tag/comment
- **When** the user sends the DELETE request
- **Then** the response is 403 and the entity still exists

### User-Both can delete

- **Given** a User-Both and an existing entity
- **When** the user sends the DELETE request
- **Then** the response is 204 and the entity is removed

### Delete with query parameter is still role-checked

- **Given** a User-None
- **When** the user sends `DELETE /api/companies/{id}?deleteContacts=true`
- **Then** the response is 403 and neither the company nor its contacts are removed

## Backend — Admin-area endpoints (IT-ADMIN required)

Each scenario below applies identically to every method of the admin controllers:
- `ApiKeyController`: `POST /api/api-keys`, `GET /api/api-keys`, `DELETE /api/api-keys/{id}`
- `WebhookController`: `GET /api/webhooks`, `GET /api/webhooks/{id}`, `POST /api/webhooks`, `PUT /api/webhooks/{id}`, `POST /api/webhooks/{id}/ping`, `DELETE /api/webhooks/{id}`
- `BrevoSyncController`: `GET /api/brevo/settings`, `PUT /api/brevo/settings`, `DELETE /api/brevo/settings`, `POST /api/brevo/sync`

### User-None cannot access admin endpoints

- **Given** a User-None
- **When** the user sends any admin-area request (GET, POST, PUT, DELETE)
- **Then** the response is 403

### User-Admin cannot access admin endpoints

- **Given** a User-Admin
- **When** the user sends any admin-area request
- **Then** the response is 403

### User-ItAdmin can access admin endpoints

- **Given** a User-ItAdmin
- **When** the user sends any admin-area request
- **Then** the request is processed normally (2xx on valid input, standard error codes otherwise)

### User-Both can access admin endpoints

- **Given** a User-Both
- **When** the user sends any admin-area request
- **Then** the request is processed normally

## Backend — Unaffected endpoints

### Read/create/update of CRM entities remains open to authenticated users

- **Given** a User-None
- **When** the user sends `GET`, `POST`, or `PUT` on `/api/companies`, `/api/contacts`, `/api/tasks`, `/api/tags`, or `/api/comments`
- **Then** the request is processed normally (role checks apply only to DELETE)

### Health endpoint remains public

- **Given** an unauthenticated request
- **When** it calls `GET /api/health`
- **Then** the response is 200 regardless of roles or authentication

### API key authentication continues to work without role checks

- **Given** a valid API key
- **When** a GET request is sent with the `X-API-Key` header
- **Then** the request is authorized on GET endpoints (non-delete) without consulting roles

## Frontend — Sidebar admin sub-menu

### User-None sees no admin items (desktop)

- **Given** a User-None viewing the desktop sidebar
- **When** the sidebar renders
- **Then** the admin parent group and all its sub-items are absent from the DOM

### User-Admin sees no admin items

- **Given** a User-Admin viewing the desktop sidebar
- **When** the sidebar renders
- **Then** the admin parent group and all its sub-items are absent from the DOM

### User-ItAdmin sees all admin items

- **Given** a User-ItAdmin viewing the desktop sidebar
- **When** the sidebar renders
- **Then** the admin parent group is rendered and its sub-items (Server Status, Bearer Token, Brevo, API Keys, Webhooks) are available

### User-Both sees all admin items

- **Given** a User-Both viewing the desktop sidebar
- **When** the sidebar renders
- **Then** the admin parent group and all sub-items are available

### Mobile sidebar respects the same rule

- **Given** any user and the mobile sidebar
- **When** the sidebar renders
- **Then** admin items appear in the flat list only if the user has `IT-ADMIN`

## Frontend — Admin route access

### Direct navigation without IT-ADMIN shows the 403 page

- **Given** a User-None navigating to `/admin/status`, `/admin/token`, `/admin/brevo`, `/api-keys`, or `/webhooks`
- **When** the route renders
- **Then** the shared `ForbiddenPage` is displayed with the "Access denied" heading, description, and "Back to home" link

### User-Admin also sees the 403 page on admin routes

- **Given** a User-Admin navigating to any admin route
- **When** the route renders
- **Then** the 403 page is displayed

### User-ItAdmin can open admin routes

- **Given** a User-ItAdmin navigating to any admin route
- **When** the route renders
- **Then** the regular admin page content is displayed

## Frontend — Delete buttons in CRM views

Each scenario applies to delete buttons in:
- `company-detail.tsx`, `company-list.tsx`
- `contact-detail.tsx`, `contact-list.tsx`
- `task-detail.tsx`
- `tag-list.tsx`
- `company-comments.tsx`, `contact-comments.tsx`, `task-comments.tsx`

### Without ADMIN the delete button is visible but disabled

- **Given** a User-None or User-ItAdmin viewing the detail/list view
- **When** the delete button renders
- **Then** the button is visible, disabled, and a tooltip with the text "ADMIN role required" / "ADMIN-Rolle erforderlich" appears on hover/focus

### With ADMIN the delete button is enabled

- **Given** a User-Admin or User-Both viewing the detail/list view
- **When** the delete button renders
- **Then** the button is visible, enabled, and no role-required tooltip is shown

### Clicking a disabled delete button does not open the dialog

- **Given** a User-None viewing the detail view
- **When** the user clicks the disabled delete button
- **Then** no confirmation dialog opens and no API call is made

## Frontend — Backend 403 error handling

### 403 on delete surfaces a permission-specific message

- **Given** a User-None whose UI somehow triggers a DELETE (e.g. stale button state after role revocation)
- **When** the backend returns 403
- **Then** the delete confirmation dialog displays the error "Delete not allowed — ADMIN role required." / "Löschen nicht erlaubt — ADMIN-Rolle erforderlich."
- **And** the generic error message is not shown

### 403 on company "delete with contacts" surfaces the same permission error

- **Given** a User-None triggering `DELETE /api/companies/{id}?deleteContacts=true`
- **When** the backend returns 403
- **Then** the `CompanyDeleteDialog` shows the permission-specific error

### Non-403 errors still show the generic error

- **Given** a User-Admin
- **When** a DELETE returns 500 or 404
- **Then** the dialog shows the existing generic error message, not the permission-specific one

## State transitions

### Role revocation during an active session

- **Given** a User-Admin who has just had their `ADMIN` role removed in Authentik
- **When** the user clicks a delete button (still enabled from stale session)
- **Then** the backend returns 403 and the dialog shows the permission-specific error
- **And** after the user signs out and signs in again, the delete buttons are disabled

### Role addition during an active session

- **Given** a User-None who has just been granted `ADMIN` in Authentik
- **When** the user refreshes the page without signing out
- **Then** delete buttons remain disabled (stale session)
- **And** after the user signs out and signs in again, delete buttons become enabled
