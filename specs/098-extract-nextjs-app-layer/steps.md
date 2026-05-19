# Implementation Steps: Extract @open-elements/nextjs-app-layer

Tracking the 9-phase migration plan from `design.md` § "Key flows — Migration
order". Phases 1–6 are landed in PR #18 as a milestone (workspace + foundation);
phases 7–9 follow in a separate PR.

## Phase 1 — Workspace skeleton

- [x] `frontend/pnpm-workspace.yaml` lists `.` and `packages/*`.
- [x] `frontend/packages/nextjs-app-layer/package.json` declares
      `@open-elements/nextjs-app-layer`, exports `.` and `/server`, lists peer
      deps (Next, next-auth, React, @open-elements/ui, lucide-react).
- [x] `frontend/packages/nextjs-app-layer/tsconfig.json` and
      `vitest.config.ts` mirror the app setup.
- [x] `frontend/package.json` adds `"@open-elements/nextjs-app-layer":
      "workspace:*"`.
- [x] `next.config.ts` lists the lib in `transpilePackages`.
- [x] `globals.css` adds `@source "../../packages/nextjs-app-layer/src/**/*.{ts,tsx}"`.
- [x] `pnpm install` succeeds; the lib resolves via workspace symlink.
- [x] App build remains green.

**Related behaviors:** Lib package is discoverable as a workspace dependency;
Lib is consumed in source mode; Tailwind v4 sees lib sources.

## Phase 2 — Pure utilities + DTOs

- [x] `nextjs-app-layer/src/lib/roles.ts` owns `ROLE_ADMIN`, `ROLE_IT_ADMIN`,
      `hasRole`.
- [x] `nextjs-app-layer/src/lib/forbidden-error.ts` owns `ForbiddenError`.
- [x] `nextjs-app-layer/src/api/types.ts` owns `Page<T>`, `UserDto`,
      `AuditAction`, `AuditLogDto`, `ApiKey*`, `Webhook*`,
      `TranslationConfigDto`, `PageRequest`.
- [x] App's `lib/roles.ts` and `lib/forbidden-error.ts` become 1-line
      re-exports.
- [x] App's `lib/types.ts` re-exports the migrated DTOs from the lib.
- [x] App build remains green.

## Phase 3 — Translation provider

- [x] `nextjs-app-layer/src/translations/{de,en}.ts` carry the lib-owned
      strings (`login`, `nav.*`, `admin`, `health`, `webhooks`, `apiKeys`,
      `users`, `auditLog`, `errors.forbidden`).
- [x] `AppLayerTranslationProvider` reads the active language from
      `useLanguage()` of `@open-elements/ui`.
- [x] `useAppLayerTranslations()` throws when used outside the provider.
- [x] Exported from the lib's public `index.ts`.

## Phase 4 — Server factories

- [x] `createAppLayerAuth({ issuer, clientId, clientSecret })` factory.
- [x] `createBackendProxyHandler({ backendUrl, auth })` factory.
- [x] `createLogoutHandler({ auth, oidcIssuer, authUrl })` factory.
- [x] `middlewareConfig` matcher exported.
- [x] Side-effect `import` module
      `@open-elements/nextjs-app-layer/server/next-auth-types` carries the
      `declare module "next-auth"` augmentation.
- [x] App's `auth.ts`, `app/api/[...path]/route.ts`, `app/api/logout/route.ts`,
      and `middleware.ts` shrink to thin shells.
- [x] App build remains green.

## Phase 5 — Shared components

- [x] `SessionProvider`, `ForbiddenPage` (with optional `homeRoute` prop),
      `BearerTokenCard`, `AddCommentDialog` moved into the lib.
- [x] `ForbiddenPage` and `BearerTokenCard` switched to
      `useAppLayerTranslations`.
- [x] App-side files become 1-line re-exports.
- [x] `AppLayerTranslationProvider` wired into the root layout so the
      lib components have a provider in scope.

## Phase 6 — API client

- [x] `AppLayerApiClient` interface with the 13 lib-relevant methods.
- [x] `defaultApiClient` calls the OE proxy paths (`/api/...`).
- [x] `ApiClientProvider` + `useApiClient()` (throws when missing).
- [x] `ApiClientProvider` wired into the root layout.

