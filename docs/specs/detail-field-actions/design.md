# Design: Detail Field Actions

## GitHub Issue

—

## Summary

Add interactive action icons to fields in the Company and Contact detail views. Fields can have copy-to-clipboard, open-URL-in-new-tab, mailto:, and tel: actions. Icons appear on hover only when the field has a value. The currently duplicated `DetailField` component is extracted into a shared component with boolean props to enable each action type.

## Goals

- Let users quickly copy field values to the clipboard
- Let users open URLs, send emails, and initiate phone calls directly from the detail view
- Extract `DetailField` into a shared, reusable component

## Non-goals

- Adding actions to list table cells
- Adding actions to edit forms
- Adding actions to print views

## Technical Approach

### Shared DetailField Component

Extract the duplicated `DetailField` from `company-detail.tsx` and `contact-detail.tsx` into a shared component at `frontend/src/components/detail-field.tsx`.

**Props:**

```typescript
interface DetailFieldProps {
  readonly label: string;
  readonly value: string | null;
  readonly copyable?: boolean;    // Show copy-to-clipboard icon
  readonly linkable?: boolean;    // Show open-in-new-tab icon (value is treated as URL)
  readonly mailable?: boolean;    // Show mailto: icon (value is treated as email)
  readonly callable?: boolean;    // Show tel: icon (value is treated as phone number)
  readonly multiline?: boolean;   // Render value with line breaks (for address block)
}
```

**Rationale:** Boolean props keep the API simple and declarative. Each detail view controls which actions are active per field. This avoids complex configuration objects and keeps the component easy to use.

### Action Icons

Use icons from `lucide-react` (already a project dependency):
- **Copy:** `Copy` icon → on click, copies value to clipboard via `navigator.clipboard.writeText()`, then briefly switches to `Check` icon for ~2 seconds
- **Open URL:** `ExternalLink` icon → on click, opens `value` in new tab via `window.open(url, '_blank')`
- **Mailto:** `Mail` icon → on click, navigates to `mailto:{value}`
- **Tel:** `Phone` icon → on click, navigates to `tel:{value}`

### Icon Visibility

- Icons are **hidden by default** and appear on hover over the `DetailField` container
- Icons are **never shown** when the value is null/empty (the field displays "—")
- Icons are rendered as small buttons (`text-oe-gray-mid hover:text-oe-dark`) inline after the value text

### URL Normalization

For the `linkable` action: if the value does not start with `http://` or `https://`, automatically prepend `https://` before opening. This handles cases where LinkedIn URLs or websites are stored without protocol prefix.

### Field-to-Action Mapping

**Company detail:**

| Field | copyable | linkable | mailable | callable |
|-------|----------|----------|----------|----------|
| Email | ✓ | | ✓ | |
| Website | ✓ | ✓ | | |
| Address (Spec 036 block) | ✓ | | | |

**Contact detail:**

| Field | copyable | linkable | mailable | callable |
|-------|----------|----------|----------|----------|
| Email | ✓ | | ✓ | |
| LinkedIn | ✓ | ✓ | | |
| Phone | ✓ | | | ✓ |

All other fields (position, gender, birthday, language, company link) have no actions.

### Address Copy Behavior

The merged address block (Spec 036) copies the full multi-line text with newline characters. For example:

```
Musterstraße 42
12345 Berlin
Deutschland
```

The `multiline` prop on `DetailField` enables line-break rendering. When `copyable` is true on a multiline field, the raw multi-line string is copied.

### Files Affected

**Frontend (new):**
- `frontend/src/components/detail-field.tsx` — shared DetailField component with action icons

**Frontend (modified):**
- `frontend/src/components/company-detail.tsx` — import shared DetailField, remove local definition, add action props
- `frontend/src/components/contact-detail.tsx` — import shared DetailField, remove local definition, add action props

### Dependencies

- **Spec 036 (Detail view cleanup):** This spec builds on the merged address block and removed name fields. The address `DetailField` uses `multiline` + `copyable`.
- **lucide-react:** Already available, provides `Copy`, `Check`, `ExternalLink`, `Mail`, `Phone` icons.

## Open Questions

None — all details resolved during design discussion.
