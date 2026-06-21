# Behaviors: Admin routes, translate components, wrapper-file cleanup

## @open-elements/ui — TranslateDialog

### Initial render kicks off the translation

- **Given** `<TranslateDialog>` is rendered with `open=true`, `sourceText="Hallo"`, an `onTranslate` mock that resolves to `{ translatedText: "Hello" }`
- **When** the dialog mounts
- **Then** `onTranslate` is called exactly once with arguments `("Hallo", <currentLanguage>)`
- **And** the loading state (`Loader2` + `loading` label) is shown until the promise resolves
- **And** after resolution the translated text is rendered

### Target language follows `useLanguage()`

- **Given** the active `LanguageProvider` language is `"de"`
- **When** `<TranslateDialog>` triggers a translation
- **Then** `onTranslate` is called with `targetLanguage = "de"`

- **Given** the active language is `"en"` (or any non-`"de"` value)
- **When** the dialog triggers a translation
- **Then** `onTranslate` is called with `targetLanguage = "en"`

### Error state when `onTranslate` rejects

- **Given** `onTranslate` rejects with any error
- **When** the dialog mounts with `open=true`
- **Then** the `error` translation is shown in destructive color
- **And** the copy button is disabled

### Copy success flips the icon for two seconds

- **Given** the dialog shows a translated result
- **When** the user clicks the copy button
- **Then** `navigator.clipboard.writeText` is called with the translated text
- **And** the button label flips to `copied` with a `Check` icon
- **And** approximately two seconds later the label reverts to `copy` with a `Copy` icon

### Re-opening resets the dialog state

- **Given** the dialog has previously shown a translated result
- **When** `open` flips `false` then back to `true` with a new `sourceText`
- **Then** the loading state shows again
- **And** the previous translation is not displayed during the in-flight new request

### `open=false` does not trigger a translation

- **Given** `<TranslateDialog open={false}>` mounts
- **Then** `onTranslate` is **not** called
- **And** no fetch / loading state is shown

## @open-elements/ui — TranslateButton

### Hidden when `text` is empty or whitespace

- **Given** `<TranslateButton text="" configured={true} …>` renders
- **Then** the component returns `null` (no button, no tooltip)

- **Given** `<TranslateButton text="   " configured={true} …>` renders
- **Then** the component returns `null`

- **Given** `<TranslateButton text={null} configured={true} …>` renders
- **Then** the component returns `null`

### Hidden while the config probe is still in flight

- **Given** `<TranslateButton text="something" configured={null} …>` renders
- **Then** the component returns `null`

### Hidden when the backend feature is not configured

- **Given** `<TranslateButton text="something" configured={false} …>` renders
- **Then** the component returns `null`

### Renders the icon button when text is non-empty and config is `true`

- **Given** `<TranslateButton text="something" configured={true} …>` renders
- **Then** a `Languages` icon button is rendered
- **And** the button has an `aria-label` matching `translations.button`
- **And** a tooltip with the same label appears on hover/focus

### Clicking opens the dialog with the given text

- **Given** the button is rendered
- **When** the user clicks the button
- **Then** the embedded `TranslateDialog` opens with `sourceText` equal to the button's `text` prop
- **And** the `translations.dialog` prop is forwarded as the dialog's `translations` prop

### Icon size follows `size` prop

- **Given** `size="sm"`
- **Then** the rendered icon has classes `h-3.5 w-3.5`

- **Given** `size="md"` (or omitted — default)
- **Then** the rendered icon has classes `h-4 w-4`

## @open-elements/nextjs-app-layer — default-route changes

### `apiKeysPageMeta.defaultRoute` points to `/admin/api-keys`

- **Given** a consumer imports `apiKeysPageMeta` from `@open-elements/nextjs-app-layer@0.2.0`
- **Then** `apiKeysPageMeta.defaultRoute === "/admin/api-keys"`

### `webhooksPageMeta.defaultRoute` points to `/admin/webhooks`

- **Given** a consumer imports `webhooksPageMeta`
- **Then** `webhooksPageMeta.defaultRoute === "/admin/webhooks"`

### Other page metas are unchanged

- **Given** the new release
- **Then** `auditLogsPageMeta.defaultRoute === "/admin/audit-logs"`,
  `usersPageMeta.defaultRoute === "/admin/users"`,
  `serverStatusPageMeta.defaultRoute === "/admin/status"`,
  `bearerTokenPageMeta.defaultRoute === "/admin/token"`

## Open CRM — admin route move

### `/admin/api-keys` and `/admin/webhooks` render the page

- **Given** the migrated app, an authenticated IT-ADMIN session
- **When** the user navigates to `/admin/api-keys`
- **Then** the API-keys page renders (table, create button, etc.)

- **When** the user navigates to `/admin/webhooks`
- **Then** the webhooks page renders

### Old `/api-keys` and `/webhooks` URLs return 404

- **Given** the migrated app
- **When** the user navigates to `/api-keys` or `/webhooks`
- **Then** the response is the Next.js 404 page (no automatic redirect)

