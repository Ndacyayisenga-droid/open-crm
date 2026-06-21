# Implementation Steps: Extract @open-elements/nextjs-app-layer

The full 9-phase migration from `design.md` § "Key flows — Migration order".
Phases 1–6 landed in PR #18 (workspace + foundation). Phases 7–9 land in the
follow-up PR (this branch).

## Phase 1 — Workspace skeleton

- [x] `frontend/pnpm-workspace.yaml` lists `.` and `packages/*`.
- [x] `nextjs-app-layer/package.json` declares `@open-elements/nextjs-app-layer`,
      `exports` `.`, `/server`, `/server/next-auth-types`, `/layout`.
- [x] `tsconfig.json` and `vitest.config.ts` mirror the app setup.
- [x] `frontend/package.json` adds the `workspace:*` dependency.
- [x] `next.config.ts` lists the lib in `transpilePackages`.
- [x] `globals.css` adds `@source` for the lib sources.

## Phase 2 — Pure utilities + DTOs

- [x] `lib/roles.ts`, `lib/forbidden-error.ts`, `api/types.ts` own the
      cross-cutting primitives. App-side files become re-exports.

## Phase 3 — Translation provider

- [x] `translations/{de,en}.ts` carry the lib-owned strings.
- [x] `AppLayerTranslationProvider` reads from `useLanguage()`.
- [x] `useAppLayerTranslations` hook throws when used outside the provider.

## Phase 4 — Server factories

- [x] `createAppLayerAuth`, `createBackendProxyHandler`, `createLogoutHandler`,
      `middlewareConfig`, side-effect `next-auth-types` module.

## Phase 5 — Shared components

- [x] `SessionProvider`, `ForbiddenPage` (with optional `homeRoute` prop),
      `BearerTokenCard`, `AddCommentDialog`.

## Phase 6 — API client

- [x] `AppLayerApiClient` interface + `defaultApiClient` + `ApiClientProvider`
      + `useApiClient`.

## Phase 7 — Page migrations

- [x] `createAuditLogsPage({auth, homeRoute})` + `AuditLogsClient` + `auditLogsPageMeta`.
- [x] `createUsersPage` + `UsersClient` + `usersPageMeta`.
- [x] `createServerStatusPage` + `ServerStatusClient` + `serverStatusPageMeta`.
- [x] `createBearerTokenPage` + `BearerTokenClient` + `bearerTokenPageMeta`.
- [x] `createApiKeysPage` + `ApiKeysClient` + `apiKeysPageMeta` — **normalized**
      to `Table` + `AlertCircle` (matching audit-logs / users).
- [x] `createWebhooksPage` + `WebhooksClient` + `webhooksPageMeta` —
      **normalized** to `Table` + `AlertCircle`.
- [x] `createLoginPage({homeRoute})` + `LoginClient` (Open CRM passes
      `homeRoute="/updates"`).
- [x] App `page.tsx` files reduced to 2-line `createXyzPage({auth})`
      re-exports.
- [x] Original app-side client files and component tests deleted.

## Phase 8 — OERootLayout

- [x] `OERootLayout` ships from `@open-elements/nextjs-app-layer/layout`,
      wrapping `<html>` + Montserrat/Lato + `SessionProvider` +
      `LanguageProvider` + `AppLayerTranslationProvider` + `ApiClientProvider`.
- [x] App's `app/layout.tsx` shrinks to ~12 lines (metadata + `<OERootLayout>`).
- [x] Subpath export keeps `next/font/google` out of the main client-safe
      barrel so app tests do not need to mock it.

## Phase 9 — Lib tests + README audit + INDEX done

- [x] Lib vitest suite covers `roles`, `ForbiddenError`,
      `AppLayerTranslationProvider`, `ApiClientProvider`, `ForbiddenPage`,
      `AuditLogsClient`, `UsersClient` (loading / empty / error / data).
- [x] Shared `renderWithLibProviders` test helper in
      `src/test/render-with-providers.tsx`.
- [x] localStorage shim in the lib test setup so client-component tests
      that read `pageSize.*` keys do not blow up in jsdom 29.
- [x] App `test-utils.tsx` keeps wrapping with `AppLayerTranslationProvider`.
- [x] README documents the four entry points (`.`, `/server`,
      `/server/next-auth-types`, `/layout`), wiring excerpts, OE
      conventions, and deferred follow-up specs.
- [x] `INDEX.md` flipped to `done`.

---

## Behavior coverage

| Scenario | Layer | Covered in Phase |
|---|---|---|
| Lib package is discoverable as a workspace dependency | Workspace | 1 |
| Lib is consumed in source mode | Workspace | 1 |
| Tailwind v4 sees lib sources | Workspace | 1 |
| Test runner covers both packages | Workspace | 9 (`pnpm -r test`) |
| `index.ts` does not leak internals | API | 9 (audited; `OERootLayout` moved to subpath) |
| Server-only code is reachable only via `/server` | API | 4 |
| Next-auth augmentation is opt-in via side-effect import | API | 4 |
| Initial sign-in populates the session | Auth | 4 |
| Refresh-token flow extends an expiring session | Auth | 4 |
| Refresh-token failure surfaces RefreshTokenError | Auth | 4 (+ SessionProvider in 5) |
| Profile without a roles claim defaults to an empty list | Auth | 4 |
| Bearer token attached from session | Proxy | 4 |
| Content-Type / Accept headers forwarded | Proxy | 4 |
| Query params forwarded | Proxy | 4 |
| Non-GET/HEAD body forwarded | Proxy | 4 |
| GET requests have no body | Proxy | 4 |
| Logout redirects to OIDC end-session | Logout | 4 |
| Logout falls back to /login on discovery failure | Logout | 4 |
| Cookie attributes match HTTP vs. HTTPS | Logout | 4 |
| Lib pages render in active app language | Translations | 3 + 9 (test) |
| Missing translation provider throws | Translations | 3 + 9 (test) |
| Default API client routes to proxy paths | API client | 6 + 9 (test) |
| Replacement API client used exactly as provided | API client | 6 + 9 (test) |
| Missing api-client provider throws | API client | 6 + 9 (test) |
| IT-ADMIN sees admin pages | Roles | 7 (factory guard) |
| Non-IT-ADMIN sees ForbiddenPage | Roles | 7 (factory guard) |
| Unauthenticated request redirects to /login | Middleware | 4 (middleware matcher) |
| Page meta carries defaultRoute, icon, label, requiredRole | Page meta | 7 |
| Audit-logs page parity (fetch / empty / error / filters / pagination) | UI | 7a + 9 (test) |
| Users page parity (data / avatar fallback / loading / error) | UI | 7a + 9 (test) |
| API-keys page parity + normalization | UI | 7c |
| Webhooks page parity + normalization | UI | 7d |
| Server-status page parity | UI | 7b |
| Bearer-token page parity | UI | 7b |
| Login page parity (redirect home / error / sign-in) | UI | 7e |
| ForbiddenPage parity | UI | 5 + 9 (test) |
| Migration verification: app page.tsx is 2-line re-export | Cleanup | 7 |
| Migration verification: original files deleted | Cleanup | 7 |
| CI green | CI | 9 |
