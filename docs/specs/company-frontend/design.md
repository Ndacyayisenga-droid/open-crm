# Design: Company Frontend

## GitHub Issue

_To be created._

## Summary

Build the first real UI for Open CRM: a Company management frontend. Users can list, view, create, edit, soft-delete, and restore companies through a responsive web interface. This introduces the application's navigation structure (sidebar) and establishes the frontend patterns (API client, form components, i18n strings) that future UI features (contacts, comments) will reuse.

## Goals

- Company list with pagination, filtering (name, city, country), and sorting
- Company detail page showing all fields
- Create and edit forms on dedicated pages
- Soft-delete with confirmation dialog and 409 error handling
- Restore soft-deleted companies from an "archived" view
- Responsive sidebar navigation (collapses to hamburger on mobile)
- Placeholder comment section on the detail page (no functionality yet)
- All user-facing strings are i18n-ready

## Non-goals

- Comment functionality (display and creation) — placeholder UI only
- Contact management UI — separate spec
- Wildcard search (`*lando`, `Zal*`) — future spec with backend changes
- Authentication / authorization — separate feature
- Dashboard layout — may come later

## Technical Approach

### Navigation & Layout

A vertical sidebar on the left provides the main navigation. It contains entries for "Server-Health" and "Firmen". The root layout (`layout.tsx`) wraps all pages with this sidebar.

On mobile screens (below `md` breakpoint), the sidebar collapses into a hamburger menu button in a top bar.

**Rationale:** A sidebar scales well as more features are added (contacts, settings, etc.) without cluttering a horizontal nav bar. shadcn/ui provides sidebar building blocks.

### Routing

| Route | Page | Type |
|-------|------|------|
| `/` | Redirect to `/companies` | Redirect |
| `/health` | Server health status (moved from `/`) | Server component |
| `/companies` | Company list | Client component (interactive filters) |
| `/companies/new` | Create company form | Client component |
| `/companies/[id]` | Company detail | Server component |
| `/companies/[id]/edit` | Edit company form | Client component |

**Rationale:** The health page moves to `/health` so `/` can serve as the app entry point redirecting to the main feature. List and form pages are client components for interactivity; the detail page is a server component since it's primarily read-only.

### API Client

A shared API client module (`src/lib/api.ts`) provides typed functions for all company operations:

```typescript
// Server-side (uses BACKEND_URL)
getCompanies(params): Promise<Page<CompanyDto>>
getCompany(id): Promise<CompanyDto>

// Client-side (uses /api/* rewrite)
createCompany(data): Promise<CompanyDto>
updateCompany(id, data): Promise<CompanyDto>
deleteCompany(id): Promise<void>
restoreCompany(id): Promise<CompanyDto>
```

Server-side functions use `BACKEND_URL` env var directly. Client-side functions use the Next.js rewrite (`/api/*` → backend).

**Rationale:** Separating server and client API calls follows the Next.js convention of not exposing backend URLs to the browser. Typed functions provide compile-time safety.

### Component Structure

```
src/
├── app/
│   ├── layout.tsx                    (modified — add sidebar)
│   ├── page.tsx                      (modified — redirect to /companies)
│   ├── health/
│   │   └── page.tsx                  (moved health status here)
│   └── companies/
│       ├── page.tsx                  (company list page)
│       ├── new/
│       │   └── page.tsx              (create company page)
│       └── [id]/
│           ├── page.tsx              (company detail page)
│           └── edit/
│               └── page.tsx          (edit company page)
├── components/
│   ├── sidebar.tsx                   (navigation sidebar)
│   ├── company-list.tsx              (list with table, filters, pagination)
│   ├── company-form.tsx              (reusable create/edit form)
│   ├── company-detail.tsx            (detail display with all fields)
│   └── delete-confirm-dialog.tsx     (reusable confirmation dialog)
└── lib/
    ├── api.ts                        (API client functions)
    └── constants.ts                  (extended with company strings)
```

### Company List UI

- **Table** with columns: Name, Website
- **Filter bar** above the table: text inputs for name, city, country
- **Sort** dropdown or clickable column headers
- **Pagination** controls at the bottom (page numbers, previous/next)
- **"Neue Firma"** button in the header
- **"Archivierte Firmen anzeigen"** toggle button
- **Delete** button per row (icon button)
- **Empty state:** "Keine Firmen vorhanden. Erstellen Sie die erste Firma." with a create button
- Clicking a row navigates to the detail page

When "Archivierte Firmen anzeigen" is active:
- Deleted companies appear in the list (visually distinct, e.g., muted text)
- A "Wiederherstellen" button replaces the delete button for archived entries

### Company Detail UI

- All company fields displayed in a structured layout (card or definition list)
- **"Bearbeiten"** button → navigates to edit page
- **"Löschen"** button → opens confirmation dialog
- **Comments section** at the bottom: "Kommentare" heading, "Keine Kommentare vorhanden" text, disabled "Kommentar hinzufügen" button

### Company Form UI

- Reusable form component for both create and edit
- Fields: Name (required), E-Mail, Website, Straße, Hausnummer, PLZ, Stadt, Land
- Client-side validation (name required)
- **"Speichern"** button submits the form
- **"Abbrechen"** button navigates back
- On success: redirect to detail page
- On error: inline error message

### Delete Flow

1. User clicks delete button (list or detail page)
2. Confirmation dialog: "Möchten Sie die Firma '{name}' wirklich löschen?"
3. On confirm: call DELETE API
4. On success: redirect to list (from detail) or refresh list (from list)
5. On 409 error: show error dialog "Die Firma kann nicht gelöscht werden, da noch Kontakte zugeordnet sind."

### shadcn/ui Components Required

- `Button` — all actions
- `Input` — form fields, filter inputs
- `Table` — company list
- `Dialog` / `AlertDialog` — delete confirmation, error display
- `Label` — form labels
- `Separator` — visual dividers
- `Sheet` — mobile sidebar
- `Skeleton` — loading states

### Brand Guidelines Applied

- **Sidebar background:** oe-dark (`#020144`)
- **Sidebar text:** oe-white (`#ffffff`)
- **Primary action buttons:** oe-green (`#5CBA9E`)
- **Delete/error buttons:** oe-red (`#E63277`)
- **Page backgrounds:** oe-white
- **Headings:** Montserrat font
- **Body text:** Lato font
- **Table/card borders:** oe-gray-light (`#e8e6dc`)

## Dependencies

- shadcn/ui components: Button, Input, Table, Dialog, AlertDialog, Label, Separator, Sheet, Skeleton
- Existing: Next.js 15, React 19, Tailwind CSS, Vitest + React Testing Library

## Open Questions

- Exact pagination style (page numbers vs. simple prev/next)
- Whether sort should be via clickable column headers or a separate dropdown
