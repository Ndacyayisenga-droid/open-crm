# @open-elements/nextjs-app-layer

In-repo pnpm workspace package that extracts the Next.js foundation every
Open Elements app of the Open-CRM family needs. Currently consumed only by
`open-crm-frontend`.

This is an **in-progress extraction** of spec
[098-extract-nextjs-app-layer](../../../specs/098-extract-nextjs-app-layer/).
Phases 1–6 (workspace, utils, DTOs, translations, server factories,
shared components, API client) are landed. Phases 7–9 (page migrations,
`OERootLayout`, full README and test suite) are pending.

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

### `@open-elements/nextjs-app-layer/server` (server-only)

- `createAppLayerAuth({ issuer, clientId, clientSecret })`
- `createBackendProxyHandler({ backendUrl, auth })`
- `createLogoutHandler({ auth, oidcIssuer, authUrl })`
- `middlewareConfig`

### `@open-elements/nextjs-app-layer/server/next-auth-types`

Side-effect module that augments NextAuth's `Session` type. Apps activate
it via `import "@open-elements/nextjs-app-layer/server/next-auth-types";`
inside their own `auth.ts`.

## Wiring (current state — Open CRM)

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
export { middlewareConfig as config } from "@open-elements/nextjs-app-layer/server";
```

```tsx
// frontend/src/app/layout.tsx (excerpt)
<SessionProvider>
  <LanguageProvider translations={appTranslations}>
    <AppLayerTranslationProvider>
      <ApiClientProvider>{children}</ApiClientProvider>
    </AppLayerTranslationProvider>
  </LanguageProvider>
</SessionProvider>
```

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
