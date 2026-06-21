# Implementation Steps: Company Logo Display Fix

## Step 1: Change CSS classes on logo images

- [x] In `company-list.tsx`: change `h-8 w-8 rounded object-cover` to `h-8 w-8 object-contain`
- [x] In `company-detail.tsx`: change `h-24 w-24 rounded object-cover` to `h-24 w-24 object-contain`
- [x] In `company-form.tsx`: change `h-16 w-16 rounded object-cover` to `h-16 w-16 object-contain` (2 occurrences: file preview and current logo)

**Acceptance criteria:**
- [x] Frontend builds successfully
- [x] All frontend tests pass (144/144)
- [x] Contact photos still use `object-cover` and `rounded-full`
