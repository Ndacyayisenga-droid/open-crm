# Implementation Steps: Comment Count Live Update

## Step 1: Fix CompanyComments count

- [x] In `frontend/src/components/company-comments.tsx`:
  - Add state: `const [displayCount, setDisplayCount] = useState(totalCount)`
  - Add effect to sync with prop: `useEffect(() => setDisplayCount(totalCount), [totalCount])`
  - In `handleSend`, after successful API call: `setDisplayCount((prev) => (prev !== undefined ? prev + 1 : undefined))`
  - Replace `totalCount` with `displayCount` in the heading

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Comment count increments after adding a comment
- [x] Count does not increment on API failure

**Related behaviors:** Count updates after adding a comment (company), Count stays unchanged on API failure (company), Count displays correctly when totalCount is undefined (company), Count resets when navigating to a different company

---

## Step 2: Fix ContactComments count

- [x] In `frontend/src/components/contact-comments.tsx`:
  - Same four changes as Step 1

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] Comment count increments after adding a comment
- [x] Count does not increment on API failure

**Related behaviors:** Count updates after adding a comment (contact), Count stays unchanged on API failure (contact), Count displays correctly when totalCount is undefined (contact), Count resets when navigating to a different contact

---

## Step 3: Frontend tests for company comment count

- [x] Add tests to `frontend/src/components/__tests__/company-comments.test.tsx`:
  - "should show totalCount in heading" — render with `totalCount={3}`, verify heading shows "Kommentare (3)"
  - "should increment count after adding a comment" — render with `totalCount={3}`, add comment successfully, verify heading shows "Kommentare (4)"
  - "should not increment count on API failure" — render with `totalCount={3}`, fail API call, verify heading still shows "Kommentare (3)"
  - "should not show count when totalCount is undefined" — render without `totalCount`, add comment successfully, verify heading shows "Kommentare" without parentheses

**Acceptance criteria:**
- [x] All new tests pass
- [x] All existing tests still pass

**Related behaviors:** Count updates after adding a comment (company), Count stays unchanged on API failure (company), Count displays correctly when totalCount is undefined (company)

---

## Step 4: Frontend tests for contact comment count

- [x] Create `frontend/src/components/__tests__/contact-comments.test.tsx` with tests for `ContactComments`:
  - "should show totalCount in heading"
  - "should increment count after adding a comment"
  - "should not increment count on API failure"
  - "should not show count when totalCount is undefined"

**Acceptance criteria:**
- [x] All new tests pass
- [x] All existing tests still pass

**Related behaviors:** Count updates after adding a comment (contact), Count stays unchanged on API failure (contact), Count displays correctly when totalCount is undefined (contact)

---

## Step 5: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — note live comment count update behavior

**Acceptance criteria:**
- [x] Documentation reflects the fix

**Related behaviors:** (none — documentation step)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Count updates after adding a comment (company) | Frontend | 1, 3 |
| Count stays unchanged on API failure (company) | Frontend | 1, 3 |
| Count displays correctly when totalCount is undefined (company) | Frontend | 1, 3 |
| Count resets when navigating to a different company | Frontend | 1, 3 |
| Count updates after adding a comment (contact) | Frontend | 2, 4 |
| Count stays unchanged on API failure (contact) | Frontend | 2, 4 |
| Count displays correctly when totalCount is undefined (contact) | Frontend | 2, 4 |
| Count resets when navigating to a different contact | Frontend | 2, 4 |
