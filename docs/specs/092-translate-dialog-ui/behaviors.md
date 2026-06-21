# Behaviors: Extract TranslateDialog to @open-elements/ui

## Component Rendering

### Dialog opens with loading state

- **Given** `open` is `true` and `sourceText` is non-empty
- **When** the dialog mounts
- **Then** `onTranslate` is called with `sourceText` and the current UI language, and a loading indicator is shown

### Dialog shows translated text on success

- **Given** the dialog is open and `onTranslate` resolved with `{ translatedText: "Hello" }`
- **When** the loading completes
- **Then** the loading indicator disappears and "Hello" is displayed with preserved line breaks

### Dialog shows error on failure

- **Given** the dialog is open and `onTranslate` rejected with an error
- **When** the loading completes
- **Then** the loading indicator disappears and the `translations.error` message is shown

### Dialog re-triggers translation on reopen

- **Given** the dialog was previously opened, showed a result, and was closed
- **When** the dialog is opened again with the same `sourceText`
- **Then** `onTranslate` is called again and the loading state is shown

## Target Language

### Target language derived from useLanguage

- **Given** the UI language is set to German (`de`)
- **When** the dialog opens
- **Then** `onTranslate` is called with `targetLanguage` = `"de"`

### Target language switches with UI language

- **Given** the UI language is set to English (`en`)
- **When** the dialog opens
- **Then** `onTranslate` is called with `targetLanguage` = `"en"`

## Copy to Clipboard

### Copy button copies translated text

- **Given** the dialog shows a successfully translated text
- **When** the user clicks the copy button
- **Then** the translated text is copied to the clipboard and the button shows a check icon with the `translations.copied` label

### Copy button resets after feedback

- **Given** the copy button shows the check icon feedback
- **When** 2 seconds pass
- **Then** the button reverts to the copy icon with the `translations.copy` label

### Copy button disabled during loading

- **Given** the translation is still loading
- **When** the dialog is displayed
- **Then** the copy button is disabled

### Copy button disabled on error

- **Given** the translation failed with an error
- **When** the dialog is displayed
- **Then** the copy button is disabled

## Close Behavior

### Close button closes dialog

- **Given** the dialog is open
- **When** the user clicks the close button
- **Then** `onOpenChange(false)` is called

### Dialog closable during loading

- **Given** the translation is still loading
- **When** the user clicks close
- **Then** the dialog closes and the in-flight request is cancelled (no state update after unmount)

## Translations Prop

### All labels rendered from translations prop

- **Given** the dialog is rendered with a `translations` prop
- **When** the dialog is displayed
- **Then** the title uses `translations.title`, loading text uses `translations.loading`, error text uses `translations.error`, copy button uses `translations.copy`/`translations.copied`, close button uses `translations.close`

## Integration in open-crm

### Import from @open-elements/ui

- **Given** the `TranslateDialog` has been migrated to `@open-elements/ui`
- **When** open-crm imports `TranslateDialog`
- **Then** the import path is `@open-elements/ui`, not `@/components/translate-dialog`

### Local file removed

- **Given** the migration is complete
- **When** checking the open-crm frontend source
- **Then** `frontend/src/components/translate-dialog.tsx` no longer exists

### onTranslate wired to existing API

- **Given** open-crm renders a `TranslateDialog`
- **When** the `onTranslate` prop is called
- **Then** it delegates to the existing `translateText` function from `@/lib/api`

### Existing translate buttons unchanged

- **Given** the migration is complete
- **When** a user clicks a translate button on a description or comment
- **Then** the dialog opens and behaves identically to before the migration
