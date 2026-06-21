# Behaviors: Extract DeleteConfirmDialog and DetailField to @open-elements/ui

## DeleteConfirmDialog — Confirmation Flow

### Dialog shows title and description

- **Given** `open` is `true` and no `error` is set
- **When** the dialog renders
- **Then** the title and description are displayed with confirm and cancel buttons

### Confirm triggers async callback

- **Given** the dialog is open and no error is set
- **When** the user clicks the confirm button
- **Then** `onConfirm` is called and the confirm button shows a loading spinner and is disabled

### Confirm button re-enables after success

- **Given** `onConfirm` was called and resolved successfully
- **When** the Promise resolves
- **Then** the loading spinner disappears and the button returns to its normal state

### Confirm button re-enables after failure

- **Given** `onConfirm` was called and rejected with an error
- **When** the Promise rejects
- **Then** the loading spinner disappears and the button returns to its normal state

### Cancel remains clickable during loading

- **Given** `onConfirm` is in progress (loading state)
- **When** the user clicks cancel
- **Then** the dialog closes via `onOpenChange(false)`

### Double-click prevention

- **Given** the confirm button was already clicked and loading is in progress
- **When** the user clicks the confirm button again
- **Then** nothing happens — the button is disabled

## DeleteConfirmDialog — Error State

### Error replaces description

- **Given** the `error` prop is set to a non-null string
- **When** the dialog renders
- **Then** the error text is shown instead of the description, and only the cancel button is displayed

### Error title overrides title

- **Given** `error` is set and `errorTitle` is provided
- **When** the dialog renders
- **Then** `errorTitle` is shown as the dialog title instead of `title`

## DeleteConfirmDialog — Close Behavior

### Cancel closes dialog

- **Given** the dialog is open in normal state
- **When** the user clicks cancel
- **Then** `onOpenChange(false)` is called

### Cancel closes error state

- **Given** the dialog is showing an error
- **When** the user clicks cancel
- **Then** `onOpenChange(false)` is called

## DetailField — Display

### Label and value rendered

- **Given** a `label` and non-null `value` are provided
- **When** the component renders
- **Then** the label is shown as `text-oe-gray-mid` and the value as `text-oe-black`

### Null value shows dash

- **Given** `value` is `null`
- **When** the component renders
- **Then** "—" is displayed instead of the value

### Empty string shows dash

- **Given** `value` is an empty string
- **When** the component renders
- **Then** "—" is displayed instead of the value

### Multiline preserves line breaks

- **Given** `multiline` is `true` and value contains newlines
- **When** the component renders
- **Then** the value is displayed with `whitespace-pre-line` preserving line breaks

### Children override value

- **Given** `children` are provided
- **When** the component renders
- **Then** the children are rendered instead of the value text

## DetailField — Action Icons

### Copy action with tooltip

- **Given** `copyable` is `true` and a `translations` prop with `copy` and `copied` is provided
- **When** the component renders
- **Then** a copy icon button is shown with the `translations.copy` text as tooltip

### Copy feedback

- **Given** the user clicks the copy button
- **When** the text is copied to clipboard
- **Then** the icon changes to a check icon with `translations.copied` as tooltip for 2 seconds, then reverts

### Open link action with tooltip

- **Given** `linkable` is `true` and `translations.open` is provided
- **When** the component renders
- **Then** an external link icon button is shown with the `translations.open` text as tooltip

### Open link adds protocol

- **Given** `linkable` is `true` and the value does not start with `http://` or `https://`
- **When** the user clicks the open button
- **Then** the URL is opened with `https://` prepended

### Email action with tooltip

- **Given** `mailable` is `true` and `translations.email` is provided
- **When** the component renders
- **Then** a mail icon button is shown with the `translations.email` text as tooltip

### Email opens mailto

- **Given** `mailable` is `true`
- **When** the user clicks the email button
- **Then** `mailto:{value}` is triggered

### Call action with tooltip

- **Given** `callable` is `true` and `translations.call` is provided
- **When** the component renders
- **Then** a phone icon button is shown with the `translations.call` text as tooltip

### Call opens tel

- **Given** `callable` is `true`
- **When** the user clicks the call button
- **Then** `tel:{value}` is triggered

### No actions when value is null

- **Given** `value` is `null` and `copyable` is `true`
- **When** the component renders
- **Then** no action icons are shown

## DetailField — Translations Default

### Default English tooltips when translations omitted

- **Given** no `translations` prop is provided
- **When** the component renders with `copyable` enabled
- **Then** the tooltip shows "Copy" (English default)

### Custom translations override defaults

- **Given** `translations` prop is provided with `copy: "Kopieren"`
- **When** the component renders with `copyable` enabled
- **Then** the tooltip shows "Kopieren"

## Integration in open-crm

### Import from @open-elements/ui

- **Given** the migration is complete
- **When** open-crm imports `DeleteConfirmDialog` or `DetailField`
- **Then** the import path is `@open-elements/ui`

### Local files removed

- **Given** the migration is complete
- **When** checking the open-crm frontend source
- **Then** `frontend/src/components/delete-confirm-dialog.tsx` and `frontend/src/components/detail-field.tsx` no longer exist

### Existing behavior unchanged

- **Given** the migration is complete
- **When** a user interacts with any delete confirmation or detail field in the application
- **Then** the behavior is identical to before the migration (with the addition of loading state on delete confirm)
