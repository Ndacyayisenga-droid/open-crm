# Design: Add Comment and Edit Actions to Company and Contact List Tables

## GitHub Issue

_To be created_

## Summary

The actions column in both company and contact list tables currently only has a delete (or restore) button. Users must navigate to the detail view to edit a record or add a comment. This spec adds an edit button and a comment button directly in the table, allowing quick actions without leaving the list view.

## Goals

- Add an edit button that navigates to the edit page
- Add a comment button that opens the existing comment modal dialog
- Apply to both company and contact list tables
- Maintain consistent button order: Edit, Comment, Delete/Restore

## Non-goals

- Live-updating the comment count after adding a comment (updates on next page load)
- Adding more action types (export, duplicate, etc.)
- Changing the existing delete/restore behavior

## Technical Approach

### Button order and layout

The actions column shows three icon buttons in a row (left to right):

1. **Edit** — `Pencil` icon (lucide-react), navigates to edit page
2. **Comment** — `MessageSquarePlus` icon (lucide-react), opens comment modal
3. **Delete** / **Restore** — existing `Trash2` / `RotateCcw` icons

All buttons use the existing pattern: `variant="ghost" size="icon"` with `e.stopPropagation()` to prevent row click navigation.

For archived companies, the order is: Edit, Comment, Restore (instead of Delete).

### Actions column width

The current column width (`w-[100px]`) may need to increase to accommodate three buttons. Adjust to `w-[140px]` or use `w-auto` to let it size naturally.

### Frontend — Company list (`company-list.tsx`)

**Edit button:**
```tsx
<Button
  variant="ghost"
  size="icon"
  title={S.detail.edit}
  onClick={(e) => {
    e.stopPropagation();
    router.push(`/companies/${company.id}/edit`);
  }}
>
  <Pencil className="h-4 w-4 text-oe-blue" />
</Button>
```

**Comment button:**

Add state for the comment target:
```tsx
const [commentTarget, setCommentTarget] = useState<CompanyDto | null>(null);
const [commentSending, setCommentSending] = useState(false);
```

Button in the actions cell:
```tsx
<Button
  variant="ghost"
  size="icon"
  title={S.comments.add}
  onClick={(e) => {
    e.stopPropagation();
    setCommentTarget(company);
  }}
>
  <MessageSquarePlus className="h-4 w-4 text-oe-blue" />
</Button>
```

Render the `AddCommentDialog` (already exists as a reusable component):
```tsx
<AddCommentDialog
  open={commentTarget !== null}
  onOpenChange={(open) => { if (!open) setCommentTarget(null); }}
  onSubmit={async (text) => {
    await createCompanyComment(commentTarget!.id, { text });
    setCommentTarget(null);
  }}
  sending={commentSending}
  title={S.comments.addTitle}
  placeholder={S.comments.placeholder}
  sendLabel={S.comments.send}
  sendingLabel={S.comments.sending}
  errorTitle={S.comments.errorTitle}
  errorMessage={S.comments.errorGeneric}
/>
```

**Rationale:** The `AddCommentDialog` is entity-agnostic — it only needs an `onSubmit` callback. No wrapper component needed; the list component manages the state directly (same pattern as the delete confirm dialog).

### Frontend — Contact list (`contact-list.tsx`)

Same pattern as company list:
- Edit button navigates to `/contacts/${contact.id}/edit`
- Comment button opens `AddCommentDialog` with `createContactComment` as the submit handler

### Frontend — Imports

Add to both list components:
- `Pencil`, `MessageSquarePlus` from `lucide-react`
- `AddCommentDialog` from `@/components/add-comment-dialog`
- `createCompanyComment` / `createContactComment` from `@/lib/api`

### Icon colors

- Edit: `text-oe-blue` (consistent with restore button)
- Comment: `text-oe-blue`
- Delete: `text-oe-red` (existing)
- Restore: `text-oe-blue` (existing)

### Translations

No new translation keys needed — existing keys are reused:
- `S.detail.edit` — already exists for the edit button label
- `S.comments.add` / `S.comments.addTitle` / etc. — already exist for the comment dialog

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Add edit + comment buttons, comment dialog state, import AddCommentDialog |
| `frontend/src/components/contact-list.tsx` | Add edit + comment buttons, comment dialog state, import AddCommentDialog |
| `frontend/src/components/__tests__/company-list.test.tsx` | Test new action buttons |
| `frontend/src/components/__tests__/contact-list.test.tsx` | Test new action buttons |

## Open Questions

None.
