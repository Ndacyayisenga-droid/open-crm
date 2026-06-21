# Design: Admin routes, translate components, wrapper-file cleanup

## GitHub Issue

[#22](https://github.com/OpenElementsLabs/open-crm/issues/22)

Supersedes the open scope of spec [092](../092-translate-dialog-ui/) (TranslateDialog → `@open-elements/ui`).

## Summary

Three structural cleanups that finish the migration tail of spec 098 and pick up the open scope of spec 092, bundled into one spec so the two affected libs (`@open-elements/ui`, `@open-elements/nextjs-app-layer`) and the Open-CRM app are aligned in one wave of releases:

1. **Admin route consolidation** — move `/api-keys` and `/webhooks` under `/admin/`. Both are IT-ADMIN-gated and the sidebar already groups them with the other admin pages; the URL should match the role contract.
2. **Translate components → `@open-elements/ui`** — extract `TranslateDialog` (per spec 092's existing design) and `TranslateButton` as prop-driven, reusable components.
3. **Wrapper-file cleanup** — remove pass-through re-export files left over from spec 098 (`session-provider.tsx`, `forbidden-page.tsx`, `add-comment-dialog.tsx`, `roles.ts`, `forbidden-error.ts`). Every importer points directly at `@open-elements/nextjs-app-layer`.

## Goals

- All Open-CRM admin pages live under `/admin/*`.
- `TranslateButton` and `TranslateDialog` are reusable across Open Elements apps via `@open-elements/ui`.
- Open-CRM contains no pass-through indirection layers — readers see at the import what library a symbol comes from.
- The two libs are released in lock-step with the app PR:
  - `@open-elements/ui` minor bump (TranslateButton + TranslateDialog).
  - `@open-elements/nextjs-app-layer@0.2.0` (default-route changes).
- Behavior of every affected feature is unchanged in the browser.

## Non-goals

- Migrating `useTranslationConfig` into `@open-elements/nextjs-app-layer`. Spec 098's design said it should be there; the implementation left it in `frontend/src/lib/use-translation-config.ts`. That drift is acknowledged here but not fixed — the hook stays in the app, and lib components consume `configured` as a prop. A separate spec can address the drift later.
- Bumping past 0.2.0 for `nextjs-app-layer` or past the chosen minor for ui-lib.
- Backend changes.
- Changing how the translate feature works (target language selection, copy behaviour, error display).
- Switching consumption mode from source (`src/`) to built `dist/` for either lib. Spec 099 already decided source mode stays for phase 1.
- Adding redirects from the old `/api-keys` / `/webhooks` URLs. These were internal admin URLs and external links are not a concern.

## Technical approach

### 1. `@open-elements/ui` — `TranslateDialog`

Direct lift from spec 092's design (which never reached implementation). Prop-driven, reads target language from `@open-elements/ui`'s `useLanguage()` hook internally, manages its own loading/error/success/copy state.

```ts
interface TranslateResult {
  readonly translatedText: string;
}

interface TranslateDialogTranslations {
  readonly title: string;
  readonly loading: string;
  readonly error: string;
  readonly copy: string;
  readonly copied: string;
  readonly close: string;
}

interface TranslateDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly sourceText: string;
  readonly onTranslate: (text: string, targetLanguage: string) => Promise<TranslateResult>;
  readonly translations: TranslateDialogTranslations;
}

export function TranslateDialog(props: TranslateDialogProps): JSX.Element;
```

Behaviour mirrors today's `frontend/src/components/translate-dialog.tsx`:

- When `open` flips to `true`, kick off `onTranslate(sourceText, targetLanguage)`.
- `targetLanguage` is `"de"` when `useLanguage().language === "de"`, otherwise `"en"`. This is intentionally the same minimal binary that the current app code uses.
- During the in-flight request, render a `Loader2` plus the `loading` label.
- On error, render the `error` label in destructive color.
- On success, render the translated text and enable the copy button.
- Copy uses `navigator.clipboard.writeText`; on success it flips the icon to `Check` plus the `copied` label for 2 seconds.

### 2. `@open-elements/ui` — `TranslateButton`

Composes `TranslateDialog`. Owns the visibility logic that today's `frontend/src/components/translate-button.tsx` owns: returns `null` if text is empty/whitespace, or if the backend translation feature is not configured.

```ts
interface TranslateButtonTranslations {
  readonly button: string;            // tooltip / aria-label
  readonly dialog: TranslateDialogTranslations;
}

interface TranslateButtonProps {
  readonly text: string | null | undefined;
  readonly size?: "sm" | "md";
  readonly configured: boolean | null;
  readonly onTranslate: (text: string, targetLanguage: string) => Promise<TranslateResult>;
  readonly translations: TranslateButtonTranslations;
}

export function TranslateButton(props: TranslateButtonProps): JSX.Element | null;
```

`configured` is a prop, not a hook call inside the component, because `useTranslationConfig` is an app-layer concern (it hits the backend feature-toggle endpoint via the app's API client). Apps that don't have such a toggle can pass `true` unconditionally.

`size` controls icon dimensions: `"sm"` → `h-3.5 w-3.5`, `"md"` → `h-4 w-4`. Same as today.

### 3. `@open-elements/ui` — exports and tests

Add to the package barrel:

```ts
export { TranslateDialog, TranslateButton } from "./components/...";
export type {
  TranslateDialogProps,
  TranslateDialogTranslations,
  TranslateButtonProps,
  TranslateButtonTranslations,
  TranslateResult,
} from "./components/...";
```

Tests in `src/components/__tests__/`:

- `TranslateDialog`: skeleton/loading state, error state, success state with copy, copy success flips label, dialog reset on re-open.
- `TranslateButton`: hidden when `text` empty, hidden when `configured` is `null`, hidden when `configured` is `false`, renders when `configured` is `true` and text non-empty, click opens dialog.

Release as a new minor — the next version after the currently published one (currently `^0.6.0` in the app; whatever the published latest is at release time gets the bump).

### 4. `@open-elements/nextjs-app-layer` — default-route changes

Today (in `0.1.0`):

```ts
// src/pages/api-keys/meta.ts
export const apiKeysPageMeta = {
  defaultRoute: "/api-keys",
  // …
};

// src/pages/webhooks/meta.ts
export const webhooksPageMeta = {
  defaultRoute: "/webhooks",
  // …
};
```

After:

```ts
export const apiKeysPageMeta = { defaultRoute: "/admin/api-keys", /* … */ };
export const webhooksPageMeta = { defaultRoute: "/admin/webhooks", /* … */ };
```

This is a visible API-surface change for downstream consumers reading the metadata. Release as `0.2.0` (minor — not breaking in TypeScript shape, but the suggested-route value changes).

The lib's own tests do not reference these literal route strings, so no test updates are required there. (Verified by `grep` during implementation.)

### 5. Open-CRM — admin route move

```
frontend/src/app/(app)/
  ├── api-keys/page.tsx        → admin/api-keys/page.tsx
  └── webhooks/page.tsx        → admin/webhooks/page.tsx
```

Per-page files stay 2-line re-exports of the lib's page factories. The Next.js file system route name is the only thing changing.

`frontend/src/app/(app)/layout.tsx` (`CrmSidebar`) updates:

- `NavItem href="/api-keys"` → `href="/admin/api-keys"`
- `NavItem href="/webhooks"` → `href="/admin/webhooks"`
- Both `active` matchers update accordingly.
- `ADMIN_PREFIXES` simplifies from `["/admin", "/api-keys", "/webhooks"]` to just `["/admin"]`.

The old `/api-keys` and `/webhooks` URLs become 404s, which is acceptable because these were internal admin paths and bookmarks/external links are not a concern.

### 6. Open-CRM — wrapper-file removal

The following files in `frontend/src/` only re-export from `@open-elements/nextjs-app-layer`. They are deleted; their importers point directly at the lib:

| File | Re-exports | Importers to update |
|------|-----------|---------------------|
| `src/components/session-provider.tsx` | `SessionProvider` | App layout |
| `src/components/forbidden-page.tsx` | `ForbiddenPage` | Pages that gate manually |
| `src/components/add-comment-dialog.tsx` | `AddCommentDialog` | Comment lists |
| `src/lib/roles.ts` | `ROLE_ADMIN`, `ROLE_IT_ADMIN`, `hasRole` | Sidebar, gated pages |
| `src/lib/forbidden-error.ts` | `ForbiddenError` | API error handling |

During implementation a `grep -r` confirms no consumer is missed. The acceptance criteria includes a "grep returns nothing" check.

### 7. Open-CRM — translate-component replacement

`frontend/src/components/translate-dialog.tsx` and `frontend/src/components/translate-button.tsx` are deleted. Every usage site changes:

```tsx
// before
import { TranslateButton } from "@/components/translate-button";

<TranslateButton text={description} />

// after
import { TranslateButton } from "@open-elements/ui";
import { useTranslationConfig } from "@/lib/use-translation-config";
import { translateText } from "@/lib/api";

const { configured } = useTranslationConfig();
const S = t.translation;

<TranslateButton
  text={description}
  configured={configured}
  onTranslate={(text, lang) => translateText(text, lang)}
  translations={{
    button: S.translate,
    dialog: {
      title: S.title,
      loading: S.loading,
      error: S.error,
      copy: S.copy,
      copied: S.copied,
      close: S.close,
    },
  }}
/>
```

The five usage sites identified by spec 092 are: `company-detail.tsx`, `contact-detail.tsx`, `company-comments.tsx`, `contact-comments.tsx`, `task-comments.tsx`. During implementation, `grep -r "translate-button\|translate-dialog"` confirms the full set.

To avoid repeating the props bundle five times, a thin local helper can be added to `frontend/src/components/` if the call-site noise gets annoying — but only if it carries real value (e.g., conditional rendering tied to the description language). Otherwise the prop bundle stays inline. Decided during implementation by the smallest diff that reads cleanly.

### 8. Open-CRM — dependency bumps

```diff
-"@open-elements/ui": "^0.6.0",
+"@open-elements/ui": "^0.<new-minor>.0",
-"@open-elements/nextjs-app-layer": "^0.1.0",
+"@open-elements/nextjs-app-layer": "^0.2.0",
```

Concrete versions are filled in at implementation time once the lib releases are published.

### 9. Release ordering

The lib releases must precede the Open-CRM PR being merged:

1. `@open-elements/ui` release (TranslateDialog + TranslateButton) → `0.7.0` (or whatever the next minor is at the time).
2. `@open-elements/nextjs-app-layer` release → `0.2.0`.
3. Open-CRM PR consumes both, executes the file moves and wrapper deletions.

The Open-CRM PR can be opened before step 1+2 are published — local development uses `pnpm link` or `pnpm overrides` against the local lib checkouts until the registry versions exist. CI is only green once the deps are reachable from npm.

### 10. Verification

After the lib releases and the app PR:

1. `pnpm install && pnpm test && pnpm build` from `frontend/` succeeds.
2. `grep -r "packages/nextjs-app-layer\|/api-keys\|/webhooks" frontend/src` returns only the new `/admin/api-keys` and `/admin/webhooks` paths.
3. `grep -r "@/components/session-provider\|@/components/forbidden-page\|@/components/add-comment-dialog\|@/components/translate-button\|@/components/translate-dialog\|@/lib/roles\|@/lib/forbidden-error" frontend/src` returns nothing.
4. `.next/server/middleware-manifest.json` still contains the canonical matcher (regression guard for `e89b66f`).
5. Manual browser smoke test of:
   - `/admin/api-keys` and `/admin/webhooks` — full CRUD parity.
   - `/api-keys` and `/webhooks` return 404.
   - Every admin sidebar link is highlighted as active when on its page.
   - TranslateButton appears next to a company description when the backend feature is configured, hides when it is not, opens the dialog on click, the dialog completes a translation and copy works.

## Dependencies

- Spec 099 (workspace → npm dep switch) must be implemented and merged first. Otherwise this spec would need to also rip out the workspace, which is out of scope.
- `@open-elements/ui` and `@open-elements/nextjs-app-layer` must both release the planned versions before the Open-CRM PR can be merged.

## Security considerations

None new. No new data flows, no new auth surface, no GDPR-relevant change. The role guard on the moved admin pages is identical (same lib factories), and the translate components do not access the session or any personal data outside the source text they are explicitly given.

## Open questions

- **`useTranslationConfig` drift:** Spec 098 said it should live in `@open-elements/nextjs-app-layer`. It does not. This spec acknowledges the drift but does not fix it. A separate small spec ("Finish phase-098 migration: useTranslationConfig") can pick it up later if desired.
