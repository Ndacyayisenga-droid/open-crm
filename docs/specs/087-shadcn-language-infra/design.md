# Design: shadcn/ui Components + LanguageProvider Infrastructure

## GitHub Issue

—

## Summary

The `@open-elements/ui` package currently contains a small set of components (Button, Input, Textarea, Combobox, TagMultiSelect). Multiple apps (CRM, TODO, and two more planned) share the same tech stack (Next.js, Tailwind, shadcn/ui) and duplicate the same 13 shadcn/ui component files and i18n infrastructure. This spec moves all 13 shadcn/ui components and the LanguageProvider/Context into the shared UI package so that consuming apps import them from a single source.

This is a prerequisite for Spec 088, which extracts higher-level app components that depend on these primitives.

## Goals

- Move all 13 shadcn/ui components into `@open-elements/ui` so apps no longer need local copies
- Move the `LanguageProvider`, `useTranslations`, and `useLanguage` into the UI package so consuming apps share a single i18n infrastructure
- Establish a translation merge pattern where the UI package owns component-internal translations and apps extend them with domain-specific keys
- Keep the UI package build-free (ships raw `.tsx` source)

## Non-goals

- Modifying component behavior or styling — this is a pure move/re-export
- Adding new shadcn/ui components beyond the existing 13
- Supporting languages beyond DE/EN
- Moving the `session-provider` or any auth-related code

## Technical Approach

### Phase 1: Move shadcn/ui Components

Move all 13 components from `frontend/src/components/ui/` into `frontend/packages/ui/src/components/`:

| Component | External Dependencies | Notes |
|---|---|---|
| `alert-dialog` | `radix-ui` (AlertDialog) | Uses `cn`, `Button` from UI package (already available) |
| `badge` | `radix-ui` (Slot), `class-variance-authority` | CVA already a peer dep |
| `calendar` | `react-day-picker`, `lucide-react` | Needs `react-day-picker` as new peer dep |
| `card` | — | Only uses `cn` |
| `dialog` | `radix-ui` (Dialog), `lucide-react` | — |
| `label` | `radix-ui` (Label) | — |
| `popover` | `radix-ui` (Popover) | — |
| `select` | `radix-ui` (Select), `lucide-react` | — |
| `separator` | `radix-ui` (Separator) | — |
| `sheet` | `radix-ui` (Dialog as Sheet), `lucide-react` | — |
| `skeleton` | — | Only uses `cn` |
| `table` | — | Only uses `cn` |
| `tooltip` | `@radix-ui/react-tooltip` | Uses individual Radix package, not aggregated `radix-ui` |

**Tooltip dependency note:** The tooltip component imports from `@radix-ui/react-tooltip` (individual package) while all other components use the aggregated `radix-ui` package. During the move, the tooltip import should be migrated to use the aggregated `radix-ui` package for consistency, or `@radix-ui/react-tooltip` must be added as a peer dependency. The preferred approach is to migrate to `radix-ui` for consistency.

**New peer dependency:** `react-day-picker` (^9.14.0) must be added to the UI package's `peerDependencies` for the calendar component.

### Phase 2: Move LanguageProvider

Move the i18n infrastructure from `frontend/src/lib/i18n/` into `frontend/packages/ui/src/i18n/`:

**Files to move:**
- `language-context.tsx` → `packages/ui/src/i18n/language-context.tsx`

**Translation merge pattern:**

The UI package defines its own translation keys (for component-internal strings like TagMultiSelect placeholder). Apps extend these by merging:

```typescript
// UI package exports base translations and the Language type
export type Language = "de" | "en";
export { en } from "./i18n/en";
export { de } from "./i18n/de";

// App merges its own translations on top
import { en as uiEn, de as uiDe } from "@open-elements/ui";

const en = { ...uiEn, app: { title: "Open CRM" }, /* ... */ };
const de = { ...uiDe, app: { title: "Open CRM" }, /* ... */ };
```

The `LanguageProvider` accepts a `translations` record as a prop so apps can pass their merged translation objects:

```typescript
// In UI package
interface LanguageProviderProps {
  translations: Record<Language, Translations>;
  defaultLanguage?: Language;
  children: React.ReactNode;
}
```

The `Translations` type is generic — the provider uses whatever shape the app passes. The UI package's internal translations define a base interface that apps extend via TypeScript intersection or spread.

### Phase 3: Update App Imports

- Replace all `@/components/ui/*` imports with `@open-elements/ui`
- Replace `@/lib/i18n/language-context` imports with `@open-elements/ui`
- Remove the now-empty `frontend/src/components/ui/` directory
- Remove `frontend/src/lib/i18n/language-context.tsx` (moved to UI package)
- Update `frontend/src/lib/i18n/index.ts` to use the `Language` type from UI package

### Exports

All 13 components and their sub-components are re-exported from the UI package's `index.ts`. The LanguageProvider, `useTranslations`, `useLanguage`, and `Language` type are also exported.

## Dependencies

- `radix-ui` (already a peer dep)
- `@radix-ui/react-tooltip` (migrate to `radix-ui` or add as peer dep)
- `react-day-picker` (new peer dep for calendar)
- `lucide-react` (already a peer dep)
- `class-variance-authority` (already a peer dep)

## Security Considerations

None — this is a pure code reorganization with no behavioral changes.

## Open Questions

1. Should the tooltip component be migrated from `@radix-ui/react-tooltip` to the aggregated `radix-ui` package during the move? (Recommended: yes, for consistency)
