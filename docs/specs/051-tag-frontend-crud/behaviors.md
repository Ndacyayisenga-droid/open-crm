# Behaviors: Tag Frontend CRUD

## Navigation

### Sidebar shows Tags entry

- **Given** the user is logged in and the sidebar is visible
- **When** the sidebar renders
- **Then** a "Tags" entry with a Tag icon is shown between "Contacts" and "Brevo Sync"

### Tags link navigates to tag list

- **Given** the sidebar is visible
- **When** the user clicks the "Tags" navigation entry
- **Then** the browser navigates to `/tags`

### Tags link is highlighted when active

- **Given** the user is on any `/tags` route (list, new, or edit)
- **When** the sidebar renders
- **Then** the "Tags" entry is highlighted with the active style (green)

## Tag List

### Empty state shown when no tags exist

- **Given** no tags exist in the system
- **When** the user navigates to `/tags`
- **Then** a centered message is shown indicating no tags exist
- **And** a "Create" button is shown that navigates to `/tags/new`

### Tags displayed in table

- **Given** tags exist in the system
- **When** the user navigates to `/tags`
- **Then** a table is shown with columns: color indicator, name, description, actions
- **And** the color indicator is a small filled circle in the tag's hex color
- **And** tags are paginated with 20 items per page

### Loading state shown while fetching

- **Given** the user navigates to `/tags`
- **When** the tag data is being fetched
- **Then** skeleton loading placeholders are shown

### Name search filters tags

- **Given** tags exist with names "Important", "Follow-up", "VIP"
- **When** the user types "imp" in the search field
- **Then** only tags whose name matches the filter are shown

### Search is debounced

- **Given** the user is on the tag list page
- **When** the user types quickly in the search field
- **Then** the API is not called for every keystroke but debounced

### Pagination controls work

- **Given** more than 20 tags exist
- **When** the user views the tag list
- **Then** pagination controls (Previous/Next) are shown
- **And** the total count and current page are displayed

### Previous button disabled on first page

- **Given** the user is on page 1 of the tag list
- **When** the pagination renders
- **Then** the "Previous" button is disabled

### Next button disabled on last page

- **Given** the user is on the last page of the tag list
- **When** the pagination renders
- **Then** the "Next" button is disabled

### Search resets to first page

- **Given** the user is on page 3 of the tag list
- **When** the user changes the search filter
- **Then** the page resets to 1

### Table rows are not clickable

- **Given** tags are displayed in the table
- **When** the user clicks on a table row (not on an action button)
- **Then** nothing happens (no navigation, no detail dialog)

## Tag Deletion

### Delete button shows confirmation dialog

- **Given** a tag exists in the list
- **When** the user clicks the delete (trash) icon on a tag row
- **Then** a confirmation dialog appears with a generic warning that the tag may be assigned to companies or contacts and deleting removes all assignments

### Confirming deletion removes the tag

- **Given** the delete confirmation dialog is open for a tag
- **When** the user clicks the confirm button
- **Then** the tag is deleted via the API
- **And** the tag list refreshes without the deleted tag

### Canceling deletion closes dialog

- **Given** the delete confirmation dialog is open
- **When** the user clicks cancel
- **Then** the dialog closes and no deletion occurs

### Delete error shown in dialog

- **Given** the delete confirmation dialog is open
- **When** the user confirms and the API returns an error
- **Then** the error is displayed in the dialog
- **And** only the cancel button is shown

## Tag Create

### New Tag button navigates to create page

- **Given** the user is on the tag list page
- **When** the user clicks the "New Tag" button
- **Then** the browser navigates to `/tags/new`

### Create page shows empty form

- **Given** the user navigates to `/tags/new`
- **When** the page renders
- **Then** a form is shown with empty fields for name, description, and color
- **And** the page title indicates "Create New Tag"

### Name is required

- **Given** the user is on the create tag page
- **When** the user submits the form with an empty name
- **Then** a validation error "Name is required" is shown below the name field
- **And** the form is not submitted

### Color is required

- **Given** the user is on the create tag page
- **When** the user submits the form with no color selected
- **Then** a validation error "Color is required" is shown below the color field
- **And** the form is not submitted

### Invalid hex color rejected

- **Given** the user is on the create tag page
- **When** the user enters an invalid hex value (e.g., "red" or "#GGG") in the color input
- **Then** a validation error "Invalid color format" is shown
- **And** the form is not submitted

### Predefined color palette selects color

- **Given** the user is on the create tag page
- **When** the user clicks a color circle in the predefined palette
- **Then** the hex input field updates to the selected color's hex code
- **And** the clicked circle is visually highlighted as selected

### Hex input syncs with palette

- **Given** the user is on the create tag page
- **When** the user types a hex code that matches a predefined palette color (e.g., `#EF4444`)
- **Then** the corresponding palette circle is visually highlighted

### Custom hex color accepted

- **Given** the user is on the create tag page
- **When** the user types a valid hex code not in the palette (e.g., `#1A2B3C`)
- **Then** no palette circle is highlighted
- **And** the color is accepted as valid

### Successful creation redirects to list

- **Given** the user fills in a valid name and color
- **When** the user submits the form
- **Then** the tag is created via POST `/api/tags`
- **And** the browser redirects to `/tags`
- **And** the new tag is visible in the list

### Duplicate name shows error

