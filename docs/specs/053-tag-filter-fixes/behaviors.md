# Behaviors: Tag Filter Layout & Integration Fixes

## TagMultiSelect — Compact Trigger

### Shows placeholder when no tags selected

- **Given** no tags are selected
- **When** the TagMultiSelect is rendered
- **Then** the trigger shows "Tags..." in muted text

### Shows count text when tags are selected

- **Given** 3 tags are selected
- **When** the TagMultiSelect is rendered
- **Then** the trigger shows "3 Tags ausgewählt" (DE) or "3 tags selected" (EN)

### Shows count text for single tag

- **Given** 1 tag is selected
- **When** the TagMultiSelect is rendered
- **Then** the trigger shows "1 Tag ausgewählt" (DE) or "1 tags selected" (EN)

### Trigger has same height as Select components

- **Given** the TagMultiSelect is placed in a filter row next to shadcn Select components
- **When** the filter row is rendered
- **Then** all controls have the same height (h-10) and align on a single line

### Popover content unchanged

- **Given** tags exist in the system
- **When** the user opens the TagMultiSelect popover
- **Then** the checkbox list with color dots and checkmarks appears as before

### Tag removal via popover

- **Given** Tag A is selected
- **When** the user opens the popover and clicks Tag A
- **Then** Tag A is deselected and the count text updates

### Works in filter context

- **Given** the TagMultiSelect is used as a filter in the company list
- **When** the user selects 2 tags
- **Then** the trigger shows "2 Tags ausgewählt" and the list is filtered

### Works in form context

- **Given** the TagMultiSelect is used in the company create form
- **When** the user selects 2 tags
- **Then** the trigger shows "2 Tags ausgewählt" and the tag IDs are passed to the form state

## Print View — Tag Filter Integration

### Company print passes tag filter

- **Given** the company list is filtered by Tag A
- **When** the user clicks the print button
- **Then** the print URL includes `tagIds={tagA-id}`

### Company print filters data by tags

- **Given** the company print page is opened with `?tagIds={tagA-id}`
- **When** the data loads
- **Then** only companies with Tag A are displayed

### Company print shows tag names in filter summary

- **Given** the company print page is opened with `?tagIds={tagA-id}&tagIds={tagB-id}`
- **When** the page renders
- **Then** the filter summary includes "Tags: Marketing, VIP" (resolved tag names)

### Contact print passes tag filter

- **Given** the contact list is filtered by Tag B
- **When** the user clicks the print button
- **Then** the print URL includes `tagIds={tagB-id}`

### Contact print filters data by tags

- **Given** the contact print page is opened with `?tagIds={tagB-id}`
- **When** the data loads
- **Then** only contacts with Tag B are displayed

### Contact print shows tag names in filter summary

- **Given** the contact print page is opened with `?tagIds={tagB-id}`
- **When** the page renders
- **Then** the filter summary includes "Tags: {tagB-name}"

### Print without tag filter works as before

- **Given** the company print page is opened without `tagIds` parameter
- **When** the data loads
- **Then** all companies are shown (no tag filter applied) and no "Tags:" entry appears in the filter summary

## CSV Export — Tag Filter Integration

### Company export URL includes tag filter

- **Given** the company list is filtered by Tag A and Tag B
- **When** the user triggers CSV export
- **Then** the export URL includes `tagIds={tagA-id}&tagIds={tagB-id}`

### Company export backend filters by tags

- **Given** a CSV export request with `tagIds={tagA-id}`
- **When** the backend processes the export
- **Then** only companies with Tag A are included in the CSV

### Contact export URL includes tag filter

- **Given** the contact list is filtered by Tag A
- **When** the user triggers CSV export
- **Then** the export URL includes `tagIds={tagA-id}`

### Contact export backend filters by tags

- **Given** a CSV export request with `tagIds={tagA-id}`
- **When** the backend processes the export
- **Then** only contacts with Tag A are included in the CSV

### Export without tag filter works as before

- **Given** a CSV export request without `tagIds` parameter
- **When** the backend processes the export
- **Then** all records are included (no tag filter applied)

### Tag filter combines with other filters in export

- **Given** a company CSV export request with `tagIds={tagA-id}` and `name=Acme`
- **When** the backend processes the export
- **Then** only companies with Tag A AND name containing "Acme" are included

## Edge Cases

### Print with non-existent tag ID

- **Given** the print page is opened with `?tagIds={non-existent-id}`
- **When** the data loads
- **Then** tag name resolution fails gracefully (tag omitted from filter summary), data filter returns empty or ignores unknown tag

### Empty tag selection after clearing

- **Given** the user had 2 tags selected in the filter
- **When** the user deselects all tags via the popover
- **Then** the trigger shows "Tags..." placeholder and the list shows unfiltered data
