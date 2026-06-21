# Behaviors: Extract App Components into UI Package

## LanguageSwitch

### Renders two language buttons

- **Given** the `LanguageProvider` is mounted with active language "de"
- **When** the `LanguageSwitch` component renders
- **Then** it shows two buttons labeled "DE" and "EN", with "DE" highlighted in green and bold

### Switching language updates the provider

- **Given** the active language is "de"
- **When** the user clicks the "EN" button
- **Then** the active language changes to "en", "EN" becomes highlighted, and "DE" becomes unhighlighted

## HealthStatus

### Shows healthy status

- **Given** a `HealthStatus` component with `healthy={true}`
- **When** it renders
- **Then** it shows a green circle indicator and the "Running" status text

### Shows unhealthy status

- **Given** a `HealthStatus` component with `healthy={false}`
- **When** it renders
- **Then** it shows a red circle indicator and the "Down" status text

### Renders inside a Card with title

- **Given** a `HealthStatus` component
- **When** it renders
- **Then** it is wrapped in a Card with a "Server Status" title heading

## TagChips

### Renders colored chips for each tag

- **Given** a `TagChips` component with `tags=[{ id: "1", name: "VIP", color: "#EF4444" }]`
- **When** it renders
- **Then** it shows a chip with text "VIP", background color `#EF4444`, and white text (dark background → light text)

### Computes correct contrast color for light backgrounds

- **Given** a tag with color `#F59E0B` (yellow/light)
- **When** the chip renders
- **Then** the text color is dark (`#1A1A1A`) for readability

### Returns null for empty tags array

- **Given** a `TagChips` component with `tags=[]`
- **When** it renders
- **Then** it returns null (nothing rendered)

### Renders label when provided

- **Given** a `TagChips` component with `tags=[...]` and `label="Tags"`
- **When** it renders
- **Then** it shows a "Tags" heading above the chips

### Omits label heading when not provided

- **Given** a `TagChips` component with `tags=[...]` and no `label` prop
- **When** it renders
- **Then** it renders the chips without a heading

### Handles invalid hex color gracefully

- **Given** a tag with `color="invalid"`
- **When** the chip renders
- **Then** it falls back to a neutral gray background (`#6B7280`) with white text

## TagForm

### Renders create mode without initial values

- **Given** a `TagForm` component without a `tag` prop
- **When** it renders
- **Then** the name input, description textarea, and color picker are empty

### Renders edit mode with pre-filled values

- **Given** a `TagForm` component with `tag={ name: "VIP", description: "Important", color: "#EF4444" }`
- **When** it renders
- **Then** the name input shows "VIP", description shows "Important", and color shows "#EF4444" with the corresponding palette button selected

### Validates required name field

- **Given** the name input is empty
- **When** the user clicks the save button
- **Then** a validation error for the name field is shown and `onSave` is not called

### Validates required color field

- **Given** the color input is empty
- **When** the user clicks the save button
- **Then** a validation error for the color field is shown and `onSave` is not called

### Validates hex color format

- **Given** the color input contains "red"
- **When** the user clicks the save button
- **Then** a validation error for invalid hex format is shown and `onSave` is not called

### Calls onSave with validated data

- **Given** name is "VIP", description is "Important clients", color is "#EF4444"
- **When** the user clicks the save button
- **Then** `onSave` is called with `{ name: "VIP", description: "Important clients", color: "#EF4444" }`

### Trims whitespace from name and description

- **Given** name is "  VIP  " and description is "  Important  "
- **When** the user clicks the save button
- **Then** `onSave` is called with `{ name: "VIP", description: "Important" }`

### Shows conflict error from onSave rejection

- **Given** the `onSave` callback throws an Error with message "CONFLICT"
- **When** the save operation completes
- **Then** the `nameConflict` translation text is shown as an error on the name field

### Calls onCancel when cancel button is clicked

- **Given** the form is rendered
- **When** the user clicks the cancel button
- **Then** `onCancel` is called

### Color palette selection updates color input

- **Given** the color input is empty
- **When** the user clicks a palette color button (e.g., red `#EF4444`)
- **Then** the color input value is set to "#EF4444" and the palette button shows a checkmark

### Disables save button while submitting

- **Given** the user has clicked save and `onSave` is still pending
- **When** the form renders during submission
- **Then** the save button is disabled

## Sidebar

### Desktop layout

#### Renders fixed sidebar on desktop

- **Given** the viewport width is >= 768px (md breakpoint)
- **When** the Sidebar renders
- **Then** a fixed sidebar is visible on the left with 256px width

#### Renders navigation items in top slot

- **Given** the Sidebar has NavItem children
- **When** it renders on desktop
- **Then** the nav items are displayed in the main navigation area below the header

