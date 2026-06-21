# Design: Frontend OIDC Auth

## GitHub Issue

—

## Summary

Add OIDC-based authentication to the Next.js frontend using Auth.js v5 (NextAuth). All pages require login — unauthenticated users are redirected to the OIDC provider (mock-oauth2-server locally, Authentik in production). The sidebar displays real user data (name, email, profile picture) from the OIDC token. Logout terminates both the frontend session and the provider session. API calls to the backend include the JWT token as an `Authorization: Bearer` header via a server-side proxy route.

## Goals

- Require authentication for all pages
- Display real user identity (name, email, profile picture) in the sidebar
- Forward JWT tokens to the backend on all API calls
- Support automatic token refresh via refresh tokens
- Enable single logout (frontend session + provider session)

## Non-goals

- Backend token validation (separate spec)
- Role-based access control / permissions
- User registration or self-service account management
- Storing user data in the database

## Technical Approach

### Auth.js v5 Setup

Add `next-auth` (Auth.js v5) as a dependency. Configure a generic OIDC provider using the environment variables from Spec 047:

```typescript
import NextAuth from "next-auth";

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    {
      id: "oidc",
      name: "OIDC",
      type: "oidc",
      issuer: process.env.OIDC_ISSUER_URI,
      clientId: process.env.OIDC_CLIENT_ID,
      clientSecret: process.env.OIDC_CLIENT_SECRET,
    },
  ],
  session: { strategy: "jwt" },
  // callbacks for token handling, profile mapping
});
```

**Rationale:** Auth.js v5 is the de-facto standard for Next.js authentication (~2.5M weekly downloads), has built-in OIDC support with `wellKnown` auto-discovery, handles session management via httpOnly cookies, and works with the App Router.

### Session Strategy

JWT-based sessions stored in an httpOnly cookie. No database session storage needed — the session lives entirely in the cookie. Auth.js encrypts the cookie content.

The JWT callback stores the access token and refresh token from the OIDC provider in the session JWT. The session callback exposes the user's `name`, `email`, and `image` (from the `picture` claim) to the client via `useSession()`.

### Token Refresh

Auth.js is configured to automatically refresh expired access tokens using the refresh token. The JWT callback checks token expiry and calls the provider's token endpoint with `grant_type=refresh_token` when the access token is expired.

If the refresh token is also expired or invalid, the user is redirected to the login page.

### Route Protection

All pages are protected using Next.js middleware (`middleware.ts`). The middleware checks for a valid Auth.js session on every request. Unauthenticated users are redirected to the Auth.js sign-in route, which triggers the OIDC Authorization Code Flow with the configured provider.

```typescript
export { auth as middleware } from "@/auth";

export const config = {
  matcher: ["/((?!api/auth).*)"],
};
```

The matcher excludes Auth.js's own API routes (`/api/auth/*`) to avoid redirect loops.

### API Proxy: Route Handler Replaces Rewrite

The current Next.js rewrite in `next.config.ts` (`/api/:path*` → backend) is replaced by a catch-all Route Handler at `app/api/[...path]/route.ts`. This handler:

1. Reads the Auth.js session to extract the access token
2. Forwards the original request to the backend with an `Authorization: Bearer <token>` header
3. Returns the backend's response to the client

```typescript
// app/api/[...path]/route.ts
import { auth } from "@/auth";

async function handler(req: Request, { params }: { params: { path: string[] } }) {
  const session = await auth();
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  const path = params.path.join("/");

  const response = await fetch(`${backendUrl}/api/${path}`, {
    method: req.method,
    headers: {
      ...Object.fromEntries(req.headers),
      Authorization: `Bearer ${session?.accessToken}`,
    },
    body: req.method !== "GET" && req.method !== "HEAD" ? await req.blob() : undefined,
  });

  return new Response(response.body, {
    status: response.status,
    headers: response.headers,
  });
}

export { handler as GET, handler as POST, handler as PUT, handler as DELETE };
```

