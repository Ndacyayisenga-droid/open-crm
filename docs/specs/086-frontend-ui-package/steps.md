# Implementation Steps: Frontend UI Package Extraction

## Step 1: Create pnpm workspace and package scaffolding

- [x] Create `frontend/pnpm-workspace.yaml` with `packages: ["packages/*"]`
- [x] Create `frontend/packages/ui/package.json` with name `@open-elements/ui`, peer dependencies (react, react-dom, @base-ui/react, radix-ui, lucide-react, class-variance-authority, clsx, tailwind-merge), and dev dependencies for testing/linting
- [x] Create `frontend/packages/ui/tsconfig.json` with `@ui/*` path alias pointing to `./src/*`
- [x] Create `frontend/packages/ui/vitest.config.ts` with jsdom environment
- [x] Create `frontend/packages/ui/eslint.config.mjs`
- [x] Create `frontend/packages/ui/.prettierrc`
- [x] Create `frontend/packages/ui/README.md` with package description
- [x] Add `"@open-elements/ui": "workspace:*"` to the app's `package.json` dependencies
- [x] Run `pnpm install` from `frontend/` to link the workspace package

**Acceptance criteria:**
- [x] `pnpm install` completes without errors
- [x] `@open-elements/ui` is resolved as a workspace link
- [x] Project builds successfully

**Related behaviors:** pnpm workspace resolves the package, App declares workspace dependency

---

## Step 2: Extract utility and brand styling

- [x] Create `frontend/packages/ui/src/lib/utils.ts` with the `cn()` function
- [x] Create `frontend/packages/ui/src/styles/brand.css` with all Open Elements brand colors, fonts, and semantic tokens extracted from `globals.css`
- [x] Remove brand color/font/semantic token definitions from the app's `globals.css`
- [x] Update app's `globals.css` to import brand.css from the package and add `@source "../packages/ui/src"` directive

**Acceptance criteria:**
- [x] Brand CSS is importable from package
- [x] `cn()` works from the package
- [x] App builds successfully with brand styles applied
- [x] No `--color-oe-*` or semantic token definitions remain in app's `globals.css` `@theme` block

**Related behaviors:** Brand CSS is importable from package, Brand colors are removed from app globals.css, Tailwind scans package source for utility classes, App can override brand tokens

---

## Step 3: Extract base UI components (Button, Input, Textarea)

- [x] Copy `button.tsx` to `frontend/packages/ui/src/components/button.tsx`, update imports to use package-local paths (`../lib/utils` instead of `@/lib/utils`)
- [x] Copy `input.tsx` to `frontend/packages/ui/src/components/input.tsx`, update imports
- [x] Copy `textarea.tsx` to `frontend/packages/ui/src/components/textarea.tsx`, update imports
- [x] Delete `frontend/src/components/ui/button.tsx`, `input.tsx`, `textarea.tsx` from the app
- [x] Delete `frontend/src/lib/utils.ts` from the app
- [x] Update all app imports of `Button` from `@/components/ui/button` to `@open-elements/ui`
- [x] Update all app imports of `Input` from `@/components/ui/input` to `@open-elements/ui`
- [x] Update all app imports of `Textarea` from `@/components/ui/textarea` to `@open-elements/ui`
- [x] Update all app imports of `cn` from `@/lib/utils` to `@open-elements/ui`

**Acceptance criteria:**
- [x] Button, Input, Textarea render correctly from the package
- [x] No app files import from `@/components/ui/button`, `@/components/ui/input`, `@/components/ui/textarea`, or `@/lib/utils`
- [x] App builds successfully

**Related behaviors:** Button renders correctly from package, Input and Textarea render correctly from package, cn utility is removed from app, Original files are removed from app, All app imports of extracted components use package path

---

## Step 4: Extract InputGroup and Combobox components

- [x] Copy `input-group.tsx` to `frontend/packages/ui/src/components/input-group.tsx`, update all imports to package-local paths (import Button from `./button`, cn from `../lib/utils`)
- [x] Copy `combobox.tsx` to `frontend/packages/ui/src/components/combobox.tsx`, update all imports to package-local paths
- [x] Delete `frontend/src/components/ui/input-group.tsx` and `combobox.tsx` from the app
- [x] Update all app imports of InputGroup components to `@open-elements/ui`
- [x] Update all app imports of Combobox components to `@open-elements/ui`

**Acceptance criteria:**
- [x] InputGroup composes correctly from the package
- [x] Combobox renders correctly from the package
- [x] No app files import from `@/components/ui/input-group` or `@/components/ui/combobox`
- [x] App builds successfully

