# Design: Webhook Frontend View

## GitHub Issue

—

## Summary

Webhooks can currently only be managed via the REST API or Swagger UI. This spec adds a dedicated frontend page at `/webhooks` for managing webhooks — listing all registered webhooks in a paginated table, creating new webhooks via a dialog, deleting webhooks with confirmation, toggling active/inactive status, and sending PING test calls. The page follows the existing list page patterns (Tags, Companies, etc.).

## Goals

- Provide a webhook management UI consistent with the existing CRM frontend
- Support create, delete, toggle active/inactive, and PING actions
- Show webhook health status (lastStatus, lastCalledAt) in the table
- Bilingual support (DE/EN)

## Non-goals

- Webhook detail page (all actions are inline in the table)
- Editing webhook URL (only create and delete)
- Auto-polling after PING (user refreshes manually)
- Webhook event filtering or history view

## Technical Approach

### Page Structure

A new route at `/webhooks` within the `(app)` route group, following the thin page wrapper pattern:

```
frontend/src/app/(app)/webhooks/
└── page.tsx          — renders <WebhookList />
```

A single client component `webhook-list.tsx` handles the entire view — no detail page, no form page. The create flow uses an inline Dialog/Modal.

### Sidebar Navigation

Add a "Webhooks" entry in the sidebar below Admin, using the Lucide `Webhook` icon. Add `webhooks` to the `nav` translations in both DE and EN.

### Table Columns

| Column | Content | Responsive |
|--------|---------|------------|
| URL | Webhook URL (truncated) | Always visible |
| Active | Toggle switch or badge | Always visible |
| Last Status | Human-readable label | Hidden on mobile (`hidden md:table-cell`) |
| Last Called | Relative or formatted timestamp | Hidden on mobile (`hidden md:table-cell`) |
| Actions | PING, Delete buttons | Always visible |

### Last Status Display

| `lastStatus` value | Display |
|--------------------|---------|
| `null` | `—` |
| `200`, `201`, ... (2xx) | "OK" |
| `-1` | "Timeout" |
| `0` | "Connection Error" |
| `4xx`, `5xx` | "Bad Call (status)" e.g. "Bad Call (404)" |

### Actions per Row

1. **Toggle Active/Inactive** — Calls `PUT /api/webhooks/{id}` with current URL and toggled `active` flag. Immediate visual update after success.
2. **PING** — Calls `POST /api/webhooks/{id}/ping`. Fires directly on click, no confirmation dialog, no visible feedback. Result visible after manual page refresh via lastStatus.
3. **Delete** — Opens `DeleteConfirmDialog`, then calls `DELETE /api/webhooks/{id}`. Refetches list on success.

All action buttons have Tooltips.

### Create Webhook Dialog

A `Dialog` (shadcn/ui) triggered by the "New Webhook" button in the page header. Contains:
- URL input field (`@NotBlank`, validated client-side)
- Create button (calls `POST /api/webhooks`)
- Cancel button
- Error display if creation fails

On success: close dialog, refetch list.

**Rationale:** Using a dialog instead of a separate page because the form has only a single field (URL). A full page would be unnecessarily heavy.

### API Client Functions

Add to `frontend/src/lib/api.ts`:

```typescript
getWebhooks(params): Promise<Page<WebhookDto>>
createWebhook(data: WebhookCreateDto): Promise<WebhookDto>
updateWebhook(id, data: WebhookUpdateDto): Promise<WebhookDto>
deleteWebhook(id): Promise<void>
pingWebhook(id): Promise<void>
```

### TypeScript Types

Add to `frontend/src/lib/types.ts`:

```typescript
interface WebhookDto {
  readonly id: string;
  readonly url: string;
  readonly active: boolean;
  readonly lastStatus: number | null;
  readonly lastCalledAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface WebhookCreateDto {
  readonly url: string;
}

interface WebhookUpdateDto {
  readonly url: string;
  readonly active: boolean;
}
```

### i18n Translations

Add `webhooks` namespace to both `de.ts` and `en.ts` with:
- Page title, empty state, create button
- Column headers (URL, Active, Last Status, Last Called, Actions)
- Status labels (OK, Timeout, Connection Error, Bad Call, never called)
- Create dialog (title, URL label, placeholder, create, cancel, error)
- Delete dialog (title, description, confirm, cancel, error)
- Action tooltips (ping, delete, activate, deactivate)
- Pagination labels

### Component Patterns

Follow the established patterns from `tag-list.tsx`:
- `"use client"` component
- `useState` for data, loading, pagination, search (no search needed here — webhooks have no name filter)
- `useCallback` + `useEffect` for data fetching
- `localStorage` for page size preference (`pageSize.webhooks`)
- Skeleton loaders during loading
- Empty state with icon and create button
- `DeleteConfirmDialog` for delete confirmation

## Dependencies

- No new npm packages — uses existing shadcn/ui components
- Lucide `Webhook` icon for sidebar (already available in lucide-react)
- Existing backend API endpoints from specs 075 + 076

## Security Considerations

- All API calls go through the existing Auth.js proxy with JWT token injection
- No additional security concerns — same auth model as all other frontend pages

## Open Questions

None — all questions resolved during the grill session.
