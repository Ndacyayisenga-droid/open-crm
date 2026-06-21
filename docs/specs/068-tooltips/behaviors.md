# Behaviors: Tooltips

## Tooltip Display

### Tooltip appears on hover

- **Given** an icon-only action button is visible
- **When** the user hovers over the button
- **Then** a styled tooltip appears with the button's description

### Tooltip disappears on mouse leave

- **Given** a tooltip is visible
- **When** the user moves the mouse away from the button
- **Then** the tooltip disappears

### Tooltip not shown on touch devices

- **Given** the user is on a touch device (no mouse)
- **When** the user taps an action button
- **Then** the button action executes without showing a tooltip

## Company List Actions

### Edit button tooltip

- **Given** the company list is displayed
- **When** the user hovers over the Edit (Pencil) button on a row
- **Then** a tooltip shows "Bearbeiten" / "Edit"

### Add comment button tooltip

- **Given** the company list is displayed
- **When** the user hovers over the Comment (MessageSquarePlus) button
- **Then** a tooltip shows the add comment text

### Create task button tooltip

- **Given** the company list is displayed
- **When** the user hovers over the Create Task (CheckSquare) button
- **Then** a tooltip shows "Aufgabe erstellen" / "Create Task"

### Delete button tooltip

- **Given** the company list is displayed
- **When** the user hovers over the Delete (Trash2) button
- **Then** a tooltip shows "Löschen" / "Delete"

### Restore button tooltip

- **Given** the company list shows archived companies
- **When** the user hovers over the Restore (RotateCcw) button
- **Then** a tooltip shows "Wiederherstellen" / "Restore"

## Contact List Actions

### All contact list action buttons have tooltips

- **Given** the contact list is displayed
- **When** the user hovers over any action button (Edit, Comment, Create Task, Delete)
- **Then** the corresponding tooltip is shown

## Task List Actions

### Edit button tooltip

- **Given** the task list is displayed
- **When** the user hovers over the Edit (Pencil) button
- **Then** a tooltip shows "Bearbeiten" / "Edit"

## Tag List Actions

### Edit button tooltip

- **Given** the tag list is displayed
- **When** the user hovers over the Edit (Pencil) button
- **Then** a tooltip shows "Bearbeiten" / "Edit"

### Delete button tooltip

- **Given** the tag list is displayed
- **When** the user hovers over the Delete (Trash2) button
- **Then** a tooltip shows "Löschen" / "Delete"

## Detail Views

### Company detail action tooltips

- **Given** the company detail view is displayed
- **When** the user hovers over Edit, Delete, or Create Task buttons
- **Then** the corresponding tooltip is shown

### Contact detail action tooltips

- **Given** the contact detail view is displayed
- **When** the user hovers over Edit, Delete, or Create Task buttons
- **Then** the corresponding tooltip is shown

### Task detail action tooltips

- **Given** the task detail view is displayed
- **When** the user hovers over Edit or Delete buttons
- **Then** the corresponding tooltip is shown

## Detail Field Actions

### Copy action tooltip

- **Given** a detail field with a copy action is displayed
- **When** the user hovers over the Copy icon
- **Then** a tooltip shows "Copy" / "Kopieren"

### Email action tooltip

- **Given** a detail field with an email action is displayed
- **When** the user hovers over the Mail icon
- **Then** a tooltip shows "Email"

### Call action tooltip

- **Given** a detail field with a call action is displayed
- **When** the user hovers over the Phone icon
- **Then** a tooltip shows "Call" / "Anrufen"

### Link action tooltip

- **Given** a detail field with a link action is displayed
- **When** the user hovers over the ExternalLink icon
- **Then** a tooltip shows "Open" / "Öffnen"

## Comment Actions

### Comment delete tooltip

- **Given** a comment with a delete button is displayed
- **When** the user hovers over the X icon
- **Then** a tooltip shows "Löschen" / "Delete"

## Sidebar

### Logout button tooltip

- **Given** the sidebar is displayed
- **When** the user hovers over the Logout (LogOut) icon
- **Then** a tooltip shows "Abmelden" / "Logout"

### Hamburger menu tooltip (mobile)

- **Given** the mobile view is displayed with the hamburger menu button
- **When** the user hovers over the Menu icon
- **Then** a tooltip shows "Menü" / "Menu"

## i18n

### Tooltips in German

- **Given** the language is set to German
- **When** the user hovers over any action button
- **Then** the tooltip text is in German

### Tooltips in English

- **Given** the language is set to English
- **When** the user hovers over any action button
- **Then** the tooltip text is in English

## No Native Title Attribute

### Title attribute removed

- **Given** a button previously had a native HTML `title` attribute
- **When** the Tooltip component is applied
- **Then** the native `title` attribute is removed (no duplicate tooltip)
