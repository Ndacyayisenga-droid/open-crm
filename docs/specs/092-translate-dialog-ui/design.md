# Design: Extract TranslateDialog to @open-elements/ui

## GitHub Issue

—

## Summary

The `TranslateDialog` component was built in open-crm as part of Spec 091. It is a generic, reusable dialog that shows a translated text with copy-to-clipboard functionality. Since it has no domain-specific logic, it should live in the shared `@open-elements/ui` package so other Open Elements applications can use it.

## Goals

- Move `TranslateDialog` into `@open-elements/ui` as a prop-driven, reusable component
- Follow the established migration pattern (translations-prop, async callback-prop)
- Remove the local copy from open-crm and import from `@open-elements/ui` instead

## Non-goals

- Migrating the translate button or API call logic — these remain in open-crm
- Adding a `targetLanguage` override prop — not needed currently
- Changing the visual design or behavior of the dialog

## Technical Approach

### Component API

The migrated `TranslateDialog` follows the same patterns as `TagForm`, `HealthStatus`, and `AddCommentDialog`:

```typescript
interface TranslateDialogTranslations {
  title: string;
  loading: string;
  error: string;
  copy: string;
  copied: string;
  close: string;
}

interface TranslateResult {
  translatedText: string;
}

interface TranslateDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly sourceText: string;
  readonly onTranslate: (text: string, targetLanguage: string) => Promise<TranslateResult>;
  readonly translations: TranslateDialogTranslations;
}
```

**Key decisions:**

- **`onTranslate` callback:** The component calls this internally when the dialog opens, passing `sourceText` and the `targetLanguage` derived from `useLanguage()`. The component manages loading/error/success states itself. **Rationale:** This matches the TagForm pattern where the component orchestrates async operations internally via callback props.

- **`TranslateResult` object:** Returns `{ translatedText: string }` instead of a raw string. **Rationale:** An object is extensible — metadata like detected source language or confidence scores can be added later without breaking the API.

- **`translations` prop:** All UI labels are passed as props, not read from `useLanguage()`. **Rationale:** Consistent with all other migrated components (TagForm, HealthStatus, AddCommentDialog). Allows apps to provide their own translations.

- **`useLanguage()` for target language:** The component uses `useLanguage()` from `@open-elements/ui` internally to determine the target language. **Rationale:** This hook is already part of `@open-elements/ui`, so no external dependency is introduced. The caller does not need to know about or manage the target language.

### Changes in @open-elements/ui

1. **New file:** `TranslateDialog` component (equivalent to the current `translate-dialog.tsx` but with the prop-based API described above)
2. **Export:** Add `TranslateDialog`, `TranslateDialogProps`, `TranslateDialogTranslations`, and `TranslateResult` to the package exports

### Changes in open-crm

1. **Delete:** `frontend/src/components/translate-dialog.tsx`
2. **Update imports:** All files that import `TranslateDialog` switch from `@/components/translate-dialog` to `@open-elements/ui`
3. **Wire up props:** Each usage site passes:
   - `onTranslate` — wrapping the existing `translateText` API function
   - `translations` — from the existing `t.translation` i18n keys

### Usage after migration

```tsx
import { TranslateDialog } from "@open-elements/ui";
import { translateText } from "@/lib/api";

<TranslateDialog
  open={translateOpen}
  onOpenChange={setTranslateOpen}
  sourceText={description}
  onTranslate={async (text, lang) => translateText(text, lang)}
  translations={{
    title: t.translation.title,
    loading: t.translation.loading,
    error: t.translation.error,
    copy: t.translation.copy,
    copied: t.translation.copied,
    close: t.translation.close,
  }}
/>
```

### Files affected

| Repository | File | Action |
|------------|------|--------|
| @open-elements/ui | `TranslateDialog` component | Create |
| @open-elements/ui | Package exports | Update |
| open-crm | `frontend/src/components/translate-dialog.tsx` | Delete |
| open-crm | `frontend/src/components/company-detail.tsx` | Update import |
| open-crm | `frontend/src/components/contact-detail.tsx` | Update import |
| open-crm | `frontend/src/components/company-comments.tsx` | Update import |
| open-crm | `frontend/src/components/contact-comments.tsx` | Update import |
| open-crm | `frontend/src/components/task-comments.tsx` | Update import |

## Dependencies

- `@open-elements/ui` must be built and published (or linked locally via `pnpm link`) before open-crm can consume the new component
- `lucide-react` icons (`Check`, `Copy`, `Loader2`) — already a peer dependency of `@open-elements/ui`
- `useLanguage()` hook — already in `@open-elements/ui`

## Open Questions

None.