**Related behaviors:** Combobox renders correctly from package, InputGroup composes correctly from package, Internal imports use package-local paths

---

## Step 5: Extract TagDto type and TagMultiSelect component

- [x] Create `frontend/packages/ui/src/types/index.ts` with `TagDto`, `TagOption`, `TagMultiSelectProps`, and `TagMultiSelectTranslations` interfaces
- [x] Refactor `tag-multi-select.tsx` into `frontend/packages/ui/src/components/tag-multi-select.tsx`:
  - Remove `getTags` import from `@/lib/api`
  - Remove `useTranslations` import from `@/lib/i18n`
  - Add `loadTags`, `translations` props to the component interface
  - Use package-local imports for Combobox and other dependencies
- [x] Remove `TagDto` from the app's `src/lib/types.ts`
- [x] Delete `frontend/src/components/tag-multi-select.tsx` from the app
- [x] Update all app imports of `TagDto` to use `@open-elements/ui`
- [x] Update all app usages of `TagMultiSelect` to import from `@open-elements/ui` and pass `loadTags` callback and `translations` props

**Acceptance criteria:**
- [x] `TagDto` compiles when imported from `@open-elements/ui`
- [x] `TagMultiSelect` loads tags via the `loadTags` callback
- [x] `TagMultiSelect` uses provided translations
- [x] No `@/` path imports exist in any package file
- [x] App builds successfully

**Related behaviors:** App uses TagDto from package, TagDto is removed from app types, TagMultiSelect loads tags via callback, TagMultiSelect uses provided translations, TagMultiSelect displays selected tags as colored chips, TagMultiSelect calls onChange on selection, TagMultiSelect calls onChange on chip removal, TagMultiSelect handles loadTags failure, TagMultiSelect handles invalid color gracefully, Package has no app-specific imports

---

## Step 6: Create barrel export and translation strings

- [x] Create `frontend/packages/ui/src/i18n/de.ts` with German translations for TagMultiSelect
- [x] Create `frontend/packages/ui/src/i18n/en.ts` with English translations for TagMultiSelect
- [x] Create `frontend/packages/ui/src/index.ts` barrel export with all components, types, utilities, and translations
- [x] Update the app's `src/lib/i18n/de.ts` to import and spread `uiDe` from `@open-elements/ui`
- [x] Update the app's `src/lib/i18n/en.ts` to import and spread `uiEn` from `@open-elements/ui`

**Acceptance criteria:**
- [x] All public API items are importable from `@open-elements/ui` without TypeScript errors
- [x] Package exports `de` and `en` translation objects with `tagMultiSelect` keys
- [x] App merges package translations and `t.tagMultiSelect.placeholder` works
- [x] App builds successfully

**Related behaviors:** Barrel export exposes all public API, Package exports DE/EN translation objects, App merges package translations

---

## Step 7: Update Next.js config for workspace transpilation

- [x] Update `frontend/next.config.ts` to add `transpilePackages: ["@open-elements/ui"]` so Next.js compiles the raw `.tsx` source from the workspace package
- [x] Update the app's `tsconfig.json` if needed to resolve `@open-elements/ui` paths

**Acceptance criteria:**
- [x] `pnpm build` completes without errors from `frontend/`
- [x] `pnpm dev` works and all pages render correctly
- [x] All Tailwind utility classes from package components are included in CSS output

**Related behaviors:** App builds successfully after extraction

---

## Step 8: Package tests (cn, Combobox, TagMultiSelect)

- [x] Create `frontend/packages/ui/src/lib/__tests__/utils.test.ts` — unit tests for `cn()` class merging
- [x] Create `frontend/packages/ui/src/components/__tests__/combobox.test.tsx` — rendering tests with Testing Library for Combobox sub-components
- [x] Create `frontend/packages/ui/src/components/__tests__/tag-multi-select.test.tsx` — tests with mocked `loadTags`:
  - Tag loading via callback
  - Translation props usage
  - Colored chip rendering
  - Selection and deselection onChange calls
  - loadTags failure handling
  - Invalid color fallback
- [x] Add test setup file for the package if needed (`src/test/setup.ts`)
- [x] Verify `pnpm test` passes from `packages/ui/`

**Acceptance criteria:**
- [x] All package tests pass independently via `pnpm test` in `packages/ui/`
- [x] Tests cover: cn utility, Combobox rendering, TagMultiSelect behavior (load, select, deselect, error, invalid color)

**Related behaviors:** Package tests pass independently, TagMultiSelect loads tags via callback, TagMultiSelect uses provided translations, TagMultiSelect displays selected tags as colored chips, TagMultiSelect calls onChange on selection, TagMultiSelect calls onChange on chip removal, TagMultiSelect handles loadTags failure, TagMultiSelect handles invalid color gracefully

