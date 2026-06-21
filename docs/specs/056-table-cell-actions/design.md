# Design: Table Cell Inline Actions

## GitHub Issue

—

## Summary

The detail views already have inline action icons (copy, mailto, tel, open URL) next to field values via the `DetailField` component. The list table views currently only have row-level action buttons (edit, comment, delete) in a dedicated Actions column. This feature adds inline action icons to specific table cells in the company and contact list views, bringing the same quick-action convenience to the table.

## Goals

- Add inline action icons to specific table cells in company and contact lists
- Maintain row-click navigation to detail views alongside cell-level actions
- Follow the existing detail-view icon styling (small, light gray, darken on hover)

## Non-goals

- Adding actions to the Name column (row click handles navigation)
- Adding actions to columns not listed in scope (e.g., Brevo filter, tags)
- Changing the existing row-level action buttons (edit, comment, delete)
- Adding actions to print view or CSV export

## Technical Approach

### Action Mapping

**Company table (`company-list.tsx`):**

| Column | Actions | Icon(s) |
|--------|---------|---------|
| Website | Copy to clipboard, Open in new tab | `Copy` / `Check`, `ExternalLink` |
| Contacts (count) | Navigate to contact list filtered by company | `ExternalLink` |

**Contact table (`contact-list.tsx`):**

| Column | Actions | Icon(s) |
|--------|---------|---------|
| Email | Copy to clipboard, Send email | `Copy` / `Check`, `Mail` |
| Company | Copy company name, Open company details | `Copy` / `Check`, `ExternalLink` |

### Null handling

When a field value is null/empty, the cell shows a dash "—" with **no action icons**. Icons only appear when a value exists.

### Styling

Reuse the exact styling from the `DetailField` component (spec 040/043):

```
Icon size: h-3.5 w-3.5
Base color: text-oe-gray-light (#e8e6dc)
Hover color: hover:text-oe-dark (#020144)
Touch devices: [@media(pointer:coarse)]:text-oe-dark
Container: inline-flex gap-0.5
```

**Rationale:** Using the detail-view style (small, subtle) rather than the table action button style (larger, colored) keeps the cell actions unobtrusive and visually consistent with the detail views. The table row-level actions (edit, delete) remain visually distinct in their own column.

### Click behavior

All action icon buttons use `e.stopPropagation()` to prevent the row click from navigating to the detail view — identical to the existing edit/delete buttons in the Actions column.

### Implementation

The action icons are rendered inline within the `TableCell`, next to the value text. No new shared component is needed — the pattern is simple enough to implement directly in each cell:

```tsx
<TableCell>
  <span className="inline-flex items-center gap-1">
    <span className="text-oe-gray-mid">{company.website}</span>
    <span className="inline-flex gap-0.5 shrink-0">
      <button onClick={(e) => { e.stopPropagation(); copyToClipboard(company.website); }}>
        <Copy className="h-3.5 w-3.5 text-oe-gray-light hover:text-oe-dark ..." />
      </button>
      <button onClick={(e) => { e.stopPropagation(); openInTab(company.website); }}>
        <ExternalLink className="h-3.5 w-3.5 text-oe-gray-light hover:text-oe-dark ..." />
      </button>
    </span>
  </span>
</TableCell>
```

### Copy feedback

Same pattern as `DetailField`: after clicking copy, the `Copy` icon temporarily changes to a green `Check` icon for 2 seconds, then reverts. Each cell manages its own copy state independently.

### Navigation targets

- **Company → Contacts count:** Navigates to `/contacts?companyId={company.id}`
- **Contact → Company name:** Navigates to `/companies/{contact.companyId}` (detail page, not list)

## Dependencies

- Existing `DetailField` component styling (spec 040/043) as visual reference
- Existing `stopPropagation` pattern from table action buttons (spec 034)
- Lucide icons: `Copy`, `Check`, `ExternalLink`, `Mail`
