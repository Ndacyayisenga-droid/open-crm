# Design: Print Functionality for Company and Contact List Tables

## GitHub Issue

_To be created_

## Prerequisites

This spec should be implemented after the filter-related specs are done, as the print page needs to reflect the final filter state:

| Spec | Dependency |
|------|-----------|
| 020 — Simplify company filters | Company filters finalized |
| 024 — Brevo origin filter | Brevo filter on both tables |
| 029 — Contact table columns | Merged Name column |
| 030 — Remove contact sorting | No sort parameter |
| 031 — Contact unified search | Single search field |

## Summary

The company and contact list tables cannot be printed usefully — the browser prints the sidebar, filters, buttons, and only the current page of 20 records. A "Print" button opens a new browser tab with a print-optimized page that loads ALL filtered records via pagination, displays them in a clean table with a title and active filter summary, and auto-triggers the browser print dialog.

## Goals

- Add a "Print" button to both company and contact list views
- Open a new tab with a dedicated print route
- Load all filtered data (not just one page) via paginated API calls
- Display a clean, print-optimized table with title and filter summary
- Auto-trigger the browser print dialog when data is loaded

## Non-goals

- PDF export (browser print dialog can save as PDF)
- CSV/Excel export (separate feature)
- Print styling for detail views
- New backend endpoints (reuse existing list endpoints with larger page size)

## Technical Approach

### Print routes

Create two new Next.js pages:

- `/companies/print` — print view for companies
- `/contacts/print` — print view for contacts

Both accept the same query parameters as the regular list endpoints.

### Print button

Add a "Print" button (with a Printer icon from lucide-react) to both list views, next to the existing "New Company" / "New Contact" button. Clicking it opens the print route in a new tab via `window.open()`, passing the current filter values as URL query parameters.

**Company list example:**
```
window.open(`/companies/print?name=${nameFilter}&brevo=${brevoFilter}&includeDeleted=${includeDeleted}`)
```

Only include parameters that have active values (skip empty/default ones).

### Data loading strategy

The print page loads all data by fetching pages sequentially with `size=250`:

```typescript
const allRecords = [];
let page = 0;
let lastPage = false;

while (!lastPage) {
  const result = await getCompanies({ ...filters, page, size: 250 });
  allRecords.push(...result.content);
  lastPage = result.last;
  page++;
}
```

**Rationale:** Using the existing paginated API with a larger page size (250 instead of 20) avoids new backend endpoints. Sequential loading is simple and reliable. For typical datasets (hundreds of records), this completes in 1-3 requests.

### Print page layout

```
┌──────────────────────────────────────────┐
│  Firmen                                  │  ← Title (h1)
│  Name: Acme · Brevo: Ja                 │  ← Active filters (muted text)
├──────────────────────────────────────────┤
│  [Logo] │ Name    │ Website │ Contacts │ │  ← Table (same columns as list)
│  ...    │ ...     │ ...     │ ...      │ │
│  ...    │ ...     │ ...     │ ...      │ │
└──────────────────────────────────────────┘
```

- **Title**: "Firmen" / "Kontakte" (from i18n, same as the list view title)
- **Filter summary**: Active filter values formatted as "Label: Value" separated by " · ". If no filters are active: "Keine Filter" (DE) / "No filters" (EN)
- **Table**: Same columns as the regular list view, including logo/photo thumbnails
- **No sidebar, no navigation, no buttons, no pagination**
- **Actions column is excluded** (delete/restore buttons make no sense on print)

### Print page states

1. **Loading**: Show a loading spinner and "Loading data..." message while fetching pages
2. **Loaded**: Render the table, then call `window.print()` via `useEffect` after render
3. **Error**: Show error message if data loading fails
4. **Empty**: Show "No records found" if the filtered result is empty (don't trigger print)

### Auto-trigger print dialog

After all data is loaded and the table is rendered:

```typescript
useEffect(() => {
  if (allRecords.length > 0 && !loading) {
    // Small delay to ensure rendering is complete
    setTimeout(() => window.print(), 300);
  }
}, [allRecords, loading]);
```

### Print CSS

The print pages use minimal CSS:

```css
@media print {
  /* Hide any remaining browser chrome */
  body { margin: 0; padding: 0; }
  /* Ensure table doesn't break mid-row */
  tr { page-break-inside: avoid; }
  /* Ensure images print */
  img { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}
```

### Filter summary formatting

Map each active filter to a human-readable label:

**Company filters:**
| Parameter | Label (EN) | Label (DE) | Value format |
|-----------|-----------|-----------|--------------|
| `name` | "Name" | "Name" | Raw value |
| `brevo=true` | "Brevo" | "Brevo" | "Yes" / "Ja" |
| `brevo=false` | "Brevo" | "Brevo" | "No" / "Nein" |
| `includeDeleted=true` | "Archived" | "Archiviert" | "Included" / "Eingeschlossen" |

**Contact filters:**
| Parameter | Label (EN) | Label (DE) | Value format |
|-----------|-----------|-----------|--------------|
| `search` | "Search" | "Suche" | Raw value |
| `companyId` | "Company" | "Firma" | Company name (needs lookup) |
| `language` | "Language" | "Sprache" | "DE" / "EN" |
| `brevo=true` | "Brevo" | "Brevo" | "Yes" / "Ja" |
| `brevo=false` | "Brevo" | "Brevo" | "No" / "Nein" |

### Translations (`en.ts`, `de.ts`)

Add under a shared or per-entity `print` section:

```
print.button: "Print" / "Drucken"
print.loading: "Loading data..." / "Daten werden geladen..."
print.noFilters: "No filters" / "Keine Filter"
print.noRecords: "No records found" / "Keine Datensätze gefunden"
print.filterYes: "Yes" / "Ja"
print.filterNo: "No" / "Nein"
print.filterArchived: "Included" / "Eingeschlossen"
```

## Files Affected

| File | Change |
|------|--------|
| `frontend/src/app/companies/print/page.tsx` | **New** — company print page |
| `frontend/src/app/contacts/print/page.tsx` | **New** — contact print page |
| `frontend/src/components/company-list.tsx` | Add print button |
| `frontend/src/components/contact-list.tsx` | Add print button |
| `frontend/src/lib/i18n/en.ts` | Add print translations |
| `frontend/src/lib/i18n/de.ts` | Add print translations |
| `frontend/src/app/globals.css` | Add `@media print` rules |

## Open Questions

None.
