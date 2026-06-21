# Behaviors: Extract @open-elements/nextjs-app-layer

## Workspace integration

### Lib package is discoverable as a workspace dependency

- **Given** `frontend/pnpm-workspace.yaml` lists `.` and `packages/*`
- **And** `frontend/packages/nextjs-app-layer/package.json` declares name `@open-elements/nextjs-app-layer`
- **And** `frontend/package.json` declares dependency `"@open-elements/nextjs-app-layer": "workspace:*"`
- **When** `pnpm install` is run in `frontend/`
- **Then** `frontend/node_modules/@open-elements/nextjs-app-layer` resolves to the in-repo package via symlink

### Lib is consumed in source mode

- **Given** `next.config.ts` lists `"@open-elements/nextjs-app-layer"` in `transpilePackages`
- **When** `pnpm --filter open-crm-frontend build` is run
- **Then** the build succeeds without a prior build step in the lib
- **And** TypeScript and Tailwind classes from lib sources are included in the production output

### Tailwind v4 sees lib sources

- **Given** the `@source` directive in `globals.css` includes `packages/nextjs-app-layer/src/**/*.{ts,tsx}`
- **When** a lib component uses a Tailwind utility class that no app file uses
- **Then** the class is present in the production CSS bundle

### Test runner covers both packages

- **Given** Vitest is configured in both `frontend/` and `frontend/packages/nextjs-app-layer/`
- **When** `pnpm -r test` is run from `frontend/`
- **Then** both the app's tests and the lib's tests are executed

## Public API surface

### `index.ts` does not leak internals

- **Given** the lib's `src/index.ts` enumerates explicit named exports
- **When** a consumer imports a name not listed in `index.ts`
- **Then** TypeScript reports an error (no implicit re-export of internal modules)

### Server-only code is reachable only via `/server`

- **Given** a consumer imports `createAppLayerAuth` from `@open-elements/nextjs-app-layer`
- **Then** the import does not resolve (the symbol is only exported from `@open-elements/nextjs-app-layer/server`)

### Next-auth augmentation is opt-in via side-effect import

- **Given** the consuming app's `auth.ts` includes `import "@open-elements/nextjs-app-layer/server/next-auth.d";`
- **When** code reads `session.accessToken`
- **Then** TypeScript accepts the access (the augmented `Session` type is in scope)

- **Given** an app that does not include the side-effect import
- **When** code reads `session.accessToken`
- **Then** TypeScript reports the property as unknown on the base `Session` type

## Auth factory

### Initial sign-in populates the session

- **Given** a successful OIDC sign-in with a profile that contains `name`, `email`, `picture`, and a `roles` array
- **When** `auth()` is called from a server component immediately after sign-in
- **Then** the returned session contains `accessToken`, `idToken`, `expiresAt`, `roles` equal to the profile array, and `user.{name, email, image}` populated from the profile

### Refresh-token flow extends an expiring session

- **Given** a session whose `expiresAt` is less than 60 seconds in the future
- **And** a valid `refreshToken` is stored on the JWT
- **When** the JWT callback runs on a subsequent request
- **Then** a token refresh is performed against the OIDC `token_endpoint`
- **And** `accessToken` and `expiresAt` are replaced with the new values
- **And** `error` on the session is `undefined`

### Refresh-token failure surfaces `RefreshTokenError`

- **Given** the OIDC token endpoint returns a non-2xx response during refresh
- **When** the JWT callback runs
- **Then** the session's `error` is set to `"RefreshTokenError"`
- **And** the session's `accessToken` becomes `undefined`
- **And** `SessionProvider`'s watcher redirects the browser to `/api/logout`

### Profile without a `roles` claim defaults to an empty list

- **Given** the OIDC profile does not include a `roles` property
- **When** the session is built
- **Then** `session.roles` is `[]`

## Backend proxy

### Bearer token is attached from the session

- **Given** an authenticated session with `accessToken = "ABC"`
- **When** the browser performs `fetch("/api/companies")`
- **Then** the proxy issues `GET ${BACKEND_URL}/api/companies` with header `Authorization: Bearer ABC`

