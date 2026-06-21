# Behaviors: Frontend UI Package Extraction

## Workspace Setup

### pnpm workspace resolves the package

- **Given** `pnpm-workspace.yaml` exists at `frontend/` with `packages: ["packages/*"]`
- **When** running `pnpm install` from `frontend/`
- **Then** `@open-elements/ui` is resolved as a workspace link, not fetched from npm

### App declares workspace dependency

- **Given** the app's `package.json` contains `"@open-elements/ui": "workspace:*"`
- **When** running `pnpm install`
- **Then** the dependency resolves to `frontend/packages/ui` without errors

## Package Structure

### Barrel export exposes all public API

- **Given** the package's `src/index.ts` exists
- **When** importing `{ Button, Combobox, TagMultiSelect, TagDto, cn, de, en }` from `@open-elements/ui`
- **Then** all imports resolve without TypeScript errors

### Internal imports use package-local paths

- **Given** `combobox.tsx` in the package imports `Button` and `InputGroup`
- **When** the import path is `./button` or `./input-group` (relative within package)
- **Then** no imports reference `@/components/ui/` (the app's path alias)

### Package has no app-specific imports

- **Given** any file in `packages/ui/src/`
- **When** scanning all import statements
- **Then** no import references `@/lib/api`, `@/lib/types`, `@/lib/i18n`, or any `@/` path

## Component Extraction

### Button renders correctly from package

- **Given** the app imports `Button` from `@open-elements/ui`
- **When** rendering `<Button variant="default">Click</Button>`
- **Then** the button renders with primary background color and correct Tailwind classes

### Combobox renders correctly from package

- **Given** the app imports `Combobox`, `ComboboxChips`, `ComboboxContent` from `@open-elements/ui`
- **When** rendering a Combobox with items
- **Then** the combobox dropdown opens, items are selectable, and chips are displayed

### Input and Textarea render correctly from package

- **Given** the app imports `Input` and `Textarea` from `@open-elements/ui`
- **When** rendering both components
- **Then** they render with correct border, focus ring, and placeholder styles

### InputGroup composes correctly from package

- **Given** the app imports `InputGroup`, `InputGroupInput`, `InputGroupAddon` from `@open-elements/ui`
- **When** rendering an InputGroup with addon and input
- **Then** the addon and input are visually grouped with shared border and focus state

## TagMultiSelect Refactoring

### TagMultiSelect loads tags via callback

- **Given** a `loadTags` callback that returns `[{ value: "1", label: "VIP", color: "#FF0000" }]`
- **When** rendering `<TagMultiSelect loadTags={loadTags} ... />`
- **Then** the component calls `loadTags` on mount and displays "VIP" in the dropdown

### TagMultiSelect uses provided translations

- **Given** `translations: { placeholder: "Choose tags...", empty: "No tags" }`
- **When** rendering TagMultiSelect with no tags loaded
- **Then** the input placeholder shows "Choose tags..." and the empty state shows "No tags"

### TagMultiSelect displays selected tags as colored chips

- **Given** `loadTags` returns tags with colors and `selectedIds` contains `["1"]`
- **When** TagMultiSelect renders
- **Then** a chip with the tag's background color and contrast text color is displayed

### TagMultiSelect calls onChange on selection

- **Given** TagMultiSelect is rendered with available tags
- **When** the user selects a tag from the dropdown
- **Then** `onChange` is called with the updated array of selected tag IDs

### TagMultiSelect calls onChange on chip removal

- **Given** TagMultiSelect has a selected tag displayed as a chip
- **When** the user clicks the remove button on the chip
- **Then** `onChange` is called with the tag ID removed from the array

### TagMultiSelect handles loadTags failure

- **Given** a `loadTags` callback that rejects with an error
- **When** TagMultiSelect mounts
- **Then** the component renders without crashing and shows the empty state

### TagMultiSelect handles invalid color gracefully

- **Given** a tag with `color: "not-a-color"`
- **When** the tag is displayed as a chip or in the dropdown
- **Then** a fallback gray color (`#6B7280`) is used instead

## TagDto

### App uses TagDto from package

- **Given** the app previously imported `TagDto` from `@/lib/types`
- **When** changed to import from `@open-elements/ui`
- **Then** all type usage compiles without errors

### TagDto is removed from app types

- **Given** the package exports `TagDto`
- **When** checking the app's `src/lib/types.ts`
- **Then** `TagDto` is no longer defined there

## Brand Styling

### Brand CSS is importable from package

- **Given** the file `packages/ui/src/styles/brand.css` exists
- **When** the app's `globals.css` imports it via `@import "@open-elements/ui/src/styles/brand.css"`
- **Then** all Open Elements brand color tokens are available in the app

### Brand colors are removed from app globals.css

- **Given** brand colors are defined in the package's `brand.css`
- **When** checking the app's `globals.css`
- **Then** the `@theme` block no longer contains `--color-oe-*` or semantic token definitions

### App can override brand tokens

- **Given** the app imports brand.css from the package
- **When** the app defines `--color-primary: #FF0000` after the import
- **Then** the app uses red as primary instead of the package's green

### Tailwind scans package source for utility classes

- **Given** the app's `globals.css` contains `@source "../packages/ui/src"`
- **When** building the app with `pnpm build`
- **Then** all Tailwind utility classes used in package components are included in the CSS output

## Translation Strings

### Package exports DE/EN translation objects

- **Given** the package exports `de` and `en` from `@open-elements/ui`
- **When** importing them in the app's i18n files
- **Then** the objects contain `tagMultiSelect.placeholder` and `tagMultiSelect.empty` keys

### App merges package translations

- **Given** the app's `de.ts` spreads `...uiDe` from the package
- **When** accessing `t.tagMultiSelect.placeholder` via `useTranslations()`
- **Then** the German tag placeholder string is returned

## Import Migration

### All app imports of extracted components use package path

- **Given** the app previously imported `Button` from `@/components/ui/button`
- **When** migration is complete
- **Then** the import reads `import { Button } from "@open-elements/ui"`

### Original files are removed from app

- **Given** `Button`, `Input`, `Textarea`, `InputGroup`, `Combobox` are in the package
- **When** checking `frontend/src/components/ui/`
- **Then** `button.tsx`, `input.tsx`, `textarea.tsx`, `input-group.tsx`, `combobox.tsx` no longer exist there

### cn utility is removed from app

- **Given** `cn()` is exported from the package
- **When** checking `frontend/src/lib/utils.ts`
- **Then** the file no longer exists (or no longer exports `cn`)

## Build and Tests

### App builds successfully after extraction

- **Given** all components are extracted and imports are updated
- **When** running `pnpm build` from the app directory
- **Then** the Next.js build completes without errors

### App tests pass after extraction

- **Given** all components are extracted and imports are updated
- **When** running `pnpm test` from the app directory
- **Then** all existing tests pass

### Package tests pass independently

- **Given** the package has its own `vitest.config.ts`
- **When** running `pnpm test` from `packages/ui/`
- **Then** all package tests pass (Combobox rendering, TagMultiSelect behavior, cn utility)

### Package linting passes

- **Given** the package has its own `eslint.config.mjs`
- **When** running `pnpm lint` from `packages/ui/`
- **Then** no lint errors are reported

## Peer Dependencies

### Package declares all external deps as peer dependencies

- **Given** the package's `package.json`
- **When** checking the `peerDependencies` field
- **Then** it lists `react`, `react-dom`, `@base-ui/react`, `radix-ui`, `lucide-react`,
  `class-variance-authority`, `clsx`, and `tailwind-merge`

### Missing peer dependency produces a warning

- **Given** a consuming app that does not have `@base-ui/react` installed
- **When** running `pnpm install`
- **Then** pnpm warns about the missing peer dependency
