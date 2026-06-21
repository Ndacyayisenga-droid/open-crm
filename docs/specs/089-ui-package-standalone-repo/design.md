# Design: Extract @open-elements/ui into Standalone Repository

## GitHub Issue

—

## Summary

The `@open-elements/ui` package currently lives as a workspace package inside the Open CRM monorepo at
`frontend/packages/ui/`. This spec describes how to extract it into its own GitHub repository
(`open-elements/open-elements-ui`), add a proper build pipeline (TypeScript compilation + CSS bundling),
publish it to npm as `@open-elements/ui`, and update the CRM (and future apps) to consume the published
package instead of the workspace-local source.

## Goals

- Create a standalone GitHub repository for `@open-elements/ui`
- Add a build step that compiles `.tsx` source to `.js` + `.d.ts` + bundled CSS
- Publish to npm under the `@open-elements` scope
- Establish CI/CD with automated testing, linting, and npm publishing on release
- Update the CRM frontend to depend on the published npm package
- Ensure the migration is seamless — no import path changes in consuming apps

## Non-goals

- Changing component behavior or APIs during the extraction
- Adding new components as part of this migration
- Supporting additional languages beyond DE/EN
- Creating a documentation site or Storybook (deferred)

---

## Step-by-Step Implementation Plan

### Phase 1: Prepare the Standalone Repository

#### Step 1.1 — Create the GitHub Repository

1. Create `open-elements/open-elements-ui` on GitHub (public repository)
2. Initialize with:
    - `main` as default branch
    - Branch protection: require PR reviews, require CI to pass
    - Enable GitHub Actions
3. Add standard Open Elements repository files:
    - `LICENSE` (Apache 2.0 or the license used by Open Elements)
    - `CODE_OF_CONDUCT.md`
    - `CONTRIBUTING.md`
    - `.editorconfig` (from Open Elements template)
    - `README.md` with package description, install instructions, usage examples

#### Step 1.2 — Copy Package Source

Copy the contents of `frontend/packages/ui/` into the new repository root:

```
open-elements-ui/
├── src/
│   ├── components/          # All 25+ components
│   │   ├── __tests__/       # Component tests
│   │   ├── button.tsx
│   │   ├── sidebar.tsx
│   │   └── ...
│   ├── i18n/
│   │   ├── language-context.tsx
│   │   ├── de.ts
│   │   └── en.ts
│   ├── lib/
│   │   ├── __tests__/
│   │   └── utils.ts
│   ├── styles/
│   │   └── brand.css
│   ├── types/
│   │   └── index.ts
│   ├── test/
│   │   └── setup.ts
│   └── index.ts             # Barrel export
├── package.json
├── tsconfig.json
├── tsconfig.build.json      # NEW — build-specific config (tsc output)
├── vitest.config.ts
├── eslint.config.mjs
├── .prettierrc
├── .gitignore
├── .github/
│   └── workflows/
│       ├── ci.yml           # NEW — test + lint on PR
│       └── release.yml      # NEW — publish to npm on release
├── LICENSE
├── README.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
└── .editorconfig
```

#### Step 1.3 — Update .gitignore

```gitignore
node_modules/
dist/
*.tsbuildinfo
.env
```

---

### Phase 2: Add Build Pipeline

The package currently ships raw `.tsx` source. For npm distribution, it needs to compile to JavaScript
with type declarations and bundle the CSS.

#### Step 2.1 — Build Tool: Plain `tsc` + CSS Copy

No additional build tool dependency needed. TypeScript compiles `.tsx` → `.js` + `.d.ts`, and a
simple `cp` command copies the CSS file into `dist/`.

**Rationale:** The package has only one CSS file (`brand.css`), so a full bundler like tsup or
unbuild adds unnecessary complexity. `tsc` is already a dev dependency, produces one `.js` file per
source file (better tree-shaking in consumers), and the output mirrors the source structure 1:1
(easier to debug).

#### Step 2.2 — Add Build-specific TypeScript Config

Create `tsconfig.build.json`:

```json
{
    "extends": "./tsconfig.json",
    "compilerOptions": {
        "outDir": "./dist",
        "declaration": true,
        "declarationMap": true,
        "sourceMap": true,
        "noEmit": false,
        "jsx": "react-jsx"
    },
    "include": [
        "src/**/*.ts",
        "src/**/*.tsx"
    ],
    "exclude": [
        "src/**/*.test.ts",
        "src/**/*.test.tsx",
        "src/test/**"
    ]
}
```

