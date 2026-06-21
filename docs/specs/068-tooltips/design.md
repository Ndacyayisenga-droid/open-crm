# Design: Tooltips

## GitHub Issue

—

## Summary

Icon-only action buttons across the app lack proper tooltips. Some have native HTML `title` attributes, some have none. This spec introduces the shadcn/ui Tooltip component and wraps all icon-only action buttons with it for a consistent, styled tooltip experience. On touch devices, tooltips are not shown — the native `title` attribute is removed to avoid duplication.

## Goals

- Install and configure the shadcn/ui Tooltip component
- Add tooltips to all icon-only action buttons across the app
- Use i18n-aware tooltip text (DE/EN)
- Replace existing native `title` attributes with the Tooltip component

## Non-goals

- Tooltips on non-action elements (labels, text, navigation items)
- Custom tooltip styling beyond shadcn/ui defaults
- Long-press tooltips on touch devices

## Technical Approach

### Install shadcn/ui Tooltip

Add the Tooltip component via shadcn/ui CLI or manually create `components/ui/tooltip.tsx`. This provides `TooltipProvider`, `Tooltip`, `TooltipTrigger`, and `TooltipContent`.

**TooltipProvider** must wrap the app — add it to the root layout or a shared provider component.

### Tooltip Usage Pattern

Replace existing `title` attributes with the Tooltip wrapper:

```tsx
// Before
<Button variant="ghost" size="icon" title={S.detail.edit}>
  <Pencil className="h-4 w-4" />
</Button>

// After
<Tooltip>
  <TooltipTrigger asChild>
    <Button variant="ghost" size="icon">
      <Pencil className="h-4 w-4" />
    </Button>
  </TooltipTrigger>
  <TooltipContent>{S.detail.edit}</TooltipContent>
</Tooltip>
```

**Rationale for shadcn/ui Tooltip over native `title`:** Native `title` tooltips have inconsistent styling across browsers, cannot be styled, appear with a long delay, and have poor accessibility. The shadcn/ui Tooltip uses Radix UI under the hood, which provides proper ARIA attributes, consistent timing, and styled appearance.

### Buttons to Update

**Company list** (`company-list.tsx`):
- Edit button (Pencil)
- Add comment button (MessageSquarePlus)
- Create task button (CheckSquare)
- Restore button (RotateCcw)
- Delete button (Trash2)

**Contact list** (`contact-list.tsx`):
- Edit button (Pencil)
- Add comment button (MessageSquarePlus)
- Create task button (CheckSquare)
- Delete button (Trash2)

**Task list** (`task-list.tsx`):
- Edit button (Pencil)

**Tag list** (`tag-list.tsx`):
- Edit button (Pencil) — currently missing `title`, needs i18n key
- Delete button (Trash2) — currently missing `title`, needs i18n key

**Company detail** (`company-detail.tsx`):
- Edit button
- Delete button
- Create task button
- Show employees button (if icon-only)

**Contact detail** (`contact-detail.tsx`):
- Edit button
- Delete button
- Create task button

**Task detail** (`task-detail.tsx`):
- Edit button
- Delete button

**Detail field actions** (`detail-field.tsx`):
- Copy button
- Open/Link button (ExternalLink)
- Email button (Mail)
- Call button (Phone)

**Comment delete** (`company-comments.tsx`, `contact-comments.tsx`):
- Delete button (X icon)

**Sidebar** (`sidebar.tsx`):
- Logout button (LogOut)
- Mobile hamburger menu (Menu) — needs i18n key

### i18n

Most tooltip texts already exist as `title` attribute values. New keys needed:

| Key | EN | DE |
|-----|----|----|
| `tags.actions.edit` | "Edit" | "Bearbeiten" |
| `tags.actions.delete` | "Delete" | "Löschen" |
| `sidebar.menu` | "Menu" | "Menü" |

### Touch Device Handling

The shadcn/ui Tooltip (Radix UI) does not show on touch-only devices by default — it requires hover. This is the desired behavior. The native `title` attributes are removed (replaced by the Tooltip component) so there is no duplicate tooltip.

## Key Files

| File | Change |
|------|--------|
| `frontend/src/components/ui/tooltip.tsx` | New: shadcn/ui Tooltip component |
| `frontend/src/app/layout.tsx` | Add TooltipProvider wrapper |
| `frontend/src/components/company-list.tsx` | Wrap 5 action buttons with Tooltip |
| `frontend/src/components/contact-list.tsx` | Wrap 4 action buttons with Tooltip |
| `frontend/src/components/task-list.tsx` | Wrap 1 action button with Tooltip |
| `frontend/src/components/tag-list.tsx` | Wrap 2 action buttons with Tooltip, add i18n keys |
| `frontend/src/components/company-detail.tsx` | Wrap action buttons with Tooltip |
| `frontend/src/components/contact-detail.tsx` | Wrap action buttons with Tooltip |
| `frontend/src/components/task-detail.tsx` | Wrap action buttons with Tooltip |
| `frontend/src/components/detail-field.tsx` | Wrap 4 action icons with Tooltip |
| `frontend/src/components/company-comments.tsx` | Wrap delete button with Tooltip |
| `frontend/src/components/contact-comments.tsx` | Wrap delete button with Tooltip |
| `frontend/src/components/sidebar.tsx` | Wrap logout and hamburger buttons with Tooltip |
| `frontend/src/lib/i18n/en.ts` | Add missing tooltip translations |
| `frontend/src/lib/i18n/de.ts` | Add missing tooltip translations |
