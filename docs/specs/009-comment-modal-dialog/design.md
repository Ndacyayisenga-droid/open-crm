# Design: Comment Modal Dialog

## GitHub Issue

— (to be created)

## Summary

The comment text field is currently displayed inline in the company detail view (and will be in the contact detail view too). This is distracting when users just want to read existing comments. The creation of new comments should be moved behind a modal dialog, triggered by a button in the comment section header. Additionally, the modal should support Slack-style keyboard shortcuts: Enter to submit, Shift+Enter for newlines.

This is a generic solution — the same modal component must be reusable for both company and contact detail views.

## Goals

- Move comment creation from inline textarea to a modal dialog
- Provide a reusable comment creation modal component for companies and contacts
- Implement Enter-to-submit and Shift+Enter-for-newline keyboard behavior
- Keep the existing comment list display (read-only) unchanged

## Non-goals

- Editing or deleting existing comments
- Changing the backend API
- Changing the comment data model
- Adding new comment features (e.g., attachments, mentions, formatting)

## Technical Approach

### Component Architecture

The current `CompanyComments` component contains both the comment list display and the inline creation form. This spec splits the creation form into a separate, generic modal dialog component.

```
                    ┌─────────────────────────────┐
                    │   CommentSection             │
                    │   (generic wrapper)           │
                    │                               │
                    │   ┌─────────────────────┐     │
                    │   │ Header: Title + Button│    │
                    │   └─────────────────────┘     │
                    │   ┌─────────────────────┐     │
                    │   │ Comment list (read)  │     │
                    │   │ + Load more          │     │
                    │   └─────────────────────┘     │
                    └─────────────────────────────┘
                                  │
                          opens on click
                                  ▼
                    ┌─────────────────────────────┐
                    │   AddCommentDialog            │
                    │   (modal)                     │
                    │                               │
                    │   Textarea (min-height)       │
                    │   Enter = submit              │
                    │   Shift+Enter = newline        │
                    │   Send button (disabled if     │
                    │   empty)                       │
                    └─────────────────────────────┘
```

**Rationale:** Extracting a generic component (rather than duplicating for company/contact) follows DRY and ensures consistent behavior across all entity types that support comments.

### Component Details

#### `AddCommentDialog`

A new reusable component that receives a callback for submitting the comment text. It does not know about companies or contacts — it only handles the UI and keyboard behavior.

**Props:**
- `open: boolean` — Controls dialog visibility
- `onOpenChange: (open: boolean) => void` — Callback for open/close state changes
- `onSubmit: (text: string) => Promise<void>` — Async callback that handles the actual API call
- `sending: boolean` — External sending state to disable the button during submission

**Internal behavior:**
- Textarea with fixed minimum height (3 rows / ~80px)
- Submit button disabled when text is empty (whitespace-only counts as empty)
- Submit button disabled while `sending` is true
- On successful submit (no error thrown by `onSubmit`): clear text, close dialog
- On error (exception from `onSubmit`): keep dialog open, show error AlertDialog on top, preserve text
- `onKeyDown` handler on textarea: if Enter without Shift → call submit, prevent default newline

#### Changes to `CompanyComments` (and future `ContactComments`)

- Remove the inline textarea and send button
- Add a button in the `CardHeader` next to the title that opens the `AddCommentDialog`
- The `handleSend` logic stays in the parent component (it knows the entity ID and API function)
- Pass `handleSend` as `onSubmit` to `AddCommentDialog`

### Keyboard Behavior

| Key Combination | Action |
|----------------|--------|
| Enter | Submit comment (if text is not empty) |
| Shift+Enter | Insert newline in textarea |
| Escape | Close modal (standard Radix Dialog behavior) |

**Rationale:** This matches the established pattern from Slack, Teams, and other chat-like interfaces. Users expect Enter to submit in comment/chat contexts.

### Button Placement

The "Add Comment" button is placed in the `CardHeader`, next to the section title "Kommentare" / "Comments". This keeps the action close to where new comments appear (top of the list, newest first).

### UI Components Used

- `Dialog` / `DialogContent` / `DialogHeader` / `DialogTitle` / `DialogFooter` from shadcn/ui — for the modal
- `Button` from shadcn/ui — for the trigger button and submit button
- Native `<textarea>` — for the comment input (consistent with current implementation)
- `AlertDialog` — for error display on top of the modal
- `Plus` or `MessageSquarePlus` icon from Lucide — for the trigger button

### Brand Guidelines

- Button: `bg-oe-green hover:bg-oe-green-dark text-white` (primary action)
- Textarea border: `border-oe-gray-light`, focus: `border-oe-green ring-oe-green`
- Dialog uses standard shadcn/ui styling (requires semantic CSS variables from Spec 008)
- Typography: body text in Lato, title in Montserrat

### i18n

Existing translation keys in `companies.comments` cover most needs. A new key may be needed for the dialog title (e.g., `addTitle` → "Kommentar hinzufügen" / "Add Comment"). The translations structure should be generic enough for reuse with contacts.

## Dependencies

- **Spec 008 (Global UI Styling Fixes)** must be implemented first — otherwise the modal dialog will render with a transparent background
- Contact detail view must exist before this spec is applied to contacts

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/add-comment-dialog.tsx` | **New** — Reusable modal dialog component |
| `frontend/src/components/company-comments.tsx` | Remove inline textarea, add button + dialog integration |
| `frontend/src/lib/i18n/de.ts` | Add dialog title translation |
| `frontend/src/lib/i18n/en.ts` | Add dialog title translation |
| `frontend/src/components/__tests__/company-comments.test.tsx` | Update tests for modal flow |

## Open Questions

- None