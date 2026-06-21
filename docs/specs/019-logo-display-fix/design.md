# Design: Company Logo Display Fix

## GitHub Issue

_(to be linked once created)_

## Summary

Company logos are displayed in a fixed rectangular area at three places in the UI (list table, detail view, form preview). Currently, `object-cover` is used, which crops logos that don't match the container's aspect ratio — parts of the logo are visually cut off. Additionally, logos have slightly rounded corners (`rounded` CSS class) which is not appropriate for logos.

This spec changes the display so that logos are always fully visible (never cropped) and have sharp rectangular corners.

## Goals

- Logos are always displayed in their entirety — no visual cropping
- Logos have sharp corners (no border-radius)
- Placeholder icons (`Building2`) remain unchanged in size and appearance

## Non-goals

- Changing Contact photo display (stays `object-cover` + `rounded-full`)
- Adding background color to the empty space around fitted logos
- Handling transparent SVG visibility issues (known, deferred)
- Resizing containers or changing logo dimensions

## Fix Approach

Change CSS classes on all `<img>` tags that display company logos:

1. Replace `object-cover` with `object-contain` — this fits the entire image within the container, adding empty space instead of cropping
2. Remove `rounded` — sharp rectangular corners instead of rounded

**Files changed:**

| File | Location | Current classes | New classes |
|------|----------|----------------|-------------|
| `frontend/src/components/company-list.tsx` | Logo thumbnail (32x32) | `h-8 w-8 rounded object-cover` | `h-8 w-8 object-contain` |
| `frontend/src/components/company-detail.tsx` | Logo header (96x96) | `h-24 w-24 rounded object-cover` | `h-24 w-24 object-contain` |
| `frontend/src/components/company-form.tsx` | Logo preview (64x64) | `h-16 w-16 rounded object-cover` | `h-16 w-16 object-contain` |

**Rationale:** `object-contain` is the standard CSS approach for fitting images into a container without cropping. The logo should always be recognizable in full, even if that means it appears smaller within the allocated space.

## Regression Risk

- **Low.** Only CSS classes change on 3 `<img>` elements. No logic, API, or data changes.
- Placeholder icons are not affected (they use Lucide components, not `<img>` tags).
- Contact photos are not affected (they keep `object-cover` + `rounded-full`).

## Open Questions

- None