### Sidebar links point to the new URLs

- **Given** the migrated `CrmSidebar`
- **When** the user with `IT-ADMIN` opens the Admin sub-menu
- **Then** the `API Keys` and `Webhooks` `NavItem`s have `href="/admin/api-keys"` and `href="/admin/webhooks"`
- **And** while on `/admin/api-keys` the API-keys nav item is highlighted active
- **And** while on `/admin/webhooks` the Webhooks nav item is highlighted active

### Admin sub-menu auto-opens on any admin route

- **Given** the user is on `/admin/api-keys`
- **When** the layout renders
- **Then** the Admin `CollapsibleGroup` is open by default (was already so for other `/admin/*` routes; this scenario locks the consolidated behaviour)

### Role-gating is unchanged

- **Given** an authenticated non-IT-ADMIN user
- **When** they navigate to `/admin/api-keys` or `/admin/webhooks`
- **Then** `ForbiddenPage` is rendered server-side (same guard as before, just at a new path)

## Open CRM — wrapper-file removal

### Pass-through wrapper files are deleted

- **When** the migration is complete
- **Then** the following paths do not exist:
  - `frontend/src/components/session-provider.tsx`
  - `frontend/src/components/forbidden-page.tsx`
  - `frontend/src/components/add-comment-dialog.tsx`
  - `frontend/src/lib/roles.ts`
  - `frontend/src/lib/forbidden-error.ts`

### No consumer references a deleted path

- **When** running `grep -r "@/components/session-provider\|@/components/forbidden-page\|@/components/add-comment-dialog\|@/lib/roles\|@/lib/forbidden-error" frontend/src`
- **Then** zero matches are returned

### Equivalent symbols are reachable directly from the lib

- **Given** the migrated app
- **When** any consumer imports `SessionProvider`, `ForbiddenPage`, `AddCommentDialog`, `ROLE_ADMIN`, `ROLE_IT_ADMIN`, `hasRole`, or `ForbiddenError`
- **Then** the import resolves to `@open-elements/nextjs-app-layer`
- **And** the symbol is exported by that package at the new release

## Open CRM — translate-component replacement

### Local `translate-button.tsx` and `translate-dialog.tsx` are deleted

- **When** the migration is complete
- **Then** `frontend/src/components/translate-button.tsx` does not exist
- **And** `frontend/src/components/translate-dialog.tsx` does not exist

### All translate usages import from the ui-lib

- **When** running `grep -r "translate-button\|translate-dialog" frontend/src`
- **Then** zero matches are returned

- **Given** the migrated app
- **When** any consumer imports `TranslateButton`
- **Then** the import is from `@open-elements/ui`

### Translate button still appears when the feature is configured

- **Given** the backend translation feature is configured (`/api/translation-settings` returns `{ configured: true }`)
- **And** a company has a non-empty description
- **When** the user visits the company detail page
- **Then** the translate button is rendered next to the description

### Translate button is hidden when the feature is not configured

- **Given** `/api/translation-settings` returns `{ configured: false }`
- **When** any detail or comments view renders
- **Then** the translate button is not rendered anywhere on the page

### Clicking the translate button opens the dialog

- **Given** the translate button is visible
- **When** the user clicks it
- **Then** the translate dialog opens with the description text as `sourceText`
- **And** a `POST /api/translate` request is issued (the app's `translateText`) with the current UI language as target

## Open CRM — dependency bumps

### Both libs are on the new releases

- **Given** the migrated `frontend/package.json`
- **Then** `@open-elements/ui` matches the version that ships TranslateButton + TranslateDialog
- **And** `@open-elements/nextjs-app-layer` matches `^0.2.0` (or newer)
- **And** `pnpm-lock.yaml` reflects the matching resolutions

### `pnpm install && pnpm test && pnpm build` is green

- **When** running this command sequence from `frontend/`
- **Then** the exit code is `0`

### Middleware matcher regression guard

- **Given** the migrated frontend
- **When** `pnpm build` runs
- **Then** `.next/server/middleware-manifest.json` contains a matcher whose `originalSource` excludes `_next/static`, `_next/image`, `login`, `api/auth`, `api/logout`, and the asset extensions (regression guard for the production fix in `e89b66f`)

## Coolify deployment

### Static assets reach the browser unchanged

- **Given** the deployed migrated app
- **When** the browser loads any page
- **Then** all `/_next/static/*` requests succeed with the correct MIME types
- **And** no "Unexpected token '<'" or `ChunkLoadError` appears in the console

### All admin pages and the translate feature work in production

- **Given** an authenticated IT-ADMIN user in the deployed app
- **When** they click through `/admin/audit-logs`, `/admin/users`, `/admin/status`, `/admin/token`, `/admin/brevo`, `/admin/api-keys`, `/admin/webhooks`
- **Then** every page renders and behaves identically to before this spec

- **Given** any logged-in user on a company detail page with a description
- **When** the translation feature is configured on the backend
- **Then** the translate button appears, opens the dialog, completes a translation, and copies it to the clipboard
