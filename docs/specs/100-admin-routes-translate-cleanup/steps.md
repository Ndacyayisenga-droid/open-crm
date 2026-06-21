# Implementation Steps: Admin routes + translate cleanup

Three structural cleanups bundled into one wave of releases:

1. Move `/api-keys` and `/webhooks` under `/admin/*` (consistent with the
   IT-ADMIN role guard).
2. Replace local translate components with `@open-elements/ui@^0.8.0`'s
   `TranslateButton` and `TranslateDialog`.
3. Delete pass-through wrapper files in `frontend/src/` left over from
   spec 098; importers point directly at `@open-elements/nextjs-app-layer`.

## Step 1: Bump library dependencies

- [x] `frontend/package.json`:
  - `@open-elements/ui`: `^0.6.0` → `^0.8.0` (TranslateButton + TranslateDialog).
  - `@open-elements/nextjs-app-layer`: `^0.1.0` → `^0.2.0` (admin defaultRoute).
- [x] `pnpm install` regenerates the lockfile against the new versions.

**Related behaviors:** Both libs are on the new releases; pnpm-lock.yaml
reflects the matching resolutions.

## Step 2: Move admin pages under `/admin/`

- [x] Move `frontend/src/app/(app)/api-keys/page.tsx` → `frontend/src/app/(app)/admin/api-keys/page.tsx`.
- [x] Move `frontend/src/app/(app)/webhooks/page.tsx` → `frontend/src/app/(app)/admin/webhooks/page.tsx`.
- [x] Delete the now-empty `api-keys/` and `webhooks/` directories.

**Related behaviors:** `/admin/api-keys` and `/admin/webhooks` render the page;
old `/api-keys` and `/webhooks` URLs return 404.

## Step 3: Update sidebar links and active-route matching

- [x] In `frontend/src/app/(app)/layout.tsx` (`CrmSidebar`):
  - `NavItem href="/api-keys"` → `href="/admin/api-keys"`.
  - `NavItem href="/webhooks"` → `href="/admin/webhooks"`.
  - Update `active` matchers accordingly.
  - Simplify `ADMIN_PREFIXES` from `["/admin", "/api-keys", "/webhooks"]`
    to just `["/admin"]`.

**Related behaviors:** Sidebar links point to the new URLs; Admin
sub-menu auto-opens on any admin route.

## Step 4: Delete pass-through wrapper files

- [x] Delete:
  - `frontend/src/components/session-provider.tsx`
  - `frontend/src/components/forbidden-page.tsx`
  - `frontend/src/components/add-comment-dialog.tsx`
  - `frontend/src/lib/roles.ts`
  - `frontend/src/lib/forbidden-error.ts`
- [x] Delete their tests:
  - `frontend/src/components/__tests__/forbidden-page.test.tsx`
  - `frontend/src/lib/__tests__/roles.test.ts`
  - `frontend/src/lib/__tests__/forbidden-error.test.ts`
- [x] Update every importer to point directly at
  `@open-elements/nextjs-app-layer`.

**Related behaviors:** Pass-through wrapper files are deleted; no
consumer references a deleted path.

## Step 5: Replace local translate components with ui-lib

- [x] Delete `frontend/src/components/translate-dialog.tsx`.
- [x] Delete `frontend/src/components/translate-button.tsx`.
- [x] Delete `frontend/src/components/__tests__/translate-button.test.tsx`
      (the new lib carries equivalent tests).
- [x] Update the five usage sites (`company-detail.tsx`,
      `contact-detail.tsx`, `company-comments.tsx`, `contact-comments.tsx`,
      `task-comments.tsx`) to:
  - Import `TranslateButton` from `@open-elements/ui`.
  - Import `useTranslationConfig` from `@/lib/use-translation-config`
    (the hook stays in the app — drift acknowledged in design § Open
    questions).
  - Import `translateText` from `@/lib/api`.
  - Pass `configured`, `onTranslate`, and the `translations` bundle as
    props.

**Related behaviors:** Local translate files are deleted; all translate
usages import from the ui-lib; translate button still appears when
configured, hides when not, opens dialog on click.

## Step 6: Verify and tidy

- [x] `pnpm install && pnpm --filter open-crm-frontend build` exits 0.
- [x] `pnpm --filter open-crm-frontend test` reports the same pass count
      as before (minus the deleted wrapper-tests).
- [x] `grep -r "@/components/session-provider\|@/components/forbidden-page\|@/components/add-comment-dialog\|@/components/translate-button\|@/components/translate-dialog\|@/lib/roles\|@/lib/forbidden-error" frontend/src` returns zero.
- [x] `grep -r "translate-button\|translate-dialog" frontend/src` returns zero.
- [x] `.next/server/middleware-manifest.json` matcher still contains the
      canonical `_next/static` / `_next/image` / `login` / `api/auth` /
      `api/logout` / asset-extension exclusions (regression guard for
      `e89b66f`).

## Step 7: INDEX done + push + PR

- [x] `specs/INDEX.md` for spec 100 → `done`.
- [x] Push branch; open PR referencing #22 (`Closes #22`).
- [x] CI will be red until the two lib PRs are merged and published.
      Once `@open-elements/ui@0.8.0` and `@open-elements/nextjs-app-layer@0.2.0`
      reach npm, re-run CI.

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|---|---|---|
| `apiKeysPageMeta.defaultRoute === "/admin/api-keys"` | lib-side | sibling PR in nextjs-app-layer |
| `webhooksPageMeta.defaultRoute === "/admin/webhooks"` | lib-side | sibling PR in nextjs-app-layer |
| `/admin/api-keys` and `/admin/webhooks` render | App routing | 2 |
| Old `/api-keys` and `/webhooks` 404 | App routing | 2 (by deletion) |
| Sidebar links point to new URLs | App nav | 3 |
| Admin sub-menu auto-opens | App nav | 3 (`ADMIN_PREFIXES`) |
| Role-gating unchanged | App routing | 2 (factories unchanged) |
| Pass-through wrappers deleted | Cleanup | 4 |
| No consumer references deleted paths | Cleanup | 4 + 6 (grep) |
| Symbols reachable from lib | Cleanup | 4 (import re-points) |
| Local translate files deleted | Cleanup | 5 |
| Translate usages import from ui-lib | Cleanup | 5 |
| Translate button appears when configured | Feature | 5 (prop wiring) |
| Translate button hidden when not configured | Feature | 5 (prop wiring) |
| Clicking opens dialog | Feature | sibling PR in open-elements-ui |
| Both libs on new releases | Deps | 1 |
| Build + test green | CI | 6 (after lib releases) |
| Middleware matcher preserved | Build | 6 |
| Static assets reach browser unchanged | Runtime | indirect (no middleware change) |
