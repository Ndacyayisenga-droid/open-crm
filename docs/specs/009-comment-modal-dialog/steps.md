# Implementation Steps: Comment Modal Dialog

## Step 1: Add contact comment API functions

- [x] Add `getContactComments(contactId, page)` to `api.ts`
- [x] Add `createContactComment(contactId, data)` to `api.ts`

## Step 2: Add i18n key for dialog title

- [x] Add `addTitle` key to `companies.comments` in `de.ts` and `en.ts`

## Step 3: Create AddCommentDialog component

- [x] Create `frontend/src/components/add-comment-dialog.tsx` — reusable modal with textarea, Enter-to-submit, Shift+Enter for newline, send button, error handling

## Step 4: Refactor CompanyComments to use dialog

- [x] Remove inline textarea and send button from `company-comments.tsx`
- [x] Add "Add Comment" button in CardHeader
- [x] Integrate AddCommentDialog

## Step 5: Create ContactComments component

- [x] Create `frontend/src/components/contact-comments.tsx` — mirrors CompanyComments but uses contact API functions

## Step 6: Update ContactDetail to use real comments

- [x] Replace comments placeholder in `contact-detail.tsx` with `ContactComments` component

## Step 7: Update tests

- [x] Rewrite `company-comments.test.tsx` for modal flow
- [x] Update `company-detail.test.tsx` for new comment section UI
- [x] Update `contact-detail.test.tsx` for real comments section

**Acceptance criteria:**
- [x] All 117 tests pass
- [x] TypeScript compiles without errors
- [x] No inline textarea in comment sections
- [x] Modal opens on button click with textarea and submit button
