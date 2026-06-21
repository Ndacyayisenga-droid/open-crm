# Design: Comment Count Live Update

## GitHub Issue

---

## Summary

On company and contact detail pages, the "Comments (x)" heading displays a stale count after adding a new comment. The comment itself appears immediately in the list (optimistic prepend), but the count in the heading remains unchanged until the page is reloaded.

The root cause is that `totalCount` is passed as a read-only prop from the parent detail component and is never updated after a comment is successfully created.

## Root Cause Analysis

Both `CompanyComments` and `ContactComments` receive `totalCount` as an optional prop:

- `company-detail.tsx` passes `company.commentCount`
- `contact-detail.tsx` passes `contact.commentCount`

Inside the comment components, `totalCount` is rendered directly in the heading:

```tsx
{S.title}{totalCount !== undefined ? ` (${totalCount})` : ""}
```

When `handleSend` succeeds, only the local `comments` array is updated via `setComments((prev) => [newComment, ...prev])`. There is no mechanism to update the parent's entity data, so the displayed count stays stale.

## Fix Approach

Introduce a local `displayCount` state in each comment component, initialized from the `totalCount` prop. After a successful `createCompanyComment` / `createContactComment` API call, increment `displayCount` by 1.

### Why local state instead of a parent callback?

- `totalCount` is only displayed inside the comment component's heading --- nowhere else on the detail page uses it.
- A parent callback would require lifting state and adding a prop, which is unnecessary complexity for a display-only value.
- If `totalCount` changes from outside (e.g., navigating to a different entity), the local state re-initializes via the prop.

### Changes

**`frontend/src/components/company-comments.tsx`:**

1. Add state: `const [displayCount, setDisplayCount] = useState(totalCount);`
2. Sync with prop changes: `useEffect(() => setDisplayCount(totalCount), [totalCount]);`
3. In `handleSend`, after successful API call: `setDisplayCount((prev) => (prev !== undefined ? prev + 1 : undefined));`
4. In the heading, replace `totalCount` with `displayCount`.

**`frontend/src/components/contact-comments.tsx`:**

Same four changes.

### Important: increment only on success

The count is incremented inside the `try` block, after the API call resolves successfully. If the API call throws, the count remains unchanged. This was an explicit design decision to avoid showing an incorrect count when the comment was not actually persisted.

## Regression Risk

- Low. The change is isolated to two components and only affects a display value.
- Pagination logic (`hasMore`, `page`) is unaffected --- it relies on `result.last` from the API, not on the count.
- If a future feature adds comment deletion, `displayCount` will need to be decremented analogously.