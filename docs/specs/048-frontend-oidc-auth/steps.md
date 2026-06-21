# Implementation Steps: Frontend OIDC Auth

## Step 1: Install next-auth and create Auth.js configuration

- [x] Add `next-auth@5` dependency to `frontend/package.json`
- [x] Run `pnpm install` to install the dependency
- [x] Create `frontend/src/auth.ts` with OIDC provider configuration using `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`
- [x] Configure JWT session strategy with access token and refresh token storage in JWT callback
- [x] Configure session callback to expose user name, email, image, and accessToken
- [x] Implement token refresh logic in JWT callback (check expiry, call token endpoint with refresh_token grant)
- [x] Create `frontend/src/app/api/auth/[...nextauth]/route.ts` exporting Auth.js route handlers

**Acceptance criteria:**
- [x] `pnpm install` succeeds without errors
- [x] `frontend/src/auth.ts` exports `handlers`, `auth`, `signIn`, `signOut`
- [x] Auth.js route handler file exists at `frontend/src/app/api/auth/[...nextauth]/route.ts`
- [x] Project builds successfully (`pnpm build`)

**Related behaviors:** Login works with mock-oauth2-server, Login works with Authentik, Session stored as JWT cookie, Session contains user claims, Expired access token is refreshed automatically, Expired refresh token redirects to login

---

## Step 2: Add route protection middleware

- [x] Create `frontend/src/middleware.ts` that exports Auth.js `auth` as middleware
- [x] Configure matcher to exclude `/api/auth/*` routes

**Acceptance criteria:**
- [x] `frontend/src/middleware.ts` exists with correct matcher configuration
- [x] Project builds successfully

**Related behaviors:** All pages require authentication, Unauthenticated user is redirected to OIDC provider, Auth.js API routes are not protected

---

## Step 3: Replace API rewrite with proxy Route Handler

- [x] Create `frontend/src/app/api/[...path]/route.ts` as catch-all Route Handler
- [x] Implement token injection: read Auth.js session, add `Authorization: Bearer <token>` header
- [x] Forward all HTTP methods (GET, POST, PUT, DELETE) to backend
- [x] Return backend response status and body unchanged
- [x] Remove `rewrites()` function from `frontend/next.config.ts`

**Acceptance criteria:**
- [x] Route Handler exists and exports GET, POST, PUT, DELETE handlers
- [x] `next.config.ts` no longer contains `rewrites()`
- [x] Project builds successfully

**Related behaviors:** API calls include Authorization header, API proxy forwards all HTTP methods, API proxy returns backend response, API proxy handles backend errors, Rewrite rule removed from next.config.ts

---

## Step 4: Update sidebar with real user data and logout

- [x] Replace `currentUser` import with Auth.js `useSession()` hook in sidebar
- [x] Display `session.user.name` instead of hardcoded name
- [x] Display `session.user.image` as profile picture when available, fall back to `CircleUser` icon
- [x] Implement logout button to call `signOut()` with redirect to OIDC provider's end-session endpoint
- [x] Delete `frontend/src/lib/user.ts`
- [x] Wrap the app with `SessionProvider` in the root layout (required for `useSession()`)

**Acceptance criteria:**
- [x] `frontend/src/lib/user.ts` no longer exists
- [x] No references to `currentUser` in the codebase
- [x] Sidebar displays session user data
- [x] Logout button calls Auth.js `signOut()`
- [x] Project builds successfully

**Related behaviors:** Real user name shown in sidebar, Real profile picture shown in sidebar, Placeholder icon shown when no picture claim, User section shown on mobile sidebar, user.ts removed, No references to hardcoded currentUser, Logout clears frontend session, Logout terminates provider session, After logout user is redirected to login

---

## Step 5: Update environment variables and Docker configuration

- [x] Add `AUTH_SECRET` with placeholder to `.env.example`
- [x] Add `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `AUTH_SECRET` to frontend service environment in `docker-compose.yml`
- [x] Override `OIDC_ISSUER_URI` for mock server in `docker-compose.override.yml` frontend service if needed

**Acceptance criteria:**
- [x] `.env.example` contains `AUTH_SECRET` with a placeholder
- [x] `docker-compose.yml` frontend service has all four OIDC/auth env vars
- [x] Project builds successfully

**Related behaviors:** AUTH_SECRET in .env.example, OIDC vars passed to frontend service in docker-compose

---

## Step 6: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` with OIDC auth feature
- [x] Update `.claude/conventions/project-specific/project-tech.md` with next-auth dependency
- [x] Update `.claude/conventions/project-specific/project-structure.md` with new auth files
- [x] Update `.claude/conventions/project-specific/project-architecture.md` with auth flow
- [x] Update `README.md` if setup instructions or configuration changed

**Acceptance criteria:**
- [x] All documentation files reflect the new OIDC authentication feature
- [x] Project builds successfully

**Related behaviors:** N/A (documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Unauthenticated user is redirected to OIDC provider | Frontend | Step 2 |
| Successful login redirects back to the app | Frontend | Step 1 |
| Login works with mock-oauth2-server | Frontend | Step 1 |
| Login works with Authentik | Frontend | Step 1 |
| All pages require authentication | Frontend | Step 2 |
| Auth.js API routes are not protected | Frontend | Step 2 |
| Session stored as JWT cookie | Frontend | Step 1 |
| Session contains user claims | Frontend | Step 1 |
| Expired access token is refreshed automatically | Frontend | Step 1 |
| Expired refresh token redirects to login | Frontend | Step 1 |
| API calls include Authorization header | Frontend | Step 3 |
| API proxy forwards all HTTP methods | Frontend | Step 3 |
| API proxy returns backend response | Frontend | Step 3 |
| API proxy handles backend errors | Frontend | Step 3 |
| Rewrite rule removed from next.config.ts | Frontend | Step 3 |
| Logout clears frontend session | Frontend | Step 4 |
| Logout terminates provider session | Frontend | Step 4 |
| After logout user is redirected to login | Frontend | Step 4 |
| Real user name shown in sidebar | Frontend | Step 4 |
| Real profile picture shown in sidebar | Frontend | Step 4 |
| Placeholder icon shown when no picture claim | Frontend | Step 4 |
| User section shown on mobile sidebar | Frontend | Step 4 |
| user.ts removed | Frontend | Step 4 |
| No references to hardcoded currentUser | Frontend | Step 4 |
| AUTH_SECRET in .env.example | Infra | Step 5 |
| OIDC vars passed to frontend service in docker-compose | Infra | Step 5 |
