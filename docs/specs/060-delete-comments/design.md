# Design: Delete Comments

## GitHub Issue

—

## Summary

Comments on companies and contacts can currently only be created and viewed. Users need the ability to delete comments that are no longer relevant. The backend already provides a fully functional `DELETE /api/comments/{id}` endpoint (implemented and tested). This spec adds the frontend UI to expose that existing functionality: a red X icon on each comment, a confirmation dialog before deletion, and immediate count update after successful deletion.

## Goals

- Allow users to delete any comment on companies and contacts
- Show a confirmation dialog before deletion to prevent accidental removal
- Update the comment count immediately after deletion

## Non-goals

- Comment editing (backend supports it, but out of scope for this spec)
- Permission-based deletion (e.g., only own comments) — any logged-in user can delete any comment
- Soft-delete / undo / audit trail — comments are hard-deleted
- Backend changes — the DELETE endpoint already exists

## Technical Approach

### Backend

**No changes required.** The following already exists and is tested:

- `DELETE /api/comments/{id}` — returns `204 No Content` on success, `404` if not found
- `CommentController.delete()` delegates to `CommentService.delete()` which calls `commentRepository.deleteById(id)`
- Backend tests cover successful deletion and 404 for non-existent comments

### Frontend

**Delete button per comment:**
- A red `X` icon (lucide-react) positioned on each comment entry, next to the author/date line
- No permission check — visible and functional for all logged-in users

**Confirmation dialog:**
- Reuse the existing `DeleteConfirmDialog` component (`components/delete-confirm-dialog.tsx`)
- The dialog uses the established pattern: `AlertDialog` with red confirm button (`bg-oe-red hover:bg-oe-red-dark`)
- Dialog text asks the user to confirm the deletion

**API method:**
- Add `deleteComment(commentId: string)` to `api.ts`
- Calls `DELETE /api/comments/{id}` with auth headers (using existing `authFetch` pattern)

**State updates after successful deletion:**
- Remove the deleted comment from the local `comments` state array (filter by id)
- Decrement `displayCount` by 1 (mirrors the existing increment pattern from Spec 014)

**Rationale for reusing `DeleteConfirmDialog`:** The component is already used for company and contact deletion with consistent styling and error handling. Reusing it ensures visual consistency and avoids code duplication.

### i18n

Add translation keys for the comment delete dialog:

| Key | EN | DE |
|-----|----|----|
| `comments.deleteDialog.title` | "Delete Comment" | "Kommentar löschen" |
| `comments.deleteDialog.description` | "Do you really want to delete this comment? This action cannot be undone." | "Möchten Sie diesen Kommentar wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden." |
| `comments.deleteDialog.confirm` | "Delete" | "Löschen" |
| `comments.deleteDialog.cancel` | "Cancel" | "Abbrechen" |

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/company-comments.tsx` | Add X icon per comment, delete handler, confirmation dialog, count decrement |
| `frontend/src/components/contact-comments.tsx` | Same changes as company-comments |
| `frontend/src/lib/api.ts` | Add `deleteComment(commentId)` function |
| `frontend/src/lib/i18n/en.ts` | Add `comments.deleteDialog` translations |
| `frontend/src/lib/i18n/de.ts` | Add `comments.deleteDialog` translations |

## Security Considerations

- The backend already handles authentication via JWT — unauthenticated requests are rejected
- No authorization check (any authenticated user can delete any comment) — this is a deliberate design choice for this small-team CRM
- Hard delete is permanent — the confirmation dialog mitigates accidental deletion