### Headers `Content-Type` and `Accept` are forwarded

- **Given** the browser sends `Content-Type: application/json` and `Accept: application/json`
- **When** the request passes through the proxy
- **Then** both headers are present on the upstream request

### Query parameters are forwarded

- **Given** the browser performs `fetch("/api/audit-logs?page=0&size=20&entityType=Company")`
- **When** the proxy handles the request
- **Then** the upstream URL includes `page=0`, `size=20`, and `entityType=Company`

### Non-GET/HEAD methods forward the body

- **Given** the browser performs a `POST` with a JSON body
- **When** the proxy forwards the request
- **Then** the upstream `POST` receives the same body bytes

### GET requests do not forward a body

- **Given** the browser performs a `GET`
- **When** the proxy forwards the request
- **Then** the upstream request has no body

## Logout

### Successful logout redirects to OIDC end-session

- **Given** the OIDC issuer's `.well-known` document contains `end_session_endpoint`
- **And** the session has a stored `idToken`
- **When** the browser navigates to `/api/logout`
- **Then** it is redirected to the `end_session_endpoint` with `id_token_hint` and `post_logout_redirect_uri=<authUrl>/login`
- **And** all session cookies and their chunks are cleared

### Logout falls back to `/login` when OIDC discovery fails

- **Given** the OIDC discovery request throws
- **When** the browser navigates to `/api/logout`
- **Then** it is redirected to `<authUrl>/login`
- **And** session cookies are still cleared

### Cookie attributes match HTTP vs. HTTPS

- **Given** `AUTH_URL` is `https://crm.example.com`
- **When** the logout handler clears cookies
- **Then** the `Set-Cookie` deletion headers use `secure: true`

- **Given** `AUTH_URL` is `http://localhost:3000`
- **When** the logout handler clears cookies
- **Then** the `Set-Cookie` deletion headers use `secure: false`

## Translation provider

### Lib pages render in the active app language

- **Given** the app's `LanguageProvider` is set to `de`
- **And** `<AppLayerTranslationProvider>` wraps the lib pages
- **When** an admin page renders
- **Then** lib-owned strings are rendered from `appLayerTranslations.de`

### Missing provider throws a sprechenden Error

- **Given** a lib component calls `useAppLayerTranslations()`
- **And** `<AppLayerTranslationProvider>` is **not** in the React tree
- **When** the component mounts
- **Then** an Error is thrown whose message identifies the missing provider

## API client provider

### Default client routes to the OE proxy paths

- **Given** the app wraps with `<ApiClientProvider>` and passes no `client` prop
- **When** `useApiClient().getAuditLogs({ page: 0, size: 20 })` is called
- **Then** the default implementation calls `fetch("/api/audit-logs?page=0&size=20", { ... })`

### Replacement client is used exactly as provided

- **Given** the app wraps with `<ApiClientProvider client={customClient}>`
- **When** a lib component calls `useApiClient().getUsers(...)`
- **Then** the call routes to `customClient.getUsers` and no default fetch is issued

### Missing provider throws

- **Given** a lib component calls `useApiClient()`
- **And** no `<ApiClientProvider>` is in the React tree
- **When** the component mounts
- **Then** an Error is thrown identifying the missing provider

## Role guards on admin pages

### IT-ADMIN role sees the page content

- **Given** an authenticated session whose `roles` contains `"IT-ADMIN"`
- **When** the user navigates to any of `/admin/audit-logs`, `/admin/users`, `/admin/status`, `/admin/token`, `/api-keys`, or `/webhooks`
- **Then** the page's client component is rendered

### Non-IT-ADMIN session sees `ForbiddenPage`

- **Given** an authenticated session whose `roles` does **not** contain `"IT-ADMIN"`
- **When** the user navigates to any admin page above
- **Then** `ForbiddenPage` is rendered server-side
- **And** the protected client component is not included in the response

### Unauthenticated request redirects to `/login`

- **Given** no session cookie
- **When** the user navigates to any admin page above
- **Then** the middleware redirects to `/login`

## Page metadata exports

### Page meta carries default route, icon, and label resolver

