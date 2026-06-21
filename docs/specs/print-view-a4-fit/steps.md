# Implementation Steps: Print View A4 Fit

## Step 1: Add print CSS for A4 fit, text wrapping, and header repeat

- [x] Add `@page { size: A4 portrait; margin: 15mm; }` to `@media print` in `globals.css`
- [x] Add `thead { display: table-header-group; }` for header repeat on page breaks
- [x] Add `table { width: 100%; }` to constrain table width
- [x] Add `td, th { white-space: normal !important; word-break: break-word; }` for text wrapping

**Acceptance criteria:**
- [x] Print styles compile without errors
- [x] Table headers repeat on page breaks
- [x] Long text wraps instead of overflowing

**Related behaviors:** Table fits A4 portrait, Text wrapping, Table header repeats on page breaks, Row page break prevention

---

## Step 2: Remove Comments column from print views

- [x] Remove Comments header and cell from `companies/print/page.tsx`
- [x] Remove Comments header and cell from `contacts/print/page.tsx`

**Acceptance criteria:**
- [x] Company print shows: Logo, Name, Website, Contacts
- [x] Contact print shows: Photo, Name, Email, Company
- [x] No Comments column in either print view
- [x] TypeScript compiles successfully
- [x] All existing tests pass

**Related behaviors:** Company print table has no Comments column, Contact print table has no Comments column, Logos and photos remain
