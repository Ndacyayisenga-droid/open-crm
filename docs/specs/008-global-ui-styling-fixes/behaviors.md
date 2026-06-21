# Behaviors: Global UI Styling Fixes

## Dialog Background

### Dialog renders with opaque white background

- **Given** the user is on the Company detail page
- **When** the user clicks the "Delete" button and the AlertDialog opens
- **Then** the dialog content has a solid white background (`#ffffff`), a visible border, and a drop shadow

### Dialog overlay dims the page behind it

- **Given** the user is on any page
- **When** a dialog opens
- **Then** the area behind the dialog is dimmed with a semi-transparent black overlay (`bg-black/50`)

### Dialog text is readable

- **Given** a dialog is open
- **When** the user looks at the dialog title and description
- **Then** the title is rendered in black (`#000000`) and the description in muted gray (`#b0aea5`)

## Select / Combobox Background

### Select dropdown renders with opaque white background

- **Given** the user is on the Company list page
- **When** the user clicks the sort dropdown (Select component)
- **Then** the dropdown content has a solid white background (`#ffffff`), a visible border, and a drop shadow

### Select items are readable and interactive

- **Given** the sort dropdown is open
- **When** the user hovers over a select item
- **Then** the hovered item shows a light gray accent background (`#e8e6dc`) and the text remains readable

### Select focus ring uses brand color

- **Given** the user navigates via keyboard
- **When** the select trigger receives focus
- **Then** the focus ring is displayed in the brand green color (`#5CBA9E`)

## Table Borders

### Table row borders are subtle

- **Given** the user is on the Company list page with multiple companies
- **When** the table renders
- **Then** the borders between rows are light gray (`#e8e6dc`), clearly more subtle than browser-default black

### Table header remains visually distinct from rows

- **Given** the user is on the Company list page
- **When** the table renders
- **Then** the table header row is visually distinct from data rows through its bold font weight (`font-medium`)

### Table row hover state is visible

- **Given** the user is on the Company list page
- **When** the user hovers over a table row
- **Then** the row shows a subtle muted background highlight

## Global Consistency

### All shadcn/ui components use brand-consistent colors

- **Given** a new shadcn/ui component is added to the project
- **When** it references semantic tokens like `bg-background`, `bg-popover`, `text-foreground`, or `border`
- **Then** it renders with the correct Open Elements brand colors without any additional configuration

### Primary action buttons use brand green

- **Given** any page with a primary action button (variant "default")
- **When** the button renders
- **Then** the button background is `#5CBA9E` (oe-green) with white text

### Destructive buttons use brand red

- **Given** any page with a destructive action button
- **When** the button renders
- **Then** the button background is `#E63277` (oe-red) with white text

### Input fields have visible borders

- **Given** any form page (e.g., Company create/edit)
- **When** form input fields render
- **Then** the input border is light gray (`#e8e6dc`), clearly visible against the white background

### Focus indicators are consistent

- **Given** the user navigates any form via keyboard
- **When** an input, button, or select receives focus
- **Then** the focus ring uses the brand green color (`#5CBA9E`)

## Edge Cases

### Skeleton loading states are visible

- **Given** the Company list is loading
- **When** skeleton placeholders render
- **Then** the skeletons use the accent color (`#e8e6dc`) and are clearly visible against the white background

### Archived company rows remain visually distinct

- **Given** the Company list shows archived (soft-deleted) companies
- **When** the table renders
- **Then** archived rows still have `opacity-50` and the subtle row border remains proportionally visible

### Sheet (mobile sidebar) has opaque background

- **Given** the user is on a mobile viewport
- **When** the hamburger menu is tapped and the Sheet opens
- **Then** the Sheet content has a solid white background, not transparent