# Design: User Entity

## GitHub Issue

—

## Summary

The CRM currently has no persistent user representation. User info is extracted from the JWT token on every request and returned as a transient `UserInfo` record. This spec introduces a `UserEntity` that is automatically created on a user's first authenticated request and updated on subsequent requests. The entity stores the OIDC `sub` claim as a stable identifier, the user's name and email (synced from the JWT), and an avatar image. REST endpoints allow retrieving and managing the current user's avatar. The sidebar is updated to display the local avatar instead of the OIDC `picture` claim.

## Goals

- Persist users in the local database on first authenticated request
- Sync name and email from the JWT on every request
- Support avatar upload/retrieval (same pattern as Contact photos)
- Provide REST endpoints for current user info and avatar management
- Display local avatar in the sidebar

## Non-goals

- User admin/management UI (no user list, edit, or delete views)
- Role-based access control or permissions
- Comment author FK migration (next spec, tracked in TODO.md)
- Task assignment to users (future spec)
- User profile page (future spec)

## Technical Approach

### Data Model

**UserEntity fields:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | UUID | PK, auto-generated | Internal identifier |
| `sub` | String | VARCHAR(255), NOT NULL, UNIQUE | OIDC subject identifier |
| `name` | String | VARCHAR(255), NOT NULL | Display name (synced from JWT) |
| `email` | String | VARCHAR(255) | Email address (synced from JWT) |
| `avatar` | byte[] | LAZY, nullable | Avatar image data |
| `avatarContentType` | String | VARCHAR(100), nullable | MIME type of the avatar |
| `createdAt` | Instant | TIMESTAMPTZ, NOT NULL | Auto-set on creation |
| `updatedAt` | Instant | TIMESTAMPTZ, NOT NULL | Auto-updated on modification |

**Rationale for UUID PK + sub as unique constraint:** Using a UUID PK keeps the primary key format consistent with all other entities (Company, Contact, Tag, Task). The `sub` claim is the stable OIDC identifier used for lookup but not as PK.

### Database Migration (V16)

```sql
CREATE TABLE users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sub                 VARCHAR(255)    NOT NULL UNIQUE,
    name                VARCHAR(255)    NOT NULL,
    email               VARCHAR(255),
    avatar              BYTEA,
    avatar_content_type VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_sub ON users(sub);
```

### UserService Refactoring

The existing `UserService.getCurrentUser()` changes from extracting a transient `UserInfo` to a find-or-create pattern returning `UserEntity`:

1. Extract `sub`, `name`, `email` from the JWT token
2. Look up `UserEntity` by `sub`
3. If not found: create a new `UserEntity` with `sub`, `name`, `email`
4. If found: update `name` and `email` if they differ (handles name changes in Authentik)
5. Return the `UserEntity`

**Rationale for find-or-create on every request:** This is simpler and more robust than a separate login endpoint. The first request of a new user automatically creates the entry. No coordination between frontend and backend needed. The DB lookup per request (by unique indexed `sub`) is negligible for an internal CRM.

**`UserInfo` record is removed** — all callers (`CommentService` etc.) are updated to use `UserEntity`.

### UserRepository

```java
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findBySub(String sub);
}
```

### API Design

**Base path:** `/api/users`

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| `GET` | `/api/users/me` | Get current user info | 200 |
| `GET` | `/api/users/me/avatar` | Get current user's avatar image | 200 / 404 |
| `PUT` | `/api/users/me/avatar` | Upload/replace avatar | 200 |
| `DELETE` | `/api/users/me/avatar` | Remove avatar | 204 |

**GET /api/users/me response (UserDto):**
- `id` (UUID)
- `name` (String)
- `email` (String)
- `hasAvatar` (boolean)
- `createdAt` (Instant)
- `updatedAt` (Instant)

**GET /api/users/me/avatar:**
- Returns the avatar image with the correct `Content-Type` header
- Returns 404 if no avatar is set

**PUT /api/users/me/avatar:**
- Accepts `multipart/form-data` with the image file
- Stores the image bytes and content type
- Returns the updated `UserDto`

**DELETE /api/users/me/avatar:**
- Removes avatar and avatarContentType (sets to null)
- Returns 204 No Content

**Rationale for `/me` pattern instead of `/{id}`:** Users can only manage their own profile in this spec. A `/me` endpoint avoids authorization complexity and clearly signals the intent.

### Frontend Changes

**Sidebar avatar:**
- Replace the OIDC `picture` claim with the local avatar from `/api/users/me/avatar`
- Fetch current user info via `GET /api/users/me` on sidebar load
- If `hasAvatar` is true, display the avatar image; otherwise, show the existing `CircleUser` icon

**API functions (api.ts):**
- `getCurrentUser(): Promise<UserDto>`
- `getUserAvatar(): string` — returns the URL `/api/users/me/avatar` (used as img src)
- `uploadUserAvatar(file: File): Promise<UserDto>`
- `deleteUserAvatar(): Promise<void>`

**Avatar upload UI:**
- Add avatar upload to the sidebar user section (click on avatar/icon to upload)
- Same file input pattern as Company logo / Contact photo upload
- Accept common image formats (JPEG, PNG)

### TypeScript Types (types.ts)

```typescript
interface UserDto {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly hasAvatar: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
```

### i18n

| Key | EN | DE |
|-----|----|----|
| `sidebar.uploadAvatar` | "Change avatar" | "Avatar ändern" |
| `sidebar.removeAvatar` | "Remove avatar" | "Avatar entfernen" |

### Impact on Existing Code

| File | Change |
|------|--------|
| `UserInfo.java` | **Removed** — replaced by `UserEntity` |
| `UserService.java` | Refactored: find-or-create by `sub`, returns `UserEntity` |
| `CommentService.java` | `getCurrentUser().name()` → `getCurrentUser().getName()` |

## Key Files

| File | Change |
|------|--------|
| `backend/.../user/UserEntity.java` | New: JPA entity |
| `backend/.../user/UserDto.java` | New: response DTO |
| `backend/.../user/UserRepository.java` | New: JPA repository |
| `backend/.../user/UserController.java` | New: REST controller for /api/users/me |
| `backend/.../user/UserService.java` | Refactored: find-or-create, returns UserEntity |
| `backend/.../user/UserInfo.java` | Removed |
| `backend/.../comment/CommentService.java` | Updated: use UserEntity |
| `backend/.../db/migration/V16__create_users.sql` | New: migration |
| `frontend/src/components/sidebar.tsx` | Updated: fetch and display local avatar |
| `frontend/src/lib/api.ts` | Add user API functions |
| `frontend/src/lib/types.ts` | Add UserDto type |
| `frontend/src/lib/i18n/en.ts` | Add avatar translations |
| `frontend/src/lib/i18n/de.ts` | Add avatar translations |

## Security Considerations

- Users can only access/modify their own data via `/me` endpoints — no access to other users
- Avatar upload should validate file size (reasonable limit, e.g., 2MB) and content type
- The `sub` claim is the trusted OIDC identifier — it cannot be spoofed by the user

## GDPR Considerations

- The User entity stores personal data (name, email, avatar) derived from the OIDC provider
- **Legal basis:** Legitimate interest — the data is necessary for CRM operation (displaying who created comments, task assignment)
- **Data minimization:** Only name, email, and an optional avatar are stored
- **Right to erasure:** When a user needs to be removed, the User entity and all references (comments, future task assignments) must be addressed. The comment author FK migration (next spec) will enable cascading or anonymization
- **Retention:** User data persists as long as the user account exists in Authentik. No automatic cleanup — manual process for now
