# Design: Admin View

## GitHub Issue

—

## Summary

The application currently has two separate views — "Brevo Import" (`/brevo-sync`) with API key settings and import functionality, and "Server Health" (`/health`) with a backend status card. These are merged into a single "Administration" page at `/admin`. The two existing nav items are replaced by a single "Admin" item positioned at the bottom of the sidebar, separated from the main navigation by flexible space and from the language toggle by a border line.

## Goals

- Consolidate Brevo and Health views into a single Admin page
- Position Admin nav item at the bottom of the sidebar, above the language toggle
- Create an extensible layout for future admin sections
- Clean up unused routes and components

## Non-goals

- Adding new admin functionality beyond what already exists
- Tabs, sub-navigation, or sectioned layout within the Admin page
- Backend changes — all endpoints remain as-is
- Redirect from old routes to new route

## Technical Approach

### 1. Admin page — New route

**New file:** `frontend/src/app/admin/page.tsx`

Single page with:
- Page title: "Administration" (i18n)
- Three cards stacked vertically in fixed order:
  1. **Server Health** — Reuse existing `HealthStatus` component
  2. **Brevo API Settings** — Extract from current `BrevoSync` component (API key configuration card)
  3. **Brevo Import** — Extract from current `BrevoSync` component (import trigger + results card)

**Rationale:** Vertically stacked cards are simple, extensible (new sections are appended at the bottom), and consistent with the existing card-based UI pattern. No sub-navigation needed at this scale.

### 2. Component refactoring

The current `BrevoSync` component contains both API Settings and Import in one component. Two options:

- **Option A:** Keep `BrevoSync` as-is and embed it below the Health card on the Admin page
- **Option B:** Split into `BrevoApiSettings` and `BrevoImport` components for independent cards

**Chosen: Option A.** The Brevo Settings and Import cards are already visually separate cards inside `BrevoSync`. The component can be reused directly on the Admin page below the Health status card. No refactoring needed — the component already renders two distinct cards with their own titles. Splitting adds complexity without benefit at this point.

### 3. Sidebar — Nav restructuring

**File:** `frontend/src/components/sidebar.tsx`

Current nav items (top to bottom):
1. Companies
2. Contacts
3. Tags
4. Brevo Import
5. Server Health

New structure:
- **Main navigation** (top): Companies, Contacts, Tags
- **Flexible spacer** (`flex-grow` / `mt-auto`)
- **Admin item** (bottom): Admin with Settings/Gear icon (`Settings` from lucide-react)
- **Border line** (`border-t`)
- **Language toggle + User section** (existing)

Both desktop sidebar and mobile hamburger menu must reflect this layout.

### 4. Route cleanup

**Delete:**
- `frontend/src/app/brevo-sync/page.tsx`
- `frontend/src/app/health/page.tsx`
- Directories `frontend/src/app/brevo-sync/` and `frontend/src/app/health/`

**Keep:**
- `frontend/src/components/brevo-sync.tsx` (reused on Admin page)
- `frontend/src/components/health-status.tsx` (reused on Admin page)

### 5. i18n updates

**Files:** `frontend/src/lib/i18n/en.ts`, `frontend/src/lib/i18n/de.ts`

Changes:
- Add `nav.admin`: "Admin" (EN) / "Admin" (DE)
- Add `admin.title`: "Administration" (EN) / "Administration" (DE)
- Remove `nav.brevoSync` and `nav.health` (no longer needed as separate nav labels)
- Keep all `brevo.*` and `health.*` translations — the components still use them

## Dependencies

- Existing `HealthStatus` component (`frontend/src/components/health-status.tsx`)
- Existing `BrevoSync` component (`frontend/src/components/brevo-sync.tsx`)
- Lucide `Settings` icon for the nav item
