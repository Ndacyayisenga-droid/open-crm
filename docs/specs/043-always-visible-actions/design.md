# Design: Always-Visible Action Icons

## GitHub Issue

—

## Summary

The action icons (copy, external link, mailto, tel) on detail view fields are only visible on hover, which makes them undiscoverable for users. They should always be visible in a lighter color and darken on hover to indicate interactivity.

## Reproduction

1. Open any Company or Contact detail view with fields that have actions (e.g., email, website)
2. Without hovering, no action icons are visible
3. Users do not know the actions exist until they accidentally hover over a field

## Root Cause Analysis

The icon container in `DetailField` uses `opacity-0 group-hover:opacity-100` to hide icons by default and reveal them only on hover. This is a deliberate design choice from Spec 040 that turns out to be confusing in practice.

## Fix Approach

Replace the opacity-based show/hide with an always-visible approach using color transitions:

**Current (line 69 in `detail-field.tsx`):**
```
opacity-0 group-hover:opacity-100 transition-opacity
```

**New:**
- Remove `opacity-0 group-hover:opacity-100 transition-opacity`
- Icons always visible

**Icon button colors:**
- **Desktop (hover-capable):** Default `text-oe-gray-light`, hover `text-oe-dark`
- **Touch devices (no hover):** Default `text-oe-dark`

Implementation via Tailwind media query: use `text-oe-gray-light hover:text-oe-dark` for pointer devices, and `text-oe-dark` for coarse-pointer (touch) devices. Tailwind supports this via `@media (pointer: coarse)` or the `pointer-coarse:` variant.

**Rationale:** Using `pointer: coarse` media query is the standard way to detect touch-primary devices. This ensures touch users see fully visible icons without relying on hover, while desktop users still get the subtle-to-bold color transition.

### Files Affected

- `frontend/src/components/detail-field.tsx` — remove opacity toggle, update icon button color classes

## Regression Risk

- Low. Only the visual appearance of action icons changes, no functional behavior.
- The copy feedback (Check icon in green) remains unchanged.
- Icons still hidden for null/empty fields (no change to `hasActions` logic).