This extends the existing `tsconfig.json` and overrides only what's needed for the build output.
Tests are excluded from compilation.

#### Step 2.3 — Update package.json for Publishing

```json
{
    "name": "@open-elements/ui",
    "version": "0.1.0",
    "description": "Shared UI component library for Open Elements applications",
    "license": "Apache-2.0",
    "repository": {
        "type": "git",
        "url": "https://github.com/open-elements/open-elements-ui.git"
    },
    "type": "module",
    "main": "./dist/index.js",
    "module": "./dist/index.js",
    "types": "./dist/index.d.ts",
    "exports": {
        ".": {
            "import": "./dist/index.js",
            "types": "./dist/index.d.ts"
        },
        "./styles/brand.css": "./dist/brand.css"
    },
    "files": [
        "dist",
        "src"
    ],
    "sideEffects": [
        "**/*.css"
    ],
    "scripts": {
        "build": "tsc -p tsconfig.build.json && pnpm run build:css",
        "build:css": "cp src/styles/brand.css dist/brand.css",
        "clean": "rm -rf dist",
        "prebuild": "pnpm run clean",
        "dev": "tsc -p tsconfig.build.json --watch",
        "test": "vitest run",
        "test:watch": "vitest",
        "lint": "eslint src/",
        "format": "prettier --write src/",
        "format:check": "prettier --check src/",
        "typecheck": "tsc --noEmit",
        "prepublishOnly": "pnpm run build"
    },
    "peerDependencies": {
        "react": "^19.0.0",
        "react-dom": "^19.0.0",
        "@base-ui/react": "^1.3.0",
        "radix-ui": "^1.4.0",
        "lucide-react": "^0.500.0",
        "class-variance-authority": "^0.7.0",
        "clsx": "^2.1.0",
        "tailwind-merge": "^3.0.0",
        "react-day-picker": "^9.14.0",
        "date-fns": "^4.1.0"
    },
    "devDependencies": {
        "@types/react": "^19.0.0",
        "@types/react-dom": "^19.0.0",
        "typescript": "^5.0.0",
        "@vitejs/plugin-react": "^4.0.0",
        "vitest": "^3.0.0",
        "jsdom": "^26.0.0",
        "@testing-library/react": "^16.0.0",
        "@testing-library/jest-dom": "^6.0.0",
        "eslint": "^9.0.0",
        "@eslint/js": "^9.0.0",
        "prettier": "^3.0.0",
        "react": "^19.0.0",
        "react-dom": "^19.0.0",
        "@base-ui/react": "^1.3.0",
        "radix-ui": "^1.4.0",
        "lucide-react": "^0.500.0",
        "class-variance-authority": "^0.7.0",
        "clsx": "^2.1.0",
        "tailwind-merge": "^3.0.0",
        "react-day-picker": "^9.14.0",
        "date-fns": "^4.1.0"
    }
}
```

**Important: `@types/react` and `@types/react-dom`** are required as devDependencies even though
React 19 ships its own types. When `tsc` runs standalone with `moduleResolution: "bundler"`, it
cannot resolve React's built-in type exports. The `@types` packages provide the necessary type
declarations for the build to succeed.

**Build scripts explained:**

| Script      | What it does                                                    |
|-------------|-----------------------------------------------------------------|
| `prebuild`  | Cleans `dist/` before each build                                |
| `build`     | Runs `tsc` to compile `.tsx` → `.js` + `.d.ts`, then copies CSS |
| `build:css` | Copies `brand.css` into `dist/`                                 |
| `dev`       | Runs `tsc` in watch mode for local development                  |

**Key decisions:**

| Field                      | Value                 | Rationale                                         |
|----------------------------|-----------------------|---------------------------------------------------|
| `"type": "module"`         | ESM-only              | All consumers are modern Next.js apps             |
| `"files": ["dist", "src"]` | Include source        | Allows Tailwind CSS content scanning in consumers |
| `"exports"`                | Explicit entry points | Clean public API, CSS importable separately       |
| `"sideEffects"`            | CSS files             | Prevents tree-shaking from removing CSS imports   |