- **Given** the lib exports `auditLogsPageMeta`
- **Then** the meta object has `defaultRoute = "/admin/audit-logs"`, `icon = FileText`, `requiredRole = "IT-ADMIN"`, and a `label` function returning `t.nav.auditLogs` from `AppLayerTranslations`

### App sidebar consumes the meta for its `NavItem`

- **Given** the app's sidebar imports `auditLogsPageMeta`
- **When** it renders a `NavItem` for the audit-logs entry
- **Then** the `href` matches `auditLogsPageMeta.defaultRoute`, the `icon` matches `auditLogsPageMeta.icon`, and the label resolves via the same translation key

## Audit-logs page (parity with current behavior)

### Initial render fetches page 0 with stored page size

- **Given** `localStorage.pageSize.auditLogs` is `50`
- **When** the page mounts
- **Then** the lib calls `getAuditLogs({ page: 0, size: 50 })`
- **And** until the response arrives a Skeleton is shown

### Empty result shows the empty state

- **Given** the API returns a page with empty `content` and `totalElements = 0`
- **When** the page finishes loading
- **Then** the empty illustration (`FileText`) and the empty translation string are shown

### API failure shows the error state

- **Given** the API rejects
- **When** the page finishes loading
- **Then** the `AlertCircle` icon and `t.auditLog.loadError` text are shown with `role="alert"`

### Changing the entity-type filter resets to page 0

- **Given** the page is on page 3
- **When** the user changes the entity-type filter
- **Then** the lib calls `getAuditLogs({ page: 0, size: <same>, entityType: <new>, user: <same> })`

### Changing the user filter resets to page 0

- **Given** the page is on page 3
- **When** the user changes the user filter
- **Then** the lib calls `getAuditLogs({ page: 0, size: <same>, entityType: <same>, user: <new> })`

### User dropdown loads up to 200 users

- **Given** the page mounts
- **Then** `getUsers({ size: 200 })` is called exactly once
- **And** the dropdown shows one option per returned user plus an "All users" option

## Users page (parity)

### Initial render shows skeletons, then data

- **Given** the API returns a non-empty page of users
- **When** the page mounts
- **Then** five `Skeleton` rows are shown until the response arrives
- **And** the table renders one row per user with avatar, name, email columns

### Avatar fallback when `avatarUrl` is null

- **Given** a user with `avatarUrl = null`
- **When** the row renders
- **Then** the avatar cell shows the fallback icon with `data-testid="user-avatar-fallback"`

## API-keys page (post-normalization parity)

### Loading state uses Skeletons

- **Given** the page is loading
- **Then** five `Skeleton` rows are shown (consistent with audit-logs / users)

### Empty state shows the create-first CTA

- **Given** the API returns an empty page
- **Then** the `KeyRound` icon, the empty translation, and a "Create first key" button are shown

### Create dialog produces a new key and shows it once

- **Given** the create dialog is open
- **And** the user enters a non-empty name and presses Enter or clicks Create
- **When** the API returns a `ApiKeyCreatedDto`
- **Then** the create dialog closes
- **And** a second dialog opens showing the key value with a copy button and a warning
- **And** dismissing the second dialog triggers a re-fetch of the key list

### Empty name shows inline validation

- **Given** the create dialog is open with an empty name
- **When** the user clicks Create
- **Then** the lib does **not** call the API
- **And** an inline error message is shown

### Delete is confirmed via `DeleteConfirmDialog`

- **Given** the user clicks the delete icon on a row
- **When** they confirm the dialog
- **Then** the lib calls `deleteApiKey(id)` and re-fetches the list

### API error during delete is surfaced

- **Given** `deleteApiKey` rejects with a message
- **When** the rejection arrives
- **Then** the error message is shown inside the delete dialog (the dialog stays open)

### Table renders with the same component used by audit-logs

- **Given** the migrated client
- **Then** it uses `Table`, `TableHeader`, `TableRow`, `TableHead`, `TableBody`, `TableCell` from `@open-elements/ui`
- **And** the loading/error/empty states match the audit-logs/users layout

## Webhooks page (post-normalization parity)

### Create dialog accepts a URL

