# Design: Tag Frontend CRUD

## GitHub Issue

--

## Summary

This spec adds the frontend UI for managing tags, building on the backend API defined in Spec 050. Users can view, create, edit, and delete tags via a dedicated tag list page and create/edit pages. Tags are displayed as colored chips on company and contact detail views, and can be assigned or removed through the existing company/contact edit forms via a multi-select dropdown.

Tags are a simple entity (name, color, optional description), so the UI is intentionally lightweight: no detail page is needed since all information is visible in the list table.

## Goals

- Full CRUD for tags: list, create, edit, delete
- Tag display on company and contact detail views
- Tag assignment via company and contact edit forms
- Bilingual support (DE/EN) for all new UI text
- Consistent look and feel with existing company/contact views

## Non-goals

- Tag count columns (company count, contact count) in the tag list table -- deferred to a separate feature requiring backend DTO changes
- Tag filtering on company/contact list pages
- Drag-and-drop tag ordering
- Tag detail page (all info is visible in the list)

## Technical Approach

### Routing and Navigation

Add a `Tag` icon (from `lucide-react`) to the sidebar navigation, positioned between "Contacts" and "Brevo Sync".

**Rationale:** Tags are a top-level entity like companies and contacts, so they deserve their own sidebar entry. Placing them after contacts groups the entity management items together.

New routes:

| Route | Purpose |
|-------|---------|
| `/tags` | Tag list page |
| `/tags/new` | Create new tag |
| `/tags/[id]/edit` | Edit existing tag |

No `/tags/[id]` detail page -- tags have only three visible fields (name, description, color), all of which are shown in the list table.

### Tag List Page

The tag list follows the established company/contact list pattern with these simplifications:

- **Header:** Page title + "New Tag" button (primary green)
- **Filters:** Single name search field (no additional dropdowns)
- **Table columns:**
  - Color indicator (small filled circle showing the tag's hex color)
  - Name
  - Description (truncated if long)
  - Actions: Edit (pencil icon), Delete (trash icon)
- **Rows are not clickable** (no detail page to navigate to)
- **Pagination:** 20 items per page, same controls as company/contact lists
- **Empty state:** Centered message with CTA button to create first tag
- **Delete:** Clicking the trash icon shows a `DeleteConfirmDialog` with a generic warning: the tag may be assigned to companies or contacts, and deleting it removes all assignments. No specific count is shown.

### Tag Create/Edit Pages

Separate pages (not dialogs) for consistency with the company/contact pattern:

- **Card layout** with `max-w-2xl` wrapper
- **Fields:**
  - **Name** (required) -- text input, validated non-empty, 409 conflict handled with "name already exists" error
  - **Description** (optional) -- textarea (uses shadcn/ui `Textarea` component)
  - **Color** (required) -- combined picker with:
    - Predefined palette of 12 common colors as clickable circles
    - Free hex code text input (e.g., `#5CBA9E`)
    - Both are bidirectionally synced: clicking a palette color updates the text input, typing a valid hex highlights the matching palette circle
- **Validation:**
  - Name: required, non-empty after trim
  - Color: required, must match `/^#[0-9A-Fa-f]{6}$/`
  - API 409 response: display "A tag with this name already exists"
- **Actions:** Save button (primary green, disabled while submitting) + Cancel button (navigates back to `/tags`)
- **On success:** Redirect to `/tags`

**Predefined color palette:**

| Color | Hex |
|-------|-----|
| Red | `#EF4444` |
| Orange | `#F97316` |
| Amber | `#F59E0B` |
| Yellow | `#EAB308` |
| Lime | `#84CC16` |
| Green | `#22C55E` |
| Teal | `#14B8A6` |
| Cyan | `#06B6D4` |
| Blue | `#3B82F6` |
| Indigo | `#6366F1` |
| Purple | `#A855F7` |
| Pink | `#EC4899` |

**Rationale:** 12 colors from the Tailwind default palette provide a good range without overwhelming the user. The free hex input allows full customization when needed.

### Tag Display on Detail Views

A new `TagChips` component renders assigned tags as colored badge/chip elements:

- Each chip shows the tag name with the tag's color as background
- Text color is automatically determined for contrast: white text on dark backgrounds, dark text on light backgrounds (based on relative luminance calculation)
- Displayed in company and contact detail views, between the detail card and the comments section
- Only rendered when the entity has at least one tag assigned
- Uses a "Tags" label/heading above the chips

The component receives `tagIds` from the entity DTO and fetches the full tag data to render names and colors.

### Tag Assignment in Edit Forms

A new `TagMultiSelect` component is added to both company and contact edit forms:

- **Trigger:** Button showing currently selected tags as small colored chips, or placeholder text if none selected
- **Dropdown:** Popover with a list of all available tags, each with a checkbox and color indicator circle
- **Behavior:**
  - Checking/unchecking a tag adds/removes it from the selection
  - Selected tags appear as chips in the trigger button
  - All available tags are fetched on component mount

**Null semantics for tag assignment** (matching backend spec 050):

- A `tagIdsChanged` boolean tracks whether the user interacted with the selector
- If unchanged: `tagIds` is omitted from the request body (preserves existing assignments)
- If changed to empty: `tagIds: []` is sent (removes all assignments)
- If changed to specific tags: `tagIds: [id1, id2]` is sent (replaces assignments)

### Data Model Changes (Frontend)

**New types in `types.ts`:**

```typescript
interface TagDto {
  readonly id: string;
  readonly name: string;
  readonly description: string | null;
  readonly color: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface TagCreateDto {
  readonly name: string;
  readonly description?: string | null;
  readonly color: string;
}
```

**Extended types:**

- `CompanyDto` and `ContactDto`: add `readonly tagIds: readonly string[]`
- `CompanyCreateDto` and `ContactCreateDto`: add `readonly tagIds?: readonly string[] | null`

### API Functions

New functions in `api.ts` following the existing pattern:

| Function | Method | Endpoint | Notes |
|----------|--------|----------|-------|
| `getTags(params)` | GET | `/api/tags` | Paginated, name filter |
| `getTag(id)` | GET | `/api/tags/{id}` | Single tag |
| `createTag(data)` | POST | `/api/tags` | Detect 409 → throw `"CONFLICT"` |
| `updateTag(id, data)` | PUT | `/api/tags/{id}` | Detect 409 → throw `"CONFLICT"` |
| `deleteTag(id)` | DELETE | `/api/tags/{id}` | 204 No Content |

### i18n

Full DE/EN translations added for:

- `nav.tags` -- sidebar label
- `tags.title` -- page title
- `tags.newTag` -- create button
- `tags.empty` -- empty state message
- `tags.columns.*` -- table column headers
- `tags.form.*` -- form labels, placeholders, validation errors, color picker
- `tags.deleteDialog.*` -- delete confirmation dialog
- `tags.pagination.*` -- pagination text
- `companies.form.tags` / `contacts.form.tags` -- tag selector label in entity forms

## Dependencies

- **Spec 050 (Tags Backend):** Must be implemented first -- provides the REST API and data model
- **shadcn/ui Textarea:** May need to be added if not already present (for description field)
- **lucide-react `Tag` icon:** Already available in the dependency

## Security Considerations

- Tag names are user input and must be rendered safely (React handles this by default via JSX escaping)
- Hex color values from the API are used in inline styles -- validated to match hex pattern before rendering to prevent CSS injection
- All API calls use the existing `apiFetch` wrapper which handles authentication token forwarding

## Open Questions

None -- all questions resolved during the grill session.
