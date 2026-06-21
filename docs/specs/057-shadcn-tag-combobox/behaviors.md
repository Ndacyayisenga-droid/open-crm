# Behaviors: shadcn Combobox for Tag Selection

## Chip Display

### Selected tags shown as colored chips

- **Given** Tag A (color #5CBA9E) and Tag B (color #E63277) are selected
- **When** the Combobox renders
- **Then** two chips are shown with their respective background colors and contrast text colors

### No chips when nothing selected

- **Given** no tags are selected
- **When** the Combobox renders
- **Then** no chips are shown, only the placeholder text "Tags..."

### Chip removal via X button

- **Given** Tag A is selected and shown as a chip
- **When** the user clicks the X button on the Tag A chip
- **Then** Tag A is deselected and the chip disappears

### Chips have correct contrast text color

- **Given** Tag A has a dark color (#020144)
- **When** the chip renders
- **Then** the text color is white for readability

### Chips have correct contrast on light color

- **Given** Tag A has a light color (#E8E6DC)
- **When** the chip renders
- **Then** the text color is dark for readability

## Search / Filtering

### Search filters tags by name

- **Given** tags "Marketing", "Sales", "Support" exist
- **When** the user types "mar" in the search input
- **Then** only "Marketing" appears in the dropdown list

### Search is case-insensitive

- **Given** tag "Marketing" exists
- **When** the user types "marketing" in the search input
- **Then** "Marketing" appears in the dropdown list

### Empty search result

- **Given** no tags match the search term
- **When** the user types "xyz" in the search input
- **Then** the empty state message is shown

### Search clears after selection

- **Given** the user typed "mar" and sees "Marketing"
- **When** the user selects "Marketing"
- **Then** the search input is cleared

## Dropdown List

### Color dots in dropdown items

- **Given** Tag A (color #5CBA9E) exists
- **When** the dropdown is open
- **Then** a colored circle (h-4 w-4, rounded-full) with #5CBA9E is shown next to "Tag A"

### Already selected tags distinguishable

- **Given** Tag A is selected and the dropdown is open
- **When** the user views the list
- **Then** Tag A is visually distinguishable from unselected tags (e.g., checkmark or highlighted)

### Tag selection via dropdown click

- **Given** the dropdown is open and Tag A is not selected
- **When** the user clicks Tag A
- **Then** Tag A is added to the selection and appears as a chip

### Tag deselection via dropdown click

- **Given** the dropdown is open and Tag A is selected
- **When** the user clicks Tag A
- **Then** Tag A is removed from the selection and the chip disappears

## Visual Consistency

### Combobox matches height of adjacent controls

- **Given** the TagMultiSelect is in a filter row next to Input and Select components
- **When** the filter row renders with no tags selected
- **Then** all controls have the same base height

### Combobox matches border style of adjacent controls

- **Given** the TagMultiSelect is in a filter row
- **When** the filter row renders
- **Then** the Combobox has the same border color and radius as other shadcn controls

## Context: Filter Row

### Works as filter in company list

- **Given** the company list filter row
- **When** the user selects tags in the Combobox
- **Then** the company list filters by the selected tags

### Works as filter in contact list

- **Given** the contact list filter row
- **When** the user selects tags in the Combobox
- **Then** the contact list filters by the selected tags

## Context: Create/Edit Form

### Works in company form

- **Given** the company create/edit form
- **When** the user selects tags in the Combobox
- **Then** the selected tag IDs are included in the form submission

### Works in contact form

- **Given** the contact create/edit form
- **When** the user selects tags in the Combobox
- **Then** the selected tag IDs are included in the form submission

### Pre-populated tags in edit form

- **Given** a company with Tag A and Tag B assigned
- **When** the edit form opens
- **Then** the Combobox shows Tag A and Tag B as colored chips

## Edge Cases

### Many tags selected

- **Given** 10 tags are selected
- **When** the Combobox renders
- **Then** all 10 chips are displayed (component grows in height, accepted)

### Invalid tag color

- **Given** a tag with an invalid color value (not matching #RRGGBB)
- **When** the chip or color dot renders
- **Then** a fallback gray color (#6B7280) is used

### No tags exist in system

- **Given** no tags have been created
- **When** the Combobox dropdown opens
- **Then** the empty state message is displayed
