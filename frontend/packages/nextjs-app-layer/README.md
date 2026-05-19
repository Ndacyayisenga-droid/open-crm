# @open-elements/nextjs-app-layer

In-repo pnpm workspace package that extracts the Next.js foundation every
Open Elements app of the Open-CRM family needs. Currently consumed only by
`open-crm-frontend`.

This package implements spec
[098-extract-nextjs-app-layer](../../../specs/098-extract-nextjs-app-layer/).

## Public entry points

### `@open-elements/nextjs-app-layer` (client-safe)

- `ROLE_ADMIN`, `ROLE_IT_ADMIN`, `hasRole(session, role)`
- `ForbiddenError`
- DTO types: `Page<T>`, `UserDto`, `AuditAction`, `AuditLogDto`,
  `ApiKeyDto`, `ApiKeyCreateDto`, `ApiKeyCreatedDto`, `WebhookDto`,
  `WebhookCreateDto`, `WebhookUpdateDto`, `TranslationConfigDto`,
  `PageRequest`
- `AppLayerTranslationProvider`, `useAppLayerTranslations`,
  `appLayerTranslations`, type `AppLayerTranslations`
- `SessionProvider`, `ForbiddenPage`, `BearerTokenCard`,
  `AddCommentDialog`
- `ApiClientProvider`, `useApiClient`, `defaultApiClient`,
  type `AppLayerApiClient`
- Page factories + clients + metas for each migrated admin page:
  - `createAuditLogsPage`, `AuditLogsClient`, `auditLogsPageMeta`
  - `createUsersPage`, `UsersClient`, `usersPageMeta`
  - `createServerStatusPage`, `ServerStatusClient`, `serverStatusPageMeta`
  - `createBearerTokenPage`, `BearerTokenClient`, `bearerTokenPageMeta`
  - `createApiKeysPage`, `ApiKeysClient`, `apiKeysPageMeta`
  - `createWebhooksPage`, `WebhooksClient`, `webhooksPageMeta`
  - `createLoginPage`, `LoginClient`

### `@open-elements/nextjs-app-layer/server` (server-only)

- `createAppLayerAuth({ issuer, clientId, clientSecret })`
- `createBackendProxyHandler({ backendUrl, auth })`
- `createLogoutHandler({ auth, oidcIssuer, authUrl })`
- `middlewareConfig` (reference value — see warning below)

### `@open-elements/nextjs-app-layer/server/next-auth-types`

Side-effect module that augments NextAuth's `Session` type. Apps activate
it via `import "@open-elements/nextjs-app-layer/server/next-auth-types";`
inside their own `auth.ts`.

### `@open-elements/nextjs-app-layer/layout`

`OERootLayout` — root layout component that renders `<html>` with the
Montserrat/Lato font variables and the full provider stack
(`SessionProvider`, `LanguageProvider`, `AppLayerTranslationProvider`,
`ApiClientProvider`). It is kept on its own entry point so the
`next/font/google` runtime call does not get pulled into the client-safe
barrel (which would force every test to mock `next/font/google`).

## Wiring (Open CRM)

```ts
// frontend/src/auth.ts
import "@open-elements/nextjs-app-layer/server/next-auth-types";
import { createAppLayerAuth } from "@open-elements/nextjs-app-layer/server";

export const { handlers, auth, signIn, signOut, oidcIssuer } =
  createAppLayerAuth({
    issuer: process.env.OIDC_ISSUER_URI,
    clientId: process.env.OIDC_CLIENT_ID,
    clientSecret: process.env.OIDC_CLIENT_SECRET,
  });
```

```ts
// frontend/src/app/api/[...path]/route.ts
import { auth } from "@/auth";
import { createBackendProxyHandler } from "@open-elements/nextjs-app-layer/server";

const handler = createBackendProxyHandler({
  backendUrl: process.env.BACKEND_URL ?? "http://localhost:8080",
  auth,
});
export { handler as GET, handler as POST, handler as PUT, handler as DELETE };
```

```ts
// frontend/src/app/api/logout/route.ts
import { auth, oidcIssuer } from "@/auth";
import { createLogoutHandler } from "@open-elements/nextjs-app-layer/server";

export const GET = createLogoutHandler({
  auth,
  oidcIssuer,
  authUrl: process.env.AUTH_URL ?? "http://localhost:3000",
});
```

```ts
// frontend/src/middleware.ts
export { auth as middleware } from "@/auth";

// `config` MUST be a static literal here. Next.js' build-time analyzer
// extracts the matcher directly from middleware.ts and does NOT follow
// re-exports across the workspace-package boundary. Re-exporting the lib's
// `middlewareConfig` as `config` silently disables the matcher in production
// — `/_next/static/*` requests get routed through the auth middleware and
// the deployment breaks. The lib's `middlewareConfig` is reference-only.
export const config = {
  matcher: [
    "/((?!api/auth|api/logout|login|_next/static|_next/image|favicon\\.ico|.*\\.svg$|.*\\.png$|.*\\.jpg$|.*\\.ico$).*)",
  ],
};
```

```tsx
// frontend/src/app/layout.tsx
import type { Metadata } from "next";
import { OERootLayout } from "@open-elements/nextjs-app-layer/layout";
import { translations } from "@/lib/i18n";
import "./globals.css";

export const metadata: Metadata = {
  title: "Open CRM",
  description: "CRM system by Open Elements",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <OERootLayout translations={translations}>{children}</OERootLayout>;
}
```

```tsx
// frontend/src/app/(app)/admin/audit-logs/page.tsx
import { auth } from "@/auth";
import { createAuditLogsPage } from "@open-elements/nextjs-app-layer";
export default createAuditLogsPage({ auth });
```

The same 2-line shape applies to `users/page.tsx`, `admin/status/page.tsx`,
`admin/token/page.tsx`, `api-keys/page.tsx`, `webhooks/page.tsx`, and
`login/page.tsx`.

## OE conventions this lib assumes

- OIDC role names: `IT-ADMIN` and `ADMIN` (hardcoded).
- Proxy pattern: every backend call goes through `/api/...` in the same
  origin as the Next.js app.
- Fonts: Montserrat (heading), Lato (body).
- Brand: provided by `@open-elements/ui` (`@import
  "@open-elements/ui/styles/brand.css"` in the app's `globals.css`).

## Deferred follow-up specs

The current design intentionally keeps the lib's public surface narrow.
The following are not supported today and will be addressed in dedicated
follow-up specs when concrete need arises:

- Configurable role names (per-app role mapping).
- Auth-factory extensibility hooks (custom claims, additional providers,
  signIn validation).
- Page-level customization (sub-component exports, slot props).
- Per-string translation overrides.
- Phase-2 transition to a built lib (`tsc -b`, drop `transpilePackages`).
- Phase-3 publishing decision (npm or own repo).
