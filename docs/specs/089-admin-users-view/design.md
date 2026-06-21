# Design: Admin Users View

## GitHub Issue

—

## Summary

Add a read-only "Users" page to the admin area that lists all registered CRM users in a paginated table. Each row displays the user's avatar, name, and email address. The page is restricted to the IT-ADMIN role and serves as an informational overview — no edit, delete, or navigation actions are provided.

Users are auto-created on first OIDC login and currently cannot be deleted (deletion is out of scope for this spec). The User entity comes from the external `spring-services` library and already provides `findAll(Pageable)`.

## Goals

- Provide IT-ADMINs with a quick overview of all users registered in the system
- Follow established patterns for admin sub-pages and paginated tables
- Reuse existing `UserService` and `UserDto` from the `spring-services` library

## Non-goals

- User management (create, edit, delete, role assignment)
- Search or filter functionality
- Sorting
- Row click / detail navigation
- Displaying fields beyond avatar, name, and email (no roles, no timestamps)

## Technical approach

### Backend

Add a new paginated endpoint to the existing `UserController`:

**Endpoint:** `GET /api/users`

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Zero-based page index |
| `size` | int | 20 | Page size |

**Response:** `Page<UserDto>` (Spring Data page wrapper)

**Security:** `@RequiresItAdmin` annotation (maps to `@PreAuthorize("hasRole('IT-ADMIN')")`)

**Rationale:** The `UserService.findAll(Pageable)` method already exists in the `spring-services` library, so no new service logic is needed — the controller delegates directly.

**File:** `backend/src/main/java/com/openelements/crm/user/UserController.java`

### Frontend

#### API function

Add `getUsers(params: UserListParams): Promise<Page<UserDto>>` to `frontend/src/lib/api.ts`. Uses the same `apiFetch` pattern as other paginated endpoints.

#### Page component

Create `frontend/src/app/(app)/admin/users/page.tsx` as a server component:
1. Call `auth()` to get the session
2. Check for `ROLE_IT_ADMIN` — return `<ForbiddenPage />` if missing
3. Render `<UsersClient />`

This follows the exact pattern of `/admin/status/page.tsx` and `/admin/brevo/page.tsx`.

#### Client component

Create `frontend/src/app/(app)/admin/users/users-client.tsx`:

- Paginated table with columns: Avatar, Name, Email
- Page-size selector with localStorage persistence (`pageSize.users`, default 20, options: 10/20/50/100/200)
- Previous/Next pagination buttons (shown only when `totalPages > 1`)
- Total user count display
- Loading state: skeleton rows
- Empty state: icon + message

**Avatar column:** Render a small circular image (32x32px) from `avatarUrl`. If `avatarUrl` is `null`, show a fallback icon (e.g., `User` icon from lucide-react with a neutral background circle).

#### Sidebar integration

Add a nav item in the admin collapsible group in `frontend/src/app/(app)/layout.tsx`:
- Label: `t.nav.users` ("Users" / "Benutzer")
- Icon: `Users` from lucide-react
- Route: `/admin/users`
- Position: after "Webhooks" (last item in admin sub-menu)
- Indented, same pattern as other admin nav items

#### i18n

Add translation keys to both `frontend/src/lib/i18n/en.ts` and `frontend/src/lib/i18n/de.ts`:

| Key | English | German |
|-----|---------|--------|
| `nav.users` | Users | Benutzer |
| `users.title` | Users | Benutzer |
| `users.empty` | No users registered yet. | Noch keine Benutzer registriert. |
| `users.columns.avatar` | Avatar | Avatar |
| `users.columns.name` | Name | Name |
| `users.columns.email` | Email | E-Mail |
| `users.pagination.perPage` | per page | pro Seite |
| `users.pagination.previous` | Previous | Zurück |
| `users.pagination.next` | Next | Weiter |

## Dependencies

- `spring-services` library: `UserService.findAll(Pageable)`, `UserDto`
- `@open-elements/ui`: Button, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Skeleton
- `lucide-react`: `Users` icon (sidebar + empty state), `User` icon (avatar fallback)

## Security considerations

- Endpoint restricted to IT-ADMIN role via `@RequiresItAdmin`
- Frontend page gated by role check in server component
- Email addresses are personal data (GDPR) — access justified by IT-ADMIN role in internal company software where all users are company members
