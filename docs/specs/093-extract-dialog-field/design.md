# Design: Extract DeleteConfirmDialog and DetailField to @open-elements/ui

## GitHub Issue

#7

## Summary

`DeleteConfirmDialog` and `DetailField` are two fully prop-driven components in open-crm that have no app-specific dependencies. They should be extracted to `@open-elements/ui` so other Open Elements applications can reuse them. During the migration, two targeted API improvements are made: `DetailField` gets a `translations` prop for its currently hardcoded tooltip texts, and `DeleteConfirmDialog` gets an async `onConfirm` with a built-in loading state.

## Goals

- Move both components into `@open-elements/ui` as reusable exports
- Make `DetailField` tooltip texts configurable via `translations` prop (currently hardcoded English)
- Make `DeleteConfirmDialog.onConfirm` async with automatic loading state (disabled button + spinner)
- Remove local copies from open-crm and update all imports

## Non-goals

- Refactoring either component's internal structure — that comes later once all consuming apps are migrated
- Migrating app-specific components (TranslateButton, ApiKeyList, TagList, WebhookList)
- Changing visual design or behavior beyond the two stated API improvements

## Technical Approach

### DeleteConfirmDialog

**Current API:**
```typescript
interface DeleteConfirmDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly title: string;
  readonly description: string;
  readonly confirmLabel: string;
  readonly cancelLabel: string;
  readonly onConfirm: () => void;
  readonly error?: string | null;
  readonly errorTitle?: string;
}
```

**New API in @open-elements/ui:**
```typescript
interface DeleteConfirmDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly title: string;
  readonly description: string;
  readonly confirmLabel: string;
  readonly cancelLabel: string;
  readonly onConfirm: () => Promise<void>;
  readonly error?: string | null;
  readonly errorTitle?: string;
}
```

**Changes:**
- `onConfirm` changes from `() => void` to `() => Promise<void>`
- Component manages internal `loading` state: sets `true` before calling `onConfirm`, sets `false` after resolution/rejection
- While loading: confirm button is disabled and shows a spinner icon
- Cancel button remains clickable during loading (user can abort the visual state)

**Rationale:** All existing callers already perform async operations (API delete calls) inside `onConfirm`. Making the signature async allows the component to provide loading feedback without every caller implementing their own loading state.

### DetailField

**Current API:**
```typescript
interface DetailFieldProps {
  readonly label: string;
  readonly value: string | null;
  readonly copyable?: boolean;
  readonly linkable?: boolean;
  readonly mailable?: boolean;
  readonly callable?: boolean;
  readonly multiline?: boolean;
  readonly children?: React.ReactNode;
}
```

**New API in @open-elements/ui:**
```typescript
interface DetailFieldTranslations {
  copy: string;
  copied: string;
  open: string;
  email: string;
  call: string;
}

interface DetailFieldProps {
  readonly label: string;
  readonly value: string | null;
  readonly copyable?: boolean;
  readonly linkable?: boolean;
  readonly mailable?: boolean;
  readonly callable?: boolean;
  readonly multiline?: boolean;
  readonly children?: React.ReactNode;
  readonly translations?: DetailFieldTranslations;
}
```

**Changes:**
- New optional `translations` prop for tooltip texts
- Defaults to English strings (`"Copy"`, `"Copied"`, `"Open"`, `"Email"`, `"Call"`) when `translations` is not provided, preserving backwards compatibility
- Adds `"Copied"` label for the check-icon feedback state (currently has no tooltip)

**Rationale:** Consistent with the translations-prop pattern used by all other migrated components (TagForm, HealthStatus, AddCommentDialog, TranslateDialog). Making it optional preserves backwards compatibility for the other app that already uses `@open-elements/ui`.

### Changes in @open-elements/ui

| File | Action |
|------|--------|
| `DeleteConfirmDialog` component | Create |
| `DetailField` component | Create |
| Package exports (index) | Add both components and their type interfaces |

### Changes in open-crm

| File | Action |
|------|--------|
| `frontend/src/components/delete-confirm-dialog.tsx` | Delete |
| `frontend/src/components/detail-field.tsx` | Delete |
| `frontend/src/components/company-detail.tsx` | Update import |
| `frontend/src/components/contact-detail.tsx` | Update import |
| `frontend/src/components/company-delete-dialog.tsx` | Update import, adapt onConfirm to return Promise |
| `frontend/src/components/tag-list.tsx` | Update import, adapt onConfirm to return Promise |
| `frontend/src/components/api-key-list.tsx` | Update import, adapt onConfirm to return Promise |
| `frontend/src/components/webhook-list.tsx` | Update import, adapt onConfirm to return Promise |
| `frontend/src/components/task-detail.tsx` | Update import (if DetailField used) |
| All other files importing either component | Update import path |

**Import change:**
```typescript
// Before
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { DetailField } from "@/components/detail-field";

// After
import { DeleteConfirmDialog, DetailField } from "@open-elements/ui";
```

**onConfirm adaptation:** Existing callers already use async functions inside `onConfirm`. The only change is ensuring they return the Promise instead of firing and forgetting:

```typescript
// Before (fire-and-forget)
onConfirm={handleDelete}  // handleDelete is already async, return value was ignored

// After (same function, Promise is now consumed by the component)
onConfirm={handleDelete}  // no change needed if handleDelete already returns Promise<void>
```

## Dependencies

- `@open-elements/ui` must be built and published (or linked via `pnpm link`) before open-crm can consume the updated version
- Both components depend on shadcn/ui primitives (`AlertDialog`, `Button`, `Tooltip`) — already part of `@open-elements/ui`
- `lucide-react` icons (`Copy`, `Check`, `ExternalLink`, `Mail`, `Phone`, `Loader2`) — already a peer dependency

## Open Questions

None.
