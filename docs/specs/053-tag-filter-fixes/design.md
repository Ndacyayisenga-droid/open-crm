# Design: Tag Filter Layout & Integration Fixes

## GitHub Issue

—

## Summary

The TagMultiSelect component renders selected tags as colored chips inside its trigger button, making it significantly taller than adjacent filter controls (Input, Select). This breaks the visual alignment of the filter row in company and contact list views. Additionally, the tag filter parameter (`tagIds`) is not forwarded to the print dialog or CSV export, causing those outputs to ignore active tag filters.

## Goals

- Make TagMultiSelect visually consistent with shadcn/ui Select components (same height, border, chevron)
- Replace chip rendering with compact summary text ("X Tags ausgewählt" / "X tags selected")
- Forward `tagIds` filter to print views (both company and contact)
- Forward `tagIds` filter to CSV export (both frontend URL builder and backend endpoint)
- Display active tag names in print view filter summary

## Non-goals

- Changing the popover dropdown content (checkbox list stays as-is)
- Adding tag columns to the print table or CSV output
- Separate compact/expanded variants of TagMultiSelect — one component, one behavior everywhere

## Fix Approach

### 1. TagMultiSelect — Compact trigger

**File:** `frontend/src/components/tag-multi-select.tsx`

Replace the chip-rendering trigger with a compact text-only trigger:

- **0 tags selected:** Show placeholder text `"Tags..."` in muted color (current behavior)
- **1+ tags selected:** Show `"X Tags ausgewählt"` / `"X tags selected"` (i18n)
- **Trigger styling:** Match shadcn/ui Select — fixed height (`h-10`), single line, `ChevronsUpDown` icon on the right, same border and padding
- **Remove:** All chip rendering, `removeTag()` function, `X` icon imports, `getContrastColor()` function, `isValidHex()` in trigger context (keep `isValidHex` for popover color dots)

**Rationale:** A single consistent component avoids maintaining two variants. The compact trigger works equally well in filter rows and forms — users see the count and open the popover to see/change details.

### 2. i18n — Tag count text

**Files:** `frontend/src/lib/i18n/de.ts`, `frontend/src/lib/i18n/en.ts`

Add translation key for the selected count text:

```
tags.selected: "{count} Tags ausgewählt" / "{count} tags selected"
```

The component formats this at runtime by replacing `{count}` with the actual number, or use a function-based translation.

### 3. Print views — Pass tagIds

**Files:**
- `frontend/src/components/company-list.tsx` — Add `tagIds` to print URL params
- `frontend/src/components/contact-list.tsx` — Add `tagIds` to print URL params
- `frontend/src/app/companies/print/page.tsx` — Read `tagIds` from search params, pass to API, show tag names in filter summary
- `frontend/src/app/contacts/print/page.tsx` — Read `tagIds` from search params, pass to API, show tag names in filter summary

**Tag name resolution in print view:** The print pages need to display tag names in the filter summary (e.g., "Tags: Marketing, VIP"). Since print pages already fetch data on load, they can also fetch the tag details for the given IDs using the existing `getTag()` API function.

**URL format:** `tagIds` are serialized as comma-separated values in a single param: `?tagIds=uuid1,uuid2`. The print page splits on comma to get the array.

### 4. CSV export — Pass tagIds

**Frontend files:**
- `frontend/src/lib/api.ts` — Add `tagIds` serialization to `getCompanyExportUrl()` and `getContactExportUrl()`
- `frontend/src/components/company-list.tsx` — Pass `tagIds` to export URL builder
- `frontend/src/components/contact-list.tsx` — Pass `tagIds` to export URL builder

**Backend files:**
- `backend/src/main/java/com/openelements/crm/company/CompanyController.java` — Add `@RequestParam(required = false) List<UUID> tagIds` to `exportCsv()` method
- `backend/src/main/java/com/openelements/crm/company/CompanyService.java` — Add `tagIds` parameter to `listAll()`, apply same Specification-based tag filter as in `list()`
- `backend/src/main/java/com/openelements/crm/contact/ContactController.java` — Add `@RequestParam(required = false) List<UUID> tagIds` to `exportCsv()` method
- `backend/src/main/java/com/openelements/crm/contact/ContactService.java` — Add `tagIds` parameter to `listAll()`, apply same Specification-based tag filter as in `list()`

**Rationale:** The export endpoints use `listAll()` (unpaginated) which has its own filter parameter set separate from `list()` (paginated). Both need the `tagIds` parameter added.

## Regression Risk

- **TagMultiSelect in forms:** The component is also used in company and contact create/edit forms. The chip removal was done via the `X` button on each chip. After this change, removal is done inside the popover by unchecking — this is a behavior change but consistent with how multi-select dropdowns work.
- **Print/export:** Adding a new parameter is additive — existing behavior unchanged when `tagIds` is not provided.

## Dependencies

- Spec 052 (Tag count & filter) must be implemented first — it introduces the `tagIds` parameter to the list endpoints
- Existing `getTag()` API function for resolving tag names in print view