#### Step 2.4 — Handle CSS for Tailwind Consumers

The `brand.css` file uses Tailwind's `@theme` directive. For consumers using Tailwind v4, the CSS
must be importable as a source file. Including `src` in `"files"` enables this.

Consumers import the CSS in two ways:

**Option A — Import compiled CSS (non-Tailwind consumers):**

```css
@import "@open-elements/ui/styles/brand.css";
```

**Option B — Import source CSS for Tailwind theme extension:**

```css
/* Consumer's globals.css */
@import "@open-elements/ui/src/styles/brand.css";
@import "tailwindcss";

@source "../../node_modules/@open-elements/ui/src";
```

The `@source` directive tells Tailwind to scan the package source for utility classes.

#### Step 2.5 — Verify Build Locally

```bash
pnpm install
pnpm run build
pnpm run test
pnpm run typecheck
```

Verify the `dist/` output contains:

- `index.js` + `index.d.ts` — compiled entry point with declarations
- `components/*.js` + `components/*.d.ts` — one file per component
- `lib/utils.js` + `lib/utils.d.ts` — utility functions
- `i18n/*.js` + `i18n/*.d.ts` — translations and LanguageProvider
- `types/index.js` + `types/index.d.ts` — shared types
- `brand.css` — copied CSS file

---

### Phase 3: CI/CD with GitHub Actions

#### Step 3.1 — CI Workflow (on Pull Requests)

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
    pull_request:
        branches: [ main ]
    push:
        branches: [ main ]

jobs:
    build-and-test:
        runs-on: ubuntu-latest
        steps:
            -   uses: actions/checkout@v4

            -   uses: pnpm/action-setup@v4
                with:
                    version: 10

            -   uses: actions/setup-node@v4
                with:
                    node-version: "22"
                    cache: "pnpm"

            -   run: pnpm install --frozen-lockfile

            -   run: pnpm run typecheck
            -   run: pnpm run lint
            -   run: pnpm run format:check
            -   run: pnpm run test
            -   run: pnpm run build
```

#### Step 3.2 — Release Workflow (publish to npm)

Create `.github/workflows/release.yml`:

```yaml
name: Release

on:
    release:
        types: [ published ]

jobs:
    publish:
        runs-on: ubuntu-latest
        permissions:
            contents: read
            id-token: write
        steps:
            -   uses: actions/checkout@v4

            -   uses: pnpm/action-setup@v4
                with:
                    version: 10

            -   uses: actions/setup-node@v4
                with:
                    node-version: "22"
                    cache: "pnpm"
                    registry-url: "https://registry.npmjs.org"

            -   run: pnpm install --frozen-lockfile
            -   run: pnpm run test
            -   run: pnpm run build

            -   run: pnpm publish --access public --no-git-checks
                env:
                    NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

**Required setup:**

1. Create an npm access token with publish permissions for the `@open-elements` scope
2. Add it as `NPM_TOKEN` secret in the GitHub repository settings

#### Step 3.3 — Versioning Strategy

Use **GitHub Releases** for version management:

1. Developer creates a PR, merges to `main`
2. When ready to release, create a GitHub Release with a semver tag (e.g., `v0.2.0`)
3. Before creating the release, update `version` in `package.json` to match the tag
4. The release workflow automatically publishes to npm

