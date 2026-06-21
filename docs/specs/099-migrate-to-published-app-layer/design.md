# Design: Migrate frontend to published @open-elements/nextjs-app-layer

## GitHub Issue

[#20](https://github.com/OpenElementsLabs/open-crm/issues/20)

## Summary

`@open-elements/nextjs-app-layer@0.1.0` is published on npm and lives in its
own repository at `OpenElementsLabs/nextjs-app-layer`. Open CRM currently
consumes it via pnpm `workspace:*` against the in-repo copy under
`frontend/packages/nextjs-app-layer/`. This spec completes the phase-3
transition planned in spec 098: switch Open CRM to the published npm
version, delete the in-repo package, and drop the workspace setup.

The migration is a drop-in: import paths and runtime behavior do not change.
Consumption stays in **source mode** (Open CRM transpiles the lib's `src/`
TypeScript itself via Next.js' `transpilePackages`), matching how
`@open-elements/ui` is already wired.

## Goals

- Remove the in-repo lib copy and the workspace-only setup.
- Pin Open CRM to `@open-elements/nextjs-app-layer@^0.1.0` from npm.
- Keep behavior, UI, and tests identical before and after the migration.
- Keep the Coolify Docker build green — no references to the removed
  `packages/` directory remain.

## Non-goals

- Switching consumption mode from source (`src/`) to built `dist/`. The lib
  publishes both; switching is a possible later optimisation but out of
  scope here because it would change how `transpilePackages` and the
  Tailwind source glob are wired, and would expand the diff beyond a
  pure drop-in.
- Bumping past 0.1.0. Future versions are released independently from the
  lib repo; Open CRM picks them up by normal version bumps.
- Adding features or behavior changes.
- Touching `@open-elements/ui` consumption.

## Technical approach

### 1. Dependency switch

`frontend/package.json`:

```diff
-"@open-elements/nextjs-app-layer": "workspace:*",
+"@open-elements/nextjs-app-layer": "^0.1.0",
```

`pnpm install` then resolves the dependency from npm and removes the
workspace symlink under `node_modules/`.

### 2. Workspace teardown

After the switch, `frontend/packages/` contains only the obsolete copy of
the lib. The workspace itself has no remaining members beyond the root.

- Delete `frontend/packages/nextjs-app-layer/` (the directory and all its
  contents).
- Delete `frontend/pnpm-workspace.yaml`. With no workspace members left,
  the file serves no purpose; keeping it would only signal the wrong
  expectation to contributors.

After this step, `frontend/` is a plain single-package pnpm project again,
exactly like it was before spec 098.

### 3. Tailwind source glob

`frontend/src/app/globals.css` currently has:

```css
@source "../../node_modules/@open-elements/ui/src";
@source "../../packages/nextjs-app-layer/src/**/*.{ts,tsx}";
```

The second line points into the workspace copy. After the migration the
glob targets the published-but-source-mode copy under `node_modules`,
analog to how `@open-elements/ui` is wired today:

```css
@source "../../node_modules/@open-elements/ui/src";
@source "../../node_modules/@open-elements/nextjs-app-layer/src";
```

### 4. `transpilePackages`

`frontend/next.config.ts` stays:

```ts
transpilePackages: ["@open-elements/ui", "@open-elements/nextjs-app-layer"],
```

Both packages are consumed in source mode from `node_modules`, so Next.js
must transpile their TypeScript. No change needed.

### 5. Verification

After the changes:

1. `pnpm install` (verifies the npm resolution + node_modules layout).
2. `pnpm build` (verifies Next.js can resolve the package, Tailwind sees
   its classes, and `transpilePackages` works against the npm-installed
   source).
3. `pnpm test` (verifies the existing app tests still pass — the lib's
   own tests now live in the lib's repo and run there).
4. `grep -r "packages/nextjs-app-layer" frontend/ -- :!node_modules`
   returns nothing.
5. Manual browser smoke test of: audit-logs, users, status, token,
   api-keys, webhooks, login, forbidden, plus a non-admin page
   (companies) to confirm the regular app flow is unaffected.
6. `.next/server/middleware-manifest.json` still contains the canonical
   matcher with `_next/static` / `_next/image` exclusions (regression
   guard for the production fix in `e89b66f`).

### 6. Coolify deployment

The `frontend/Dockerfile` has two relevant steps:

- `deps` stage: `pnpm install --frozen-lockfile` against the lockfile in
  the build context. After the migration, `pnpm-lock.yaml` no longer
  references a workspace `link:` entry for `nextjs-app-layer` — it
  contains a regular npm resolution. The deps stage installs from npm
  without needing the deleted `packages/`.
- `build` stage: `COPY . .` then `pnpm build`. With `packages/` deleted
  and `pnpm-workspace.yaml` removed, there is nothing to copy that
  references the workspace. The build path is identical to pre-spec-098
  Open CRM.

No `Dockerfile` changes are needed.

### 7. Rollback

If a regression surfaces, the migration is fully revertable via `git revert`:
the in-repo `packages/nextjs-app-layer/` returns, `pnpm-workspace.yaml`
returns, and `package.json` points back to `workspace:*`. Because the
public surface of `@open-elements/nextjs-app-layer@0.1.0` is byte-equivalent
to the workspace copy at the time of publishing, behavior under either
mode is identical, so the only risk is the wiring itself.

## Key flows

### Resolution chain after the migration

```mermaid
flowchart LR
  app[frontend/src/...] --> nm[node_modules/@open-elements/nextjs-app-layer]
  nm --> npm[npm registry @open-elements/nextjs-app-layer@0.1.0]
  nm --> src[node_modules/.../src/**/*.tsx]
  src --> transpile[Next.js transpilePackages]
  transpile --> bundle[Next.js bundle]
  src --> tailwind[Tailwind @source glob in globals.css]
  tailwind --> css[generated CSS]
```

The workspace symlink path is gone; everything resolves through
`node_modules` and the npm registry.

## Dependencies

- `@open-elements/nextjs-app-layer@^0.1.0` (published on npm).
- No other version bumps. `@open-elements/ui` stays at `^0.6.0`.

## Security considerations

None new. The migration changes the **source** of the lib (workspace
symlink → npm registry tarball) but not its **content**. The published
0.1.0 was extracted 1:1 from the workspace at the time of publish, so the
auth, proxy, role guards, and cookie handling are unchanged.

The lockfile (`pnpm-lock.yaml`) will gain a registry integrity hash for
the npm tarball that did not exist for the workspace `link:`. This is the
normal npm supply-chain posture and is the same trust model already
applied to `@open-elements/ui`.

## Open questions

None.
