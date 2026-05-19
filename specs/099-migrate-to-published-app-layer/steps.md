# Implementation Steps: Migrate frontend to published @open-elements/nextjs-app-layer

Drop-in switch from the in-repo workspace copy of the lib to the published
npm package. The lib's content does not change, only the *source* of the
package changes from a workspace symlink to a registry install.

## Step 1: Switch dependency to npm

- [ ] `frontend/package.json`: replace `"@open-elements/nextjs-app-layer": "workspace:*"`
      with `"@open-elements/nextjs-app-layer": "^0.1.0"`.

**Acceptance criteria:**

- [ ] `package.json` parses; no other deps changed.

**Related behaviors:** `pnpm install` resolves the lib from npm.

## Step 2: Remove the in-repo lib copy and the workspace file

- [ ] Delete `frontend/packages/nextjs-app-layer/` (the entire directory).
- [ ] Delete `frontend/pnpm-workspace.yaml` (no remaining workspace members).

**Acceptance criteria:**

- [ ] `ls frontend/packages` returns "No such file or directory".
- [ ] `ls frontend/pnpm-workspace.yaml` returns "No such file or directory".

**Related behaviors:** Workspace file is removed; In-repo lib copy is removed.

## Step 3: Update the Tailwind `@source` glob

- [ ] In `frontend/src/app/globals.css`, replace the line
      `@source "../../packages/nextjs-app-layer/src/**/*.{ts,tsx}";`
      with `@source "../../node_modules/@open-elements/nextjs-app-layer/src";`
      (matching how `@open-elements/ui` is wired).

**Acceptance criteria:**

- [ ] The glob targets the npm-installed source under `node_modules`.

**Related behaviors:** Tailwind sees lib classes via `node_modules`.

## Step 4: `pnpm install` resolves the npm package

- [ ] Run `pnpm install` (regenerates the lockfile entry).
- [ ] Verify `node_modules/@open-elements/nextjs-app-layer/package.json` is a
      regular npm install (no symlink), and `pnpm-lock.yaml` references the
      npm registry with an integrity hash.

**Acceptance criteria:**

- [ ] `frontend/node_modules/@open-elements/nextjs-app-layer/package.json`
      shows `version: "0.1.0"`.
- [ ] No `link:` entries point into `packages/` in `pnpm-lock.yaml`.

**Related behaviors:** `pnpm install` resolves the lib from npm; Lockfile
points at the npm registry.

## Step 5: Verify the build

- [ ] Run `pnpm --filter open-crm-frontend build`.
- [ ] Spot-check `.next/server/middleware-manifest.json` for the canonical
      matcher (the spec-098 production fix).
- [ ] `grep -r "packages/nextjs-app-layer" frontend/` (excluding
      `node_modules`) returns nothing.

**Acceptance criteria:**

- [ ] Build exits 0.
- [ ] `.next/standalone/` is emitted.
- [ ] Middleware matcher includes `_next/static`, `_next/image`, `login`,
      `api/auth`, `api/logout`, asset extensions.

**Related behaviors:** Next.js build succeeds; Middleware matcher is
preserved; `transpilePackages` still applies.

## Step 6: Run the test suite

- [ ] Run `pnpm --filter open-crm-frontend test`.

**Acceptance criteria:**

- [ ] All previously passing tests still pass (no new failures introduced
      by the migration).

**Related behaviors:** App test suite passes; No regression in test count.

## Step 7: Flip INDEX to `done` and open the PR

- [ ] `specs/INDEX.md` for spec 099 → `done`.
- [ ] Push branch; open PR referencing #20 ("Closes #20").
- [ ] Watch CI (backend / frontend / docker).

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|---|---|---|
| `pnpm install` resolves the lib from npm | Build | 1, 4 |
| Lockfile points at the npm registry | Build | 4 |
| Workspace file is removed | Cleanup | 2 |
| In-repo lib copy is removed | Cleanup | 2 |
| Next.js build succeeds | Build | 5 |
| Tailwind sees lib classes via `node_modules` | Build | 3, 5 |
| Middleware matcher is preserved | Build | 5 |
| `transpilePackages` still applies | Build | 5 |
| App test suite passes | Tests | 6 |
| No regression in test count | Tests | 6 |
| Authenticated IT-ADMIN sees admin pages | Runtime parity | indirect (lib content unchanged; verified via build + manual smoke) |
| Non-admin sees Forbidden | Runtime parity | indirect |
| Unauthenticated request redirects to login | Runtime parity | indirect |
| Static assets are not routed through the middleware | Runtime parity | 5 (middleware-manifest check) |
| Companies / contacts / updates flows are unaffected | Runtime parity | indirect |
| Dockerfile builds without workspace inputs | CI | 7 (CI run) |