**Alternative (automated):** Use [changesets](https://github.com/changesets/changesets) for automated
versioning and changelogs. This is recommended once the package has multiple contributors.

---

### Phase 4: npm Scope and Access Setup

#### Step 4.1 — npm Organization

1. Create the `@open-elements` organization on npmjs.com (if not already existing)
2. Add team members with appropriate publish permissions
3. Configure the organization for public packages (no paid plan needed for public)

#### Step 4.2 — First Publish

```bash
# Login to npm (one-time)
npm login

# Verify package contents before publishing
pnpm pack --dry-run

# Publish (or let CI handle this via GitHub Release)
pnpm publish --access public
```

#### Step 4.3 — Verify Published Package

```bash
npm info @open-elements/ui
```

---

### Phase 5: Update the CRM Frontend (Consumer)

#### Step 5.1 — Install the Published Package

In `frontend/package.json`, change:

```diff
- "@open-elements/ui": "workspace:*"
+ "@open-elements/ui": "^0.1.0"
```

Then:

```bash
pnpm install
```

#### Step 5.2 — Update CSS Imports

In `frontend/src/app/globals.css`, update the `@source` path:

```diff
  @import "@open-elements/ui/src/styles/brand.css";
  @import "tailwindcss";
- @source "../packages/ui/src";
+ @source "../../node_modules/@open-elements/ui/src";
```

The `@import` path stays the same because `src` is included in the published package's `files`.

#### Step 5.3 — Remove Workspace Package

1. Delete `frontend/packages/ui/` entirely
2. Delete or simplify `frontend/pnpm-workspace.yaml`:
    - If no other workspace packages exist, remove the file
    - If other packages exist, remove the `packages/ui` entry

#### Step 5.4 — Verify All Imports Still Work

All TypeScript imports remain unchanged:

```typescript
import {Button, cn} from "@open-elements/ui";
import type {TagDto} from "@open-elements/ui";
```

Run the full test and build to verify:

```bash
pnpm install
pnpm run build
pnpm run test
```

#### Step 5.5 — Update next.config

If the CRM's `next.config.ts` has `transpilePackages` configured for the workspace package,
verify it still works with the compiled npm package. If the package is now pre-compiled (ESM `.js`),
the transpile entry may no longer be needed. Test with and without it.

---

### Phase 6: Development Workflow After Extraction

#### Step 6.1 — Local Development with Linked Package

When developing the UI package and testing changes in the CRM simultaneously:

**Option A — pnpm link (recommended):**

```bash
# In open-elements-ui/
pnpm link --global

# In open-crm/frontend/
pnpm link --global @open-elements/ui
```

**Option B — pnpm overrides:**

In the CRM's `package.json`:

```json
{
    "pnpm": {
        "overrides": {
            "@open-elements/ui": "link:../open-elements-ui"
        }
    }
}
```

Remember to remove the override before committing.

**Option C — Re-add as workspace (temporary):**

For extended development periods, temporarily re-add the package as a workspace dependency.

#### Step 6.2 — Release Checklist

For each new version of `@open-elements/ui`:

1. Make changes in the UI package repository
2. Run tests locally: `pnpm test && pnpm build`
3. Update `version` in `package.json`
4. Commit, push, create PR, merge
5. Create GitHub Release with matching semver tag
6. CI publishes to npm automatically
7. In consuming apps: `pnpm update @open-elements/ui`

---

## Migration Checklist

- [ ] Create `open-elements/open-elements-ui` GitHub repository
- [ ] Copy `frontend/packages/ui/` contents to new repo
- [ ] Add build pipeline (tsc + tsconfig.build.json + CSS copy)
- [ ] Update package.json for npm publishing (exports, files, scripts)
- [ ] Add .gitignore, LICENSE, README.md, CONTRIBUTING.md, CODE_OF_CONDUCT.md
- [ ] Add CI workflow (test + lint + build on PR)
- [ ] Add release workflow (publish to npm on GitHub Release)
- [ ] Set up `@open-elements` npm organization
- [ ] Add `NPM_TOKEN` secret to GitHub repository
- [ ] Run `pnpm pack --dry-run` to verify package contents
- [ ] Publish first version to npm
- [ ] Update CRM `package.json`: `workspace:*` → `^0.1.0`
- [ ] Update CRM `globals.css`: adjust `@source` path
- [ ] Delete `frontend/packages/ui/`
- [ ] Clean up `pnpm-workspace.yaml`
- [ ] Run CRM build + tests to verify everything works
- [ ] Update any CI/CD that referenced the workspace package

## Open Questions

1. **Tailwind CSS in compiled output:** Should the compiled package include pre-built Tailwind CSS,
   or rely on consumers scanning the source? Including source is simpler and avoids duplicate theme
   configuration, but requires consumers to use Tailwind.
2. **Versioning automation:** Manual version bumps + GitHub Releases, or adopt changesets from
   the start?
3. **Storybook / documentation site:** Should a Storybook be added to the standalone repo for
   component development and documentation? Deferred, but worth considering.
4. **Next.js dependency:** `NavItem` currently renders a plain `<a>` tag. Should the package offer
   a `linkComponent` prop or a context-based approach to integrate with framework-specific routers
   (Next.js `Link`, React Router `Link`, etc.)?