---

## Step 9: App tests pass after extraction

- [x] Run existing app tests and fix any broken imports or test failures
- [x] Verify `pnpm test` passes from `frontend/`
- [x] Verify `pnpm lint` passes from `frontend/`

**Acceptance criteria:**
- [x] All existing app tests pass
- [x] No lint errors

**Related behaviors:** App tests pass after extraction, Package linting passes

---

## Step 10: Frontend behavioral scenario tests

- [x] Test that Button renders with correct variant classes from package import
- [x] Test that Input and Textarea render with correct styles from package import
- [x] Test that InputGroup composes addon + input correctly from package import
- [x] Test that Combobox opens, selects items, and displays chips from package import
- [x] Test that all imports from `@open-elements/ui` resolve (barrel export test)
- [x] Test that translations merge correctly (tagMultiSelect keys accessible)

**Acceptance criteria:**
- [x] All frontend behavioral scenarios from behaviors.md have corresponding passing tests
- [x] Tests verify component rendering, import resolution, and translation merging

**Related behaviors:** Button renders correctly from package, Input and Textarea render correctly from package, InputGroup composes correctly from package, Combobox renders correctly from package, Barrel export exposes all public API, App merges package translations

---

## Step 11: Peer dependency verification

- [x] Verify package.json `peerDependencies` lists all required external deps
- [x] Verify pnpm warns when a peer dependency is missing (manual check)

**Acceptance criteria:**
- [x] Package declares react, react-dom, @base-ui/react, radix-ui, lucide-react, class-variance-authority, clsx, tailwind-merge as peer dependencies

**Related behaviors:** Package declares all external deps as peer dependencies, Missing peer dependency produces a warning

---

## Step 12: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — add UI package feature
- [x] Update `.claude/conventions/project-specific/project-tech.md` — add pnpm workspaces
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add packages/ui directory
- [x] Update `.claude/conventions/project-specific/project-architecture.md` — add package extraction architecture
- [x] Update `README.md` if setup instructions changed (pnpm workspace)
- [x] Update `CLAUDE.md` if new conventions were introduced

**Acceptance criteria:**
- [x] All documentation files reflect the new package structure
- [x] README includes workspace setup instructions if applicable

**Related behaviors:** N/A (documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| pnpm workspace resolves the package | Build | Step 1 |
| App declares workspace dependency | Build | Step 1 |
| Barrel export exposes all public API | Frontend | Steps 6, 10 |
| Internal imports use package-local paths | Frontend | Steps 3, 4, 5 |
| Package has no app-specific imports | Frontend | Step 5 |
| Button renders correctly from package | Frontend | Steps 3, 10 |
| Combobox renders correctly from package | Frontend | Steps 4, 8, 10 |
| Input and Textarea render correctly from package | Frontend | Steps 3, 10 |
| InputGroup composes correctly from package | Frontend | Steps 4, 10 |
| TagMultiSelect loads tags via callback | Frontend | Steps 5, 8 |
| TagMultiSelect uses provided translations | Frontend | Steps 5, 8 |
| TagMultiSelect displays selected tags as colored chips | Frontend | Steps 5, 8 |
| TagMultiSelect calls onChange on selection | Frontend | Steps 5, 8 |
| TagMultiSelect calls onChange on chip removal | Frontend | Steps 5, 8 |
| TagMultiSelect handles loadTags failure | Frontend | Step 8 |
| TagMultiSelect handles invalid color gracefully | Frontend | Step 8 |
| App uses TagDto from package | Frontend | Step 5 |
| TagDto is removed from app types | Frontend | Step 5 |
| Brand CSS is importable from package | Build/Frontend | Step 2 |
| Brand colors are removed from app globals.css | Frontend | Step 2 |
| App can override brand tokens | Frontend | Step 2 |
| Tailwind scans package source for utility classes | Build | Steps 2, 7 |
| Package exports DE/EN translation objects | Frontend | Steps 6, 10 |
| App merges package translations | Frontend | Steps 6, 10 |
| All app imports of extracted components use package path | Frontend | Steps 3, 4, 5 |
| Original files are removed from app | Frontend | Steps 3, 4, 5 |
| cn utility is removed from app | Frontend | Step 3 |
| App builds successfully after extraction | Build | Steps 7, 9 |
| App tests pass after extraction | Frontend | Step 9 |
| Package tests pass independently | Frontend | Step 8 |
| Package linting passes | Frontend | Step 9 |
| Package declares all external deps as peer dependencies | Build | Step 11 |
| Missing peer dependency produces a warning | Build | Step 11 |