- **Given** a tag with name "VIP" already exists
- **When** the user tries to create another tag with name "VIP"
- **Then** the API returns 409 Conflict
- **And** the form displays "A tag with this name already exists"

### Cancel navigates back to list

- **Given** the user is on the create tag page
- **When** the user clicks "Cancel"
- **Then** the browser navigates back to `/tags`
- **And** no tag is created

### Save button disabled while submitting

- **Given** the user submitted the create form
- **When** the API call is in progress
- **Then** the save button is disabled to prevent double submission

## Tag Edit

### Edit button navigates to edit page

- **Given** a tag exists in the list
- **When** the user clicks the edit (pencil) icon on a tag row
- **Then** the browser navigates to `/tags/{id}/edit`

### Edit page shows pre-filled form

- **Given** a tag with name "VIP", description "Important clients", color "#EF4444" exists
- **When** the user navigates to `/tags/{id}/edit`
- **Then** the form is pre-filled with the tag's current values
- **And** the page title indicates "Edit Tag"
- **And** the matching palette circle is highlighted (if the color matches a predefined one)

### Edit page shows 404 for non-existent tag

- **Given** no tag with the given ID exists
- **When** the user navigates to `/tags/{id}/edit`
- **Then** the Next.js 404 page is shown

### Successful edit redirects to list

- **Given** the user changes the tag name from "VIP" to "Premium"
- **When** the user submits the form
- **Then** the tag is updated via PUT `/api/tags/{id}`
- **And** the browser redirects to `/tags`

### Duplicate name on edit shows error

- **Given** tags "VIP" and "Premium" exist
- **When** the user edits "Premium" and changes the name to "VIP"
- **Then** the API returns 409 Conflict
- **And** the form displays "A tag with this name already exists"

## Tag Display on Detail Views

### Tags shown on company detail view

- **Given** a company has tags "VIP" (red) and "Partner" (blue) assigned
- **When** the user views the company detail page
- **Then** a "Tags" section is shown between the detail card and comments
- **And** two colored chips are displayed with the tag names
- **And** chip background colors match the tag colors

### Tags shown on contact detail view

- **Given** a contact has tags assigned
- **When** the user views the contact detail page
- **Then** a "Tags" section is shown with colored chips

### Tags section hidden when no tags assigned

- **Given** a company or contact has no tags assigned
- **When** the user views the detail page
- **Then** no "Tags" section is rendered

### Tag chip text has sufficient contrast

- **Given** a tag has a dark color (e.g., `#1A1A1A`)
- **When** the chip is rendered
- **Then** the text is white for readability

### Tag chip text dark on light background

- **Given** a tag has a light color (e.g., `#EAB308`)
- **When** the chip is rendered
- **Then** the text is dark for readability

### Tags wrap on overflow

- **Given** an entity has many tags assigned
- **When** the chips exceed the container width
- **Then** the chips wrap to the next line (flex-wrap)

## Tag Assignment in Edit Forms

### Tag selector shown in company edit form

- **Given** the user navigates to the company edit page
- **When** the form renders
- **Then** a "Tags" field with a multi-select dropdown is shown

### Tag selector shown in contact edit form

- **Given** the user navigates to the contact edit page
- **When** the form renders
- **Then** a "Tags" field with a multi-select dropdown is shown

### Existing tags pre-selected

- **Given** a company has tags "VIP" and "Partner" assigned
- **When** the user opens the company edit form
- **Then** the tag selector shows "VIP" and "Partner" as selected chips

### Adding a tag via selector

- **Given** the user is editing a company
- **When** the user opens the tag dropdown and checks a previously unchecked tag
- **Then** the tag appears as a chip in the selector trigger

### Removing a tag via selector

- **Given** a company has tag "VIP" assigned
- **When** the user opens the tag dropdown and unchecks "VIP"
- **Then** the "VIP" chip is removed from the selector trigger

### Unchanged tags not sent to API

- **Given** a company has tags assigned
- **When** the user edits other fields but does not interact with the tag selector
- **Then** the `tagIds` field is omitted from the update request
- **And** existing tag assignments are preserved

### Cleared tags sent as empty array

- **Given** a company has tags "VIP" and "Partner" assigned
- **When** the user removes all tags via the selector and saves
- **Then** `tagIds: []` is sent in the update request
- **And** all tag assignments are removed

### Selected tags sent as ID array

- **Given** the user selects tags "VIP" and "New" for a company
- **When** the user saves the form
- **Then** `tagIds: [vip-id, new-id]` is sent in the update request

### Tag selector shows color indicators

- **Given** tags with different colors exist
- **When** the user opens the tag dropdown
- **Then** each tag option shows a small color circle next to its name

### Tag selector available in create forms

- **Given** the user is on the company or contact create page
- **When** the form renders
- **Then** the tag multi-select is available with no tags pre-selected

## i18n

### Tag UI available in English

- **Given** the language is set to English
- **When** the user interacts with tag pages
- **Then** all labels, buttons, messages, and errors are in English

### Tag UI available in German

- **Given** the language is set to German
- **When** the user interacts with tag pages
- **Then** all labels, buttons, messages, and errors are in German

### Language switch applies to tag pages

- **Given** the user is on the tag list page
- **When** the user switches the language
- **Then** all tag-related text updates immediately without page reload