**Rationale:** Next.js rewrites cannot add dynamic headers. A Route Handler has full access to the Auth.js session and can inject the `Authorization` header before forwarding. The frontend's API client (`lib/api.ts`) continues to call `/api/*` — no changes needed in API call code.

The rewrite rule in `next.config.ts` is removed.

### Logout

The logout button in the sidebar calls Auth.js's `signOut()` function. Auth.js is configured to redirect to the OIDC provider's end-session endpoint after clearing the local session cookie. This terminates both:

1. **Auth.js session** — httpOnly cookie is deleted
2. **Provider session** — redirect to the provider's `end_session_endpoint` (discovered via OIDC well-known config)

After the provider confirms logout, the user is redirected back to the app's login page.

### Sidebar User Display

The sidebar replaces the hardcoded dummy user with real session data:

- **Name:** `session.user.name` from the OIDC `name` claim
- **Email:** Not displayed in sidebar currently, but available via `session.user.email`
- **Profile picture:** `session.user.image` from the OIDC `picture` claim. If the claim is missing or null, the existing placeholder icon (lucide `CircleUser` or similar) is shown instead

The sidebar component uses the `useSession()` hook from Auth.js to access the current user data.

The hardcoded `currentUser` in `frontend/src/lib/user.ts` is removed.

### Environment Variables

The following environment variables are consumed (defined in Spec 047):

| Variable | Used by |
|----------|---------|
| `OIDC_ISSUER_URI` | Auth.js provider configuration |
| `OIDC_CLIENT_ID` | Auth.js provider configuration |
| `OIDC_CLIENT_SECRET` | Auth.js provider configuration |

Additionally, Auth.js requires:

| Variable | Description | Default |
|----------|-------------|---------|
| `AUTH_SECRET` | Encryption key for session cookies | (must be set, no default) |
| `AUTH_URL` | Base URL of the app (for callbacks) | auto-detected in most cases |

`AUTH_SECRET` is added to `.env.example` with a placeholder value and a comment explaining it must be changed.

### OIDC Environment Variables in docker-compose

The `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, and `AUTH_SECRET` are added to the frontend service's `environment` section in `docker-compose.yml` so they are available at runtime. In `docker-compose.override.yml`, `OIDC_ISSUER_URI` is overridden to point to the mock server's internal Docker hostname.

### Files Affected

**Frontend (new):**
- `frontend/src/auth.ts` — Auth.js configuration (provider, callbacks, session strategy)
- `frontend/src/middleware.ts` — Route protection middleware
- `frontend/src/app/api/[...path]/route.ts` — API proxy Route Handler with token forwarding
- `frontend/src/app/api/auth/[...nextauth]/route.ts` — Auth.js API route handlers

**Frontend (modified):**
- `frontend/package.json` — add `next-auth` dependency
- `frontend/src/components/sidebar.tsx` — replace dummy user with `useSession()` data, implement logout via `signOut()`
- `frontend/next.config.ts` — remove rewrite rule
- `frontend/src/lib/api.ts` — no changes needed (continues to call `/api/*`)

**Frontend (removed):**
- `frontend/src/lib/user.ts` — hardcoded dummy user no longer needed

**Infrastructure (modified):**
- `.env.example` — add `AUTH_SECRET`
- `docker-compose.yml` — add OIDC env vars + `AUTH_SECRET` to frontend service
- `docker-compose.override.yml` — override `OIDC_ISSUER_URI` for mock server

## Dependencies

- **Spec 047 (OIDC Infrastructure)** — mock-oauth2-server and OIDC env vars must be in place
- **Auth.js v5** — new npm dependency (`next-auth@5`)

## Security Considerations

- Session cookie is httpOnly and encrypted by Auth.js — not accessible via JavaScript
- Access tokens are stored in the encrypted JWT cookie, never exposed to the browser
- `AUTH_SECRET` must be a strong random value in production
- CSRF protection is handled by Auth.js automatically
- The API proxy route only forwards requests for authenticated sessions

## Open Questions

None — all details resolved during design discussion.
