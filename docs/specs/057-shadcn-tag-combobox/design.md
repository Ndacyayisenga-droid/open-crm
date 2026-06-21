# Design: shadcn Combobox for Tag Selection

## GitHub Issue

—

## Summary

The current `TagMultiSelect` component is a custom implementation using Popover + Button that looks visually inconsistent with other shadcn/ui form controls (Select, Input). The shadcn/ui Combobox component provides a native multi-select variant with chips, integrated search, and styling that matches the design system out of the box.

This spec replaces the custom `TagMultiSelect` with the shadcn/ui Combobox component. The new component shows selected tags as colored chips (using the tag's hex color), has an integrated search field for filtering, and displays color dots in the dropdown list.

**Note:** This spec partially supersedes Spec 053, which decided on compact text ("X Tags ausgewählt") instead of chips. That decision was a workaround for the layout-height problem caused by the custom component. The shadcn Combobox solves the layout consistency natively, making chips viable again.

## Goals

- Replace custom `TagMultiSelect` with shadcn/ui Combobox (multiple selection)
- Achieve visual consistency with other shadcn/ui form controls
- Retain tag-colored chips and color dots in dropdown
- Single component for both filter and form contexts

## Non-goals

- Server-side tag search/filtering (all tags preloaded, client-side filter)
- Limiting the number of selectable tags
- Truncating/collapsing chips when many are selected

## Technical Approach

### 1. Install shadcn Combobox

```bash
npx shadcn@latest add combobox
```

This adds the `combobox` component to `frontend/src/components/ui/combobox.tsx`.

### 2. Rewrite TagMultiSelect

**File:** `frontend/src/components/tag-multi-select.tsx`

Replace the entire component implementation. The new component uses:

```tsx
<Combobox items={allTags} multiple value={selectedIds} onValueChange={onChange}>
  <ComboboxChips>
    <ComboboxValue>
      {selectedTags.map((tag) => (
        <ComboboxChip
          key={tag.id}
          style={{
            backgroundColor: tag.color,
            color: getContrastColor(tag.color),
          }}
        >
          {tag.name}
        </ComboboxChip>
      ))}
    </ComboboxValue>
    <ComboboxChipsInput placeholder={t.tags.label + "..."} />
  </ComboboxChips>
  <ComboboxContent>
    <ComboboxEmpty>{t.tags.empty}</ComboboxEmpty>
    <ComboboxList>
      {(tag) => (
        <ComboboxItem key={tag.id} value={tag.id}>
          <span
            className="inline-block h-4 w-4 rounded-full shrink-0"
            style={{ backgroundColor: tag.color }}
          />
          {tag.name}
        </ComboboxItem>
      )}
    </ComboboxList>
  </ComboboxContent>
</Combobox>
```

**Key changes:**
- Trigger is now `ComboboxChips` (not a Button) — automatically matches shadcn control height and styling
- Selected tags render as `ComboboxChip` with tag-colored background and contrast text color
- Search is built into `ComboboxChipsInput` — no separate search UI needed
- Dropdown uses `ComboboxList` with color dots (same visual as before)
- Chip removal via built-in X button on each chip (provided by `ComboboxChip`)

**Preserved behavior:**
- Props: `selectedIds: readonly string[]`, `onChange: (ids: string[]) => void` — API unchanged
- Loads all tags on mount with `getTags({ size: 1000 })`
- `isValidHex()` for color validation
- `getContrastColor()` for chip text color

**Rationale:** The shadcn Combobox handles the hard parts (keyboard navigation, search filtering, chip layout, consistent sizing) that were manually implemented before. The component API (`selectedIds` + `onChange`) stays the same, so all consumers (filter rows, forms) work without changes.

### 3. Remove dead code

After rewriting, remove from `tag-multi-select.tsx`:
- Custom Popover/PopoverTrigger/PopoverContent imports
- Button import
- Manual `toggle()` and `removeTag()` functions
- Custom `open` state management
- `Check`, `ChevronsUpDown`, `X` icon imports (Combobox handles these internally)

Keep:
- `isValidHex()` utility function
- `getContrastColor()` utility function
- Tag fetching logic (`useEffect` with `getTags`)

### 4. Consumers — No changes needed

Since the component API (`selectedIds`, `onChange`) remains the same, these files need no modifications:
- `frontend/src/components/company-list.tsx` (filter)
- `frontend/src/components/contact-list.tsx` (filter)
- `frontend/src/components/company-form.tsx` (create/edit form)
- `frontend/src/components/contact-form.tsx` (create/edit form)

### 5. Spec 053 update

Spec 053's design doc should get a Drift Log entry noting that the compact text decision ("X Tags ausgewählt") was superseded by this spec. The remaining parts of Spec 053 (print and CSV export tag filter pass-through) are unaffected.

## Regression Risk

- **Layout:** The shadcn Combobox should naturally match other controls in height. If many chips are selected, the component grows — this is accepted.
- **Color styling:** Custom `style` props on `ComboboxChip` may need verification that they don't conflict with the Combobox's internal styles.
- **Form integration:** The component API is unchanged, so form state management is unaffected.

## Dependencies

- shadcn/ui Combobox component (must be installed)
- Existing `getTags` API function
- Existing tag color utilities (`isValidHex`, `getContrastColor`)
