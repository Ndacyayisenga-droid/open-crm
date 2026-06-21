# Design: Print View A4 Fit

## GitHub Issue

—

## Summary

The Company and Contact print views render HTML tables that get cut off when printing in DIN A4 portrait orientation. There are no `@media print` styles to constrain or scale the table width to fit the page. Users see truncated output where right-side columns are missing from the printed page.

## Reproduction

1. Open the Company or Contact list view
2. Click the print button (opens a new tab with the print view)
3. In the browser print dialog, select DIN A4 portrait orientation
4. Observe that the table is wider than the page — right columns are cut off

## Root Cause Analysis

The print pages render standard HTML tables with no print-specific width constraints. The table uses `overflow-x-auto` on screen (which enables horizontal scrolling) but this has no effect in print context. The `@media print` rules in `globals.css` only handle row page-break prevention and image color accuracy — there is no scaling or width management.

Additionally, the `TableCell` component uses `whitespace-nowrap` by default, preventing text from wrapping and making columns wider than necessary.

## Fix Approach

### 1. CSS scaling via `@media print`

Add print styles that scale the page content to fit within A4 portrait width. Use `transform: scale()` or the CSS `@page` directive combined with `width: 100%` table constraints. The simplest reliable approach:

- Set the table to `width: 100%` in print context
- Allow cell text to wrap (`white-space: normal` instead of `nowrap`)
- Use `@page { size: A4 portrait; margin: 15mm; }` to define the print area
- If the table still overflows, apply a CSS scale transform to the print container

**Rationale:** CSS scaling is preferred over restructuring the table layout because it preserves the visual design while guaranteeing the content fits. The trade-off of slightly smaller text is acceptable.

### 2. Remove Comment Count column from print

Remove the "Comments" column from both Company and Contact print tables. This column is not useful in a printed list and frees up horizontal space.

**Company print columns (after):** Logo | Name | Website | Contacts
**Contact print columns (after):** Photo | Name | Email | Company

### 3. Text wrapping for long content

Override `whitespace-nowrap` on table cells in print context so that long names, URLs, and email addresses wrap instead of forcing the table wider.

### 4. Repeating table header on page breaks

Add `thead { display: table-header-group; }` to the print styles so the column headers repeat on every printed page for multi-page tables.

### Files affected

- `frontend/src/app/globals.css` — add `@media print` rules (scaling, wrapping, header repeat, page size)
- `frontend/src/app/companies/print/page.tsx` — remove Comments column
- `frontend/src/app/contacts/print/page.tsx` — remove Comments column

## Regression Risk

- The `@media print` rules in `globals.css` apply globally — any other printable page would be affected. Currently only the two print views exist, so the risk is low.
- Removing `whitespace-nowrap` in print could cause very wide tables to become taller due to wrapping. This is the desired behavior.
- CSS scaling may produce slightly smaller text. This is accepted.

## Open Questions

None — all details resolved during design discussion.
