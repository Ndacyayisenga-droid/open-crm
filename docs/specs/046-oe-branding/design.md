# Design: OE Branding

## GitHub Issue

—

## Summary

Add "Developed by Open Elements" branding to the application and redesign the sidebar header. The sidebar header gets two rows: the app identity (placeholder logo + "Open CRM") and the developer branding ("Developed by" + OE landscape logo linking to the Open Elements website).

## Goals

- Make it visible that the app is developed by Open Elements
- Establish the app identity with name "Open CRM" and a placeholder logo
- Link to the Open Elements website from the branding

## Non-goals

- Designing the final Open CRM app logo (placeholder for now)
- Adding branding to print views or other pages
- Adding a separate footer element

## Technical Approach

### Sidebar Header Redesign

The current sidebar header shows only the app title as a text link. It is redesigned to contain two rows:

**Row 1 — App Identity:**
- Placeholder app logo icon (e.g., `LayoutDashboard` or similar neutral icon from lucide-react)
- App name "Open CRM" next to it
- Clicking navigates to `/companies` (existing behavior)

**Row 2 — Developer Branding:**
- Text "Developed by" in small, subdued font
- Open Elements landscape logo (dark-background SVG variant) next to it
- The entire row is a link to `https://open-elements.com` opening in a new tab

### Brand Assets

The OE landscape logo for dark backgrounds (`logo-landscape-dark-background.svg`) is copied from the brand guidelines directory to `frontend/public/` so it can be referenced as a static asset.

### Styling

Following the existing sidebar design and Open Elements brand guidelines:
- Background: `oe-dark`
- App name: `oe-white`, heading font (Montserrat), bold
- "Developed by" text: `oe-gray-mid` or `oe-gray-light`, small font size
- OE logo: sized to fit the sidebar width, small enough to be unobtrusive
- Row 2 has subtle hover effect (e.g., slight opacity change)
- Border-bottom separates header from nav links (existing)

The layout applies to both desktop sidebar and mobile drawer.

### Files Affected

**Frontend (new):**
- `frontend/public/oe-logo-landscape-dark.svg` — OE landscape logo copied from brand guidelines

**Frontend (modified):**
- `frontend/src/components/sidebar.tsx` — redesign header with two rows (app identity + OE branding)

## Dependencies

- **Open Elements Brand Guidelines** — logo file `logo-landscape-dark-background.svg`
- **Spec 045 (User model prep)** — adds user section at bottom of sidebar. No conflict — branding is in the header, user section is at the bottom.

## Open Questions

None — all details resolved during design discussion.