#### Renders bottom slot items at the bottom

- **Given** the Sidebar has `bottomChildren` with NavItems
- **When** it renders on desktop
- **Then** the bottom children are pushed to the bottom of the sidebar (above language switch and user section)

### Mobile layout

#### Hides sidebar and shows hamburger on mobile

- **Given** the viewport width is < 768px
- **When** the page renders
- **Then** the sidebar is hidden and a top header bar with hamburger menu button is shown

#### Opens Sheet when hamburger is clicked

- **Given** mobile viewport with hamburger visible
- **When** the user clicks the hamburger button
- **Then** a Sheet slides in from the left containing the full sidebar content

#### Closes Sheet on navigation

- **Given** the mobile Sheet is open
- **When** the user clicks a NavItem
- **Then** the Sheet closes

### SidebarHeader

#### Renders app title and branding

- **Given** a Sidebar with `appTitle="Open CRM"`
- **When** the header renders
- **Then** it shows "Open CRM" as the title with a dashboard icon, and "Developed by Open Elements" with the OE logo below

#### Links to home route

- **Given** a SidebarHeader with default `homeHref="/"`
- **When** the user clicks the title
- **Then** the link points to "/"

### NavItem

#### Renders active state

- **Given** a NavItem with `active={true}`
- **When** it renders
- **Then** it has green highlight styling (`bg-oe-green/20 text-oe-green`)

#### Renders inactive state

- **Given** a NavItem with `active={false}` or no active prop
- **When** it renders
- **Then** it has muted styling with hover effect

#### Renders indented when inside CollapsibleGroup

- **Given** a NavItem with `indented={true}`
- **When** it renders
- **Then** it has additional left padding

### CollapsibleGroup

#### Renders collapsed by default

- **Given** a CollapsibleGroup with `defaultOpen={false}` (or no prop)
- **When** it renders
- **Then** the child NavItems are hidden and the chevron points sideways

#### Expands on click

- **Given** a collapsed CollapsibleGroup
- **When** the user clicks the group header
- **Then** the child NavItems become visible and the chevron points down

#### Renders expanded by default when defaultOpen is true

- **Given** a CollapsibleGroup with `defaultOpen={true}`
- **When** it renders
- **Then** the child NavItems are visible

#### Shows active styling when any child is active

- **Given** a CollapsibleGroup with `active={true}`
- **When** it renders
- **Then** the group header text is white (highlighted)

### UserSection

#### Renders username and avatar

- **Given** a UserSection with `userName="Max"` and `avatarUrl="/avatar.jpg"`
- **When** it renders
- **Then** it shows the avatar image and "Max" text

#### Renders fallback icon when no avatar

- **Given** a UserSection with `userName="Max"` and no `avatarUrl`
- **When** it renders
- **Then** it shows a CircleUser icon instead of an image

#### Shows roles in tooltip

- **Given** a UserSection with `roles=["ADMIN", "USER"]`
- **When** hovering over the username
- **Then** a tooltip shows "ADMIN, USER"

#### Shows no-roles message when roles empty

- **Given** a UserSection with `roles=[]`
- **When** hovering over the username
- **Then** a tooltip shows the `noRoles` translation text

#### Calls onLogout when logout button clicked

- **Given** a UserSection is rendered
- **When** the user clicks the logout button
- **Then** `onLogout` is called

#### Calls onAvatarClick when avatar clicked

- **Given** a UserSection with `onAvatarClick` defined
- **When** the user clicks the avatar
- **Then** `onAvatarClick` is called

#### UserSection is omitted when user prop is not provided

- **Given** a Sidebar without the `user` prop
- **When** it renders
- **Then** no UserSection is shown (no username, no avatar, no logout)

### LanguageSwitch integration

#### LanguageSwitch is rendered in the sidebar

- **Given** a Sidebar with LanguageProvider context available
- **When** it renders
- **Then** the LanguageSwitch appears between the navigation area and the UserSection

## Exports

### All components are importable from UI package

- **Given** an app depends on `@open-elements/ui`
- **When** importing `LanguageSwitch`, `HealthStatus`, `TagChips`, `TagForm`, `Sidebar`, `NavItem`, `CollapsibleGroup`, `UserSection`, `SidebarHeader`
- **Then** all imports resolve successfully

## Tests

### All new components have unit tests

- **Given** all 5 components are extracted into the UI package
- **When** running the UI package test suite
- **Then** tests exist and pass for LanguageSwitch, HealthStatus, TagChips, TagForm, and Sidebar (including NavItem, CollapsibleGroup, UserSection)

### All existing UI package tests still pass

- **Given** the existing tests for Button, Input, Textarea, TagMultiSelect, cn
- **When** running the test suite after adding the new components
- **Then** all existing tests pass without modification
