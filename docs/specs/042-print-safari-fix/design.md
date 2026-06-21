# Design: Print Safari Fix

## GitHub Issue

—

## Summary

The print view tables (Spec 037) are cut off in Safari when printing in DIN A4 portrait orientation. Table headers fit correctly, but data rows (`td`) are wider than the page and get clipped on the right side. The same print view works correctly in Chromium-based browsers (Brave, Chrome).

## Reproduction

1. Open the Company or Contact list view in Safari
2. Click the print button (opens a new tab with the print view)
3. In the Safari print dialog, observe the preview
4. The table headers fit on the page, but data rows extend beyond the right edge

**Environment:** Safari on macOS. Works correctly in Brave/Chrome.

## Root Cause Analysis

The `TableCell` component (shadcn/ui) applies `whitespace-nowrap` as a Tailwind CSS class directly on each `<td>` element. Spec 037 added a global `@media print` rule to override this:

```css
td, th { white-space: normal !important; word-break: break-word; }
```

Chromium browsers respect this `!important` override in print context. **Safari does not** — it continues to apply the Tailwind `whitespace-nowrap` class on `td` elements, preventing text from wrapping and causing the table to overflow the page width.

The `<th>` elements (headers) appear to be less affected because their content is short enough to fit without wrapping.

## Fix Approach

Instead of relying on a global CSS override that Safari ignores, apply the text-wrapping classes **directly on the `TableCell` components** in the print page files. By passing `className="whitespace-normal break-words"` to each `TableCell`, the Tailwind utility class is set at the component level, which Safari respects.

This approach overrides the default `whitespace-nowrap` from the `TableCell` component via Tailwind's `cn()` merge utility, which already deduplicates conflicting classes.

**Rationale:** A component-level fix is more reliable across browsers than a global CSS specificity battle. It also makes the print-specific styling explicit in the print page components rather than hidden in a global stylesheet.

The global `@media print` rules for `td, th` in `globals.css` can be kept as a safety net for any future print contexts, but the print pages no longer depend on them.

## Files Affected

- `frontend/src/app/companies/print/page.tsx` — add `className="whitespace-normal break-words"` to `TableCell` elements
- `frontend/src/app/contacts/print/page.tsx` — add `className="whitespace-normal break-words"` to `TableCell` elements

## Regression Risk

- Low. The change only affects the two print page components.
- Chromium browsers already wrap text correctly; adding the class explicitly does not change their behavior.
- Must be verified manually in Safari.
