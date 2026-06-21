# Design: Remove Comment Count Column

## GitHub Issue

—

## Summary

The comment count column in the company and contact list tables provides no actionable value to users. The information is only useful in the detail view where comments are actually displayed and managed. This spec removes the comment count column from both list tables and removes the comment count option from CSV exports.

## Goals

- Remove the comment count column from the company list table
- Remove the comment count column from the contact list table
- Remove the comment count option from CSV exports

## Non-goals

- Removing `commentCount` from the backend DTOs — the field is still used in detail views, and there is no separate detail DTO
- Removing the comment action button (MessageSquarePlus) from table rows — it stays
- Changing the detail view — comment count remains in the heading
- Optimizing away the `commentCount` DB query in list responses — accepted trade-off since there is no separate list DTO
- Changing print views — they already don't include comment count

## Technical Approach

### Frontend

**Company list table** (`frontend/src/components/company-list.tsx`):
- Remove the `<TableHead>{S.columns.comments}</TableHead>` column header
- Remove the `<TableCell>{company.commentCount}</TableCell>` data cell

**Contact list table** (`frontend/src/components/contact-list.tsx`):
- Remove the `<TableHead>{S.columns.comments}</TableHead>` column header
- Remove the `<TableCell>{contact.commentCount}</TableCell>` data cell

**i18n**: The `columns.comments` translation key can be removed if no longer used elsewhere.

### Backend

**Company CSV export** (`CompanyExportColumn.java`):
- Remove the `COMMENT_COUNT` enum value

**Contact CSV export** (`ContactExportColumn.java`):
- Remove the `COMMENT_COUNT` enum value

### Not Changed

- `CompanyDto` / `ContactDto` — `commentCount` field remains (used by detail views)
- `CompanyService` / `ContactService` — `commentCount` computation remains
- Print views — already don't include comment count
- Comment action buttons in table rows — remain functional

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/company-list.tsx` | Remove comment count column header and cell |
| `frontend/src/components/contact-list.tsx` | Remove comment count column header and cell |
| `backend/.../company/CompanyExportColumn.java` | Remove `COMMENT_COUNT` enum value |
| `backend/.../contact/ContactExportColumn.java` | Remove `COMMENT_COUNT` enum value |
| `frontend/src/lib/i18n/en.ts` | Remove `columns.comments` key if unused |
| `frontend/src/lib/i18n/de.ts` | Remove `columns.comments` key if unused |
