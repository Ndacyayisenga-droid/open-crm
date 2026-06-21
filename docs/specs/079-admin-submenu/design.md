# Design: Admin Sub-Menu in Sidebar

## GitHub Issue

_To be created by the user._

## Summary

The sidebar currently has three separate bottom-level navigation items (Admin, Webhooks, API Keys) that are all admin-related. This spec groups them under a single collapsible "Admin" parent menu item. Additionally, the current monolithic Admin page (which contains Health Status, Bearer Token, and Brevo Integration) is split into three separate pages, bringing the total to five sub-items: Server Status, Bearer Token, Brevo Integration, API Keys, and Webhooks.

## Goals

- Group all admin-related pages under a collapsible "Admin" parent in the sidebar
- Split the existing Admin page into three focused pages (Server Status, Bearer Token, Brevo Integration)
- Keep the mobile sidebar simple with a flat list (no collapsible group)

## Non-goals

- Backend changes — this is purely a frontend restructuring
- Changing any functionality on the individual pages (Health Status, Bearer Token, Brevo, API Keys, Webhooks)
- Adding role-based visibility to admin items
- Nested sub-menus (only one level of nesting)

## Technical Approach

### Sidebar Collapsible Sub-Menu (Desktop)

The `NavLinks` component in `sidebar.tsx` is extended with a collapsible group concept. The current `bottomItems` array is replaced with an admin group structure.

**Collapse/expand behavior:**
- Clicking the "Admin" parent item toggles the sub-menu open/closed — it does **not** navigate to a page
- The sub-menu is **open** when the current route starts with any admin sub-route (`/admin`, `/api-keys`, `/webhooks`)
- The sub-menu **closes automatically** when navigating to a non-admin page (Companies, Contacts, Tasks, Tags)
- After a page refresh, the sub-menu state is derived from the current pathname — no localStorage persistence needed

**Rationale:** Deriving open/closed state from the pathname is simpler and more reliable than persisting to localStorage. If the user is on an admin page, the menu is open. If not, it's closed. This covers the refresh case naturally.

**Visual design:**
- The "Admin" parent item uses the existing nav item styling with a chevron icon (right side) that rotates when expanded
- Sub-items are indented with `pl-10` (additional left padding) to visually nest under the parent
- Active state highlighting (green) applies to individual sub-items, not the parent
- The parent item gets a subtle highlight (`text-oe-white` instead of `text-oe-white/70`) when any sub-item is active
- Uses `ChevronDown` from lucide-react, rotated 180° when collapsed

### Sidebar Flat List (Mobile)

The mobile sidebar (Sheet component) renders all five admin sub-items as top-level entries in the bottom section, without the "Admin" parent grouping. This keeps the mobile experience simple — no nested menus in a slide-out panel.

The mobile list order:
1. Server Status
2. Bearer Token
3. Brevo Integration
4. API Keys
5. Webhooks

### Admin Page Split

The current `/admin` route and its page component are replaced with three separate routes:

| Route | Page Content | Source |
|-------|-------------|--------|
| `/admin/status` | Health check (UP/DOWN indicator) | Extracted from `AdminPage` |
| `/admin/token` | Bearer Token card (show/hide, copy, validity) | `BearerTokenCard` from `AdminPage` |
| `/admin/brevo` | Brevo settings and import | `BrevoSync` component |

The existing `/api-keys` and `/webhooks` routes remain unchanged.

**Rationale:** Using `/admin/*` sub-routes for the new pages groups them logically and simplifies the pathname-based sub-menu state detection. All admin routes share the `/admin` prefix or are one of `/api-keys`, `/webhooks`.

### Route Mapping

| Sub-Menu Item | Route | Icon |
|---------------|-------|------|
| Server Status | `/admin/status` | `Activity` |
| Bearer Token | `/admin/token` | `KeyRound` |
| Brevo Integration | `/admin/brevo` | `RefreshCw` |
| API Keys | `/api-keys` | `KeyRound` |
| Webhooks | `/webhooks` | `Webhook` |

**Admin sub-menu detection:** The sub-menu is open when pathname starts with `/admin` OR `/api-keys` OR `/webhooks`.

### Component Changes

**`sidebar.tsx`** — Major changes:
- Replace `bottomItems` array with an admin sub-menu structure
- Add collapsible section with chevron toggle for desktop
- Derive open state from pathname: `pathname.startsWith("/admin") || pathname.startsWith("/api-keys") || pathname.startsWith("/webhooks")`
- Mobile `NavLinks` renders sub-items flat (no parent grouping)
- Add `onNavigate` prop passthrough for mobile sheet closing

**`frontend/src/app/(app)/admin/page.tsx`** — Delete or redirect:
- The old combined admin page is removed
- Optionally add a redirect from `/admin` to `/admin/status`

**New page files:**
- `frontend/src/app/(app)/admin/status/page.tsx` — Health check only
- `frontend/src/app/(app)/admin/token/page.tsx` — Bearer Token card (moved from admin page)
- `frontend/src/app/(app)/admin/brevo/page.tsx` — Brevo settings and import

**Existing pages unchanged:**
- `frontend/src/app/(app)/api-keys/page.tsx`
- `frontend/src/app/(app)/webhooks/page.tsx`

### i18n Changes

New navigation keys:

```
nav.admin         → "Admin" (parent label, unchanged)
nav.serverStatus  → "Server Status" / "Serverstatus"
nav.bearerToken   → "Bearer Token" / "Bearer Token"
nav.brevo         → "Brevo Integration" / "Brevo-Integration"
nav.apiKeys       → "API Keys" (unchanged)
nav.webhooks      → "Webhooks" (unchanged)
```

The existing `admin.title` key is no longer needed for the combined page heading. Each sub-page uses its own heading from existing i18n keys (e.g., `health.title`, `admin.token.title`, `brevo.title`).

## Open Questions

None — all decisions resolved during the grill session.