- **Given** the create dialog is open and the user enters a non-empty URL
- **When** they confirm
- **Then** the lib calls `createWebhook({ url })` and re-fetches the list

### Toggle active flips the boolean

- **Given** a webhook with `active = true`
- **When** the user clicks the active-toggle button
- **Then** the lib calls `updateWebhook(id, { url: <same>, active: false })`
- **And** re-fetches the list on success

### Ping fires the registered URL

- **Given** the user clicks the ping icon for a webhook
- **When** the click is handled
- **Then** the lib calls `pingWebhook(id)` (fire-and-forget; no visible feedback expected)

### Status formatting

- **Given** the most recent call to a webhook returned status `null`
- **Then** the status cell shows the "never called" translation
- **Given** the most recent status is `-1`
- **Then** the cell shows the "timeout" translation
- **Given** the most recent status is `0`
- **Then** the cell shows the "connection error" translation
- **Given** the most recent status is `204`
- **Then** the cell shows the "ok" translation
- **Given** the most recent status is `500`
- **Then** the cell shows the "bad call (500)" translation

### Error state on initial load matches audit-logs style

- **Given** `getWebhooks` rejects
- **Then** an `AlertCircle` block with the load-error translation is shown (new behavior added by this normalization)

## Server-status page (parity)

### Healthy backend renders the "up" indicator

- **Given** `GET /api/health` returns `200` with `{ status: "UP" }`
- **When** the page mounts
- **Then** `HealthStatus` is rendered with `healthy = true`

### Non-OK or non-UP response renders "down"

- **Given** `GET /api/health` returns non-2xx or `{ status: "DOWN" }`
- **Then** `HealthStatus` is rendered with `healthy = false`

## Bearer-token page (parity)

### Token is initially hidden

- **Given** the page renders with a valid session
- **Then** the token text is shown as a mask (dots)
- **And** a Show button toggles visibility
- **And** a Copy button copies the raw token to the clipboard

### Remaining validity ticks down

- **Given** `session.expiresAt` is in 5 minutes
- **When** the validity timer fires
- **Then** the displayed remaining time shows a positive `mm:ss` value
- **Given** `expiresAt` is in the past
- **Then** the "expired" translation is shown in the destructive color

### No session shows "no token"

- **Given** no session is available
- **Then** the card shows the "no token" translation

## Login page (parity)

### Authenticated user is redirected to `homeRoute`

- **Given** a session exists
- **When** the login page mounts
- **Then** the router replaces the URL with `homeRoute`
- **And** no login UI is rendered

### Error query param surfaces the error message

- **Given** the URL contains `?error=…`
- **When** the page renders
- **Then** the error translation is shown in destructive color
- **And** the original error string is logged to the console

### Sign-in button triggers OIDC

- **Given** the page is rendered for an unauthenticated visitor
- **When** the user clicks the sign-in button
- **Then** `signIn("oidc")` from `next-auth/react` is called

## Forbidden page (parity)

### Renders icon, heading, body, and home link

- **Given** a user without the required role visits a protected route
- **When** the lib renders `ForbiddenPage`
- **Then** the `ShieldAlert` icon, the forbidden title, the forbidden description, and a button linking to `homeRoute` are present

## Migration verification

### Original app files are removed after migration

- **When** the migration of a given page is complete
- **Then** the corresponding original file under `frontend/src/...` is deleted (no dead duplicate remains)

### App `page.tsx` files are 2-line re-exports

- **Given** a migrated page (e.g., `frontend/src/app/(app)/admin/audit-logs/page.tsx`)
- **Then** the file contains at most: an import of the lib's `createAuditLogsPage` (plus app `auth`) and a single default export

### Open CRM functional parity

- **Given** the migration is complete
- **When** an IT-ADMIN user clicks through audit-logs, users, server status, bearer token, api-keys, webhooks, login, logout, and forbidden
- **Then** every flow renders, behaves, and translates identically to the pre-migration state

### CI is green

- **When** `pnpm install && pnpm -r test && pnpm --filter open-crm-frontend build` is run from `frontend/`
- **Then** the command exits 0
