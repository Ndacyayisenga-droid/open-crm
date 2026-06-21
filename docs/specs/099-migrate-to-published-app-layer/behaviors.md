# Behaviors: Migrate frontend to published @open-elements/nextjs-app-layer

## Dependency resolution

### `pnpm install` resolves the lib from npm

- **Given** `frontend/package.json` lists `"@open-elements/nextjs-app-layer": "^0.1.0"`
- **And** `frontend/pnpm-workspace.yaml` is removed
- **And** `frontend/packages/nextjs-app-layer/` is removed
- **When** `pnpm install` runs in `frontend/`
- **Then** `frontend/node_modules/@open-elements/nextjs-app-layer/package.json` exists
- **And** its `version` field is `0.1.0` (or any later published version matching `^0.1.0`)
- **And** the directory is a regular npm install, not a workspace symlink

### Lockfile points at the npm registry

- **Given** the migrated state
- **When** the user inspects `frontend/pnpm-lock.yaml`
- **Then** the entry for `@open-elements/nextjs-app-layer` references the npm registry resolution (`registry.npmjs.org`)
- **And** the entry includes an `integrity` hash
- **And** there is no `link:` reference pointing into the (now-deleted) `packages/` directory

## Workspace teardown

### Workspace file is removed

- **When** the migration is complete
- **Then** `frontend/pnpm-workspace.yaml` does not exist

### In-repo lib copy is removed

- **When** the migration is complete
- **Then** `frontend/packages/` does not exist
- **And** `grep -r "packages/nextjs-app-layer" frontend/` (excluding `node_modules`) returns no matches

## Build pipeline

### Next.js build succeeds

- **Given** the migrated state
- **When** `pnpm build` runs
- **Then** the command exits 0
- **And** the build emits `.next/standalone/` (the standalone-output mode is unchanged)

### Tailwind sees lib classes via `node_modules`

- **Given** `globals.css` declares `@source "../../node_modules/@open-elements/nextjs-app-layer/src"`
- **And** a lib component uses a Tailwind utility class that no app file uses (e.g. a unique combination only present in `BearerTokenCard`)
- **When** `pnpm build` runs
- **Then** the class is present in the production CSS bundle

### Middleware matcher is preserved

- **Given** `frontend/src/middleware.ts` still declares the static `config` literal (from fix `e89b66f`)
- **When** `pnpm build` runs
- **Then** `.next/server/middleware-manifest.json` contains a matcher whose `originalSource` includes the `_next/static`, `_next/image`, `login`, `api/auth`, `api/logout` and asset-extension exclusions

### `transpilePackages` still applies

- **Given** `next.config.ts` lists `"@open-elements/nextjs-app-layer"` in `transpilePackages`
- **When** the build runs against the npm-installed source
- **Then** TypeScript files under `node_modules/@open-elements/nextjs-app-layer/src/**` are transpiled by Next.js (no separate `tsc` step needed)

## Tests

### App test suite passes

- **When** `pnpm test` runs from `frontend/`
- **Then** all existing app tests pass
- **And** no test references the deleted `packages/` directory

### No regression in test count

- **Given** the test suite size before this migration was `N`
- **When** `pnpm test` runs after the migration
- **Then** the suite reports the same `N` tests (no app test was deleted as part of the migration; lib tests live in the lib's own repo and are not part of Open CRM's count)

## Functional parity in the browser

### Authenticated IT-ADMIN sees admin pages

- **Given** the migrated app is deployed and the user signs in with `IT-ADMIN`
- **When** they navigate to each of `/admin/audit-logs`, `/admin/users`, `/admin/status`, `/admin/token`, `/api-keys`, `/webhooks`
- **Then** every page renders, fetches its data, paginates, and shows the same UI as before the migration

### Non-admin sees Forbidden

- **Given** the user signs in without `IT-ADMIN`
- **When** they navigate to any admin path
- **Then** `ForbiddenPage` is rendered server-side
- **And** the "Back to home" button links to the configured `homeRoute`

### Unauthenticated request redirects to login

- **Given** no session cookie
- **When** the user navigates to any protected page
- **Then** the middleware redirects to `/login`
- **And** `/login` renders the OE-branded sign-in screen unchanged

### Static assets are not routed through the middleware

- **Given** the deployed migrated app
- **When** the browser requests `/_next/static/...` or `/_next/image/...`
- **Then** the response is the actual asset (CSS, JS chunk, image)
- **And** the response is **not** the `/login` HTML
- **And** the browser console shows no "Unexpected token '<'" or MIME-type errors

### Companies / contacts / updates flows are unaffected

- **Given** the migrated app
- **When** the user navigates to `/companies`, `/contacts`, `/updates`, `/tags`
- **Then** every flow behaves identically to before the migration

## Coolify deployment

### Dockerfile builds without workspace inputs

- **Given** `frontend/Dockerfile` is unchanged
- **And** `frontend/packages/` and `frontend/pnpm-workspace.yaml` no longer exist in the build context
- **When** Coolify builds the image
- **Then** the `deps` stage's `pnpm install --frozen-lockfile` resolves `@open-elements/nextjs-app-layer` from the npm registry
- **And** the `build` stage's `pnpm build` succeeds
- **And** the resulting standalone output runs identically to a build from before the migration
