# Implementation Steps: Global UI Styling Fixes

## Step 1: Add semantic CSS variables to globals.css

- [x] Add all 21 shadcn/ui semantic CSS variables to the `@theme` block in `frontend/src/app/globals.css`
- [x] Map variables to Open Elements brand colors as specified in design.md

**Acceptance criteria:**
- [x] Project builds successfully
- [x] All existing tests pass
- [x] Dialog backgrounds are opaque white
- [x] Select dropdowns have opaque white backgrounds
- [x] Table row borders are subtle light gray instead of black

**Related behaviors:** All scenarios in behaviors.md — this is a single CSS change that resolves all issues.