---

## Phase 7 — Page migrations (deferred)

The seven lib-owned pages and their normalization are deferred to a follow-up
PR to keep the milestone PR reviewable. Each page needs the same pattern:

- [ ] Migrate `audit-logs-client.tsx` into the lib; switch to
      `useAppLayerTranslations` + `useApiClient`. Add `createAuditLogsPage({auth})`
      factory + `auditLogsPageMeta`.
- [ ] Same for `users-client.tsx`.
- [ ] Migrate the server-status page; add `createStatusPage({auth})` +
      `statusPageMeta`.
- [ ] Migrate the bearer-token page (already mostly there via
      `BearerTokenCard`); add `createTokenPage({auth})` + `tokenPageMeta`.
- [ ] Migrate `api-keys-client.tsx` AND normalize to `Table` + `AlertCircle`
      style (matching audit-logs/users).
- [ ] Migrate `webhooks-client.tsx` AND normalize to `Table` + `AlertCircle`.
- [ ] Migrate the login page; expose as a default page export.
- [ ] App-side `page.tsx` for each becomes a 2-line `createXyzPage({auth})`
      re-export.
- [ ] Delete original app-side files after each migration.
- [ ] Move existing component tests into the lib package.

**Related behaviors:** all scenarios under "Role guards on admin pages",
"Page metadata exports", "Audit-logs page", "Users page", "API-keys page",
"Webhooks page", "Server-status page", "Bearer-token page", "Login page",
"Forbidden page", and "Migration verification".

## Phase 8 — OERootLayout (deferred)

- [ ] `OERootLayout` ships from the lib, wrapping `<html>` + fonts +
      `SessionProvider` + `LanguageProvider` + `AppLayerTranslationProvider`
      + `ApiClientProvider`.
- [ ] App's `app/layout.tsx` shrinks to a 4-line shell that passes
      `translations`.

## Phase 9 — README + index audit (deferred)

- [ ] `packages/nextjs-app-layer/README.md` documenting both entry points,
      the supported wiring (auth, middleware, proxy/logout routes, layout,
      per-page re-exports), the OE conventions assumed (role names, proxy
      paths, fonts, logo path), and the explicit list of deferred follow-up
      specs.
- [ ] Audit `index.ts` / `server.ts` for accidental internals.
- [ ] Lib package adds its own vitest suite with at least one test for each
      migrated client and the provider hooks.
- [ ] CI `pnpm -r test` covers both packages.

---

## Behavior Coverage (phases 1–6)

| Scenario | Layer | Covered in Phase |
|---|---|---|
| Lib package is discoverable as a workspace dependency | Workspace | 1 |
| Lib is consumed in source mode | Workspace | 1 |
| Tailwind v4 sees lib sources | Workspace | 1 |
| Test runner covers both packages | Workspace | (deferred to 9) |
| `index.ts` does not leak internals | API | 1–6 (current surface is intentional; audit in phase 9) |
| Server-only code is reachable only via `/server` | API | 4 |
| Next-auth augmentation is opt-in via side-effect import | API | 4 |
| Initial sign-in populates the session | Auth | 4 |
| Refresh-token flow extends an expiring session | Auth | 4 |
| Refresh-token failure surfaces RefreshTokenError | Auth | 4 |
| Profile without a roles claim defaults to an empty list | Auth | 4 |
| Bearer token is attached from the session | Proxy | 4 |
| Headers Content-Type and Accept are forwarded | Proxy | 4 |
| Query parameters are forwarded | Proxy | 4 |
| Non-GET/HEAD methods forward the body | Proxy | 4 |
| GET requests do not forward a body | Proxy | 4 |
| Successful logout redirects to OIDC end-session | Logout | 4 |
| Logout falls back to /login when OIDC discovery fails | Logout | 4 |
| Cookie attributes match HTTP vs. HTTPS | Logout | 4 |
| Lib pages render in the active app language | Translations | 3 |
| Missing translation provider throws | Translations | 3 |
| Default client routes to the OE proxy paths | API client | 6 |
| Replacement client is used exactly as provided | API client | 6 |
| Missing api-client provider throws | API client | 6 |
| All remaining page-/normalization-/parity-/meta-related scenarios | UI | **deferred to phases 7–9** |
