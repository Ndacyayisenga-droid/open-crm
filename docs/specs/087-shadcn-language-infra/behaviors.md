# Behaviors: shadcn/ui Components + LanguageProvider Infrastructure

## shadcn/ui Component Re-exports

### All 13 components are importable from UI package

- **Given** an app that depends on `@open-elements/ui`
- **When** importing any shadcn/ui component (e.g., `import { Dialog, DialogContent } from "@open-elements/ui"`)
- **Then** the import resolves successfully and the component renders identically to the previous local version

### No local component copies remain in the app

- **Given** the migration is complete
- **When** checking `frontend/src/components/ui/`
- **Then** the directory is empty or removed

### Components retain identical styling and behavior

- **Given** a component (e.g., `Select`) imported from `@open-elements/ui`
- **When** rendering it with the same props as before
- **Then** it produces the same DOM structure, classes, and interactive behavior as the local version

## LanguageProvider

### Provider exports are available from UI package

- **Given** an app that depends on `@open-elements/ui`
- **When** importing `LanguageProvider`, `useTranslations`, `useLanguage`, and `Language` from `@open-elements/ui`
- **Then** all imports resolve successfully

### Provider initializes with browser language detection

- **Given** no language is stored in localStorage
- **When** the `LanguageProvider` mounts and the browser language starts with "de"
- **Then** the active language is "de"

### Provider initializes with stored language preference

- **Given** localStorage contains a language preference of "en"
- **When** the `LanguageProvider` mounts
- **Then** the active language is "en" regardless of browser language

### Provider falls back to default language

- **Given** no language in localStorage and browser language is "fr"
- **When** the `LanguageProvider` mounts with `defaultLanguage="en"`
- **Then** the active language is "en"

### useLanguage returns current language and setter

- **Given** the `LanguageProvider` is mounted with active language "de"
- **When** calling `useLanguage()`
- **Then** it returns `{ language: "de", setLanguage: Function }`

### setLanguage updates language and persists to localStorage

- **Given** the active language is "de"
- **When** calling `setLanguage("en")`
- **Then** the active language changes to "en", localStorage is updated, and `document.documentElement.lang` is set to "en"

### useTranslations returns translations for active language

- **Given** the `LanguageProvider` is mounted with translations `{ de: { greeting: "Hallo" }, en: { greeting: "Hello" } }` and active language "de"
- **When** calling `useTranslations()`
- **Then** it returns `{ greeting: "Hallo" }`

## Translation Merge Pattern

### App can extend UI translations

- **Given** the UI package exports base translations `{ tagMultiSelect: { placeholder: "Search..." } }`
- **When** the app creates merged translations `{ ...uiEn, app: { title: "My App" } }`
- **Then** the merged object contains both `tagMultiSelect` and `app` keys

### UI components receive their own translations through the merged object

- **Given** an app passes merged translations (UI base + app-specific) to the `LanguageProvider`
- **When** a UI package component (e.g., `TagMultiSelect`) calls `useTranslations()`
- **Then** it can access its own keys (e.g., `t.tagMultiSelect.placeholder`)

## Peer Dependencies

### Calendar component works when react-day-picker is installed

- **Given** the consuming app has `react-day-picker` installed
- **When** importing and rendering the `Calendar` component from `@open-elements/ui`
- **Then** the calendar renders correctly with all navigation and selection behavior

### Missing peer dependency produces a clear error

- **Given** the consuming app does NOT have `react-day-picker` installed
- **When** importing the `Calendar` component from `@open-elements/ui`
- **Then** the package manager warns about the missing peer dependency at install time

## Build and Type Safety

### UI package compiles without errors

- **Given** all 13 components and the LanguageProvider have been added to the UI package
- **When** running TypeScript type checking on the UI package
- **Then** no type errors are reported

### App compiles without errors after import migration

- **Given** all app imports have been updated from `@/components/ui/*` to `@open-elements/ui`
- **When** building the app with `next build` (or type checking)
- **Then** no import resolution errors or type errors occur

### All existing UI package tests still pass

- **Given** the UI package has existing tests for Button, Input, Textarea, TagMultiSelect, and cn
- **When** running the test suite after adding the new components
- **Then** all existing tests pass without modification
