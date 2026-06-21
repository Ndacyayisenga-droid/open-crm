# Design: User Model Preparation

## GitHub Issue

—

## Summary

Prepare the application for future Authentik SSO integration by introducing a user concept without actual authentication. A hardcoded dummy user is used in both frontend and backend until Authentik is connected. The backend uses the dummy user name as comment author (replacing the current "UNKNOWN"). The frontend displays the user in the sidebar footer with a logout action (no-op for now).

No User entity or table is created — user information will come exclusively from the auth token (OIDC ID Token / Userinfo endpoint) when Authentik is integrated. No user-related API endpoint is created.

## Goals

- Establish a user concept that can be swapped for Authentik token data later
- Replace "UNKNOWN" comment author with a meaningful name
- Show the current user in the sidebar UI
- Add a logout action placeholder

## Non-goals

- Actual Authentik/OAuth2/OIDC integration
- User entity or table in the database
- User management API endpoint
- Role-based access control
- GDPR compliance for user data in comments (documented as TODO for Authentik integration)

## Technical Approach

### Backend

#### Dummy user service

Create a simple service/component that provides the current user's information. Initially returns hardcoded values:

```java
public record UserInfo(String name, String email) {}
```

Hardcoded dummy:
- Name: `"Demo User"`
- Email: `"demo@example.com"`

**Rationale:** Encapsulating the user info in a dedicated component makes it easy to swap the implementation later. When Authentik is integrated, this component extracts the user from the `SecurityContext` / JWT token instead of returning hardcoded values.

#### Comment author

Update `CommentService` to inject the user service and set `author` to the current user's name instead of `"UNKNOWN"`.

Existing comments with `"UNKNOWN"` remain unchanged — no data migration.

### Frontend

#### Dummy user

Define a hardcoded user object in a dedicated file (e.g., `lib/user.ts`):

```typescript
export const currentUser = {
  name: "Demo User",
  email: "demo@example.com",
};
```

When Authentik is integrated, this will be replaced by a context/hook that reads from the OIDC token. Both frontend and backend get user info independently from the token — no API call from frontend to backend for user data.

#### Sidebar user section

Add a user section at the very bottom of the sidebar (below the language switch):

- **Avatar:** Placeholder icon (e.g., `User` or `CircleUser` from lucide-react)
- **Name:** Display the current user's name
- **Logout button:** "Abmelden" / "Logout" (translated), currently a no-op

Styling follows the existing sidebar design:
- Background: `oe-dark`
- Text: `oe-white` / `oe-gray-light` for secondary text
- Separated from language switch by a border

The user section appears in both desktop and mobile sidebar layouts.

### Files Affected

**Backend (new):**
- `UserInfo.java` — record with name and email
- `UserService.java` (or similar) — provides current user info, hardcoded for now

**Backend (modified):**
- `CommentService.java` — inject UserService, use user name as comment author

**Frontend (new):**
- `frontend/src/lib/user.ts` — hardcoded current user object

**Frontend (modified):**
- `frontend/src/components/sidebar.tsx` — add user section at bottom
- `frontend/src/lib/i18n/de.ts` — add "Abmelden" translation
- `frontend/src/lib/i18n/en.ts` — add "Logout" translation

## Open Questions

None — all details resolved during design discussion. Known limitations of the string-based author field are documented in TODO.md.
