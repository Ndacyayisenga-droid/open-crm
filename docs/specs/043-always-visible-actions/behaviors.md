# Behaviors: Always-Visible Action Icons

## Icon Visibility

### Icons visible without hover

- **Given** a company detail view with a filled email field (has copy + mailto actions)
- **When** the page renders without any mouse interaction
- **Then** the copy and mailto icons are visible

### Icons visible for all action types

- **Given** a contact detail view with filled email, LinkedIn, and phone fields
- **When** the page renders without any mouse interaction
- **Then** action icons are visible on email (copy, mailto), LinkedIn (copy, link), and phone (copy, tel)

### Icons still hidden for empty fields

- **Given** a company with email set to null
- **When** the company detail view is displayed
- **Then** the email field shows "—" and no action icons are visible

## Color Behavior — Desktop

### Icons in light color by default on desktop

- **Given** a desktop browser with a mouse (hover-capable device)
- **And** a company detail view with a filled website field
- **When** the page renders without hovering
- **Then** the action icons are displayed in `oe-gray-light` color

### Icons darken on hover on desktop

- **Given** a desktop browser with a mouse
- **And** a company detail view with a filled website field
- **When** the user hovers over an action icon
- **Then** the icon color changes to `oe-dark`

### Icons return to light color after hover leaves

- **Given** a desktop browser with a mouse
- **And** the user is hovering over an action icon (showing `oe-dark`)
- **When** the mouse leaves the icon
- **Then** the icon color returns to `oe-gray-light`

## Color Behavior — Touch Devices

### Icons in dark color on touch devices

- **Given** a touch device (tablet, phone) without hover capability
- **And** a contact detail view with a filled email field
- **When** the page renders
- **Then** the action icons are displayed in `oe-dark` color

## Copy Feedback Unchanged

### Copy checkmark still shows in green

- **Given** a field with a visible copy icon
- **When** the user clicks the copy icon
- **Then** the icon changes to a green checkmark for approximately 2 seconds
- **And** then reverts to the copy icon in its default color
