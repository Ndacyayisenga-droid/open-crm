# Behaviors: OE Branding

## App Identity

### App name shown in sidebar header

- **Given** the sidebar is displayed (desktop or mobile)
- **When** the page renders
- **Then** the sidebar header shows "Open CRM" as the app name

### Placeholder app logo shown

- **Given** the sidebar is displayed
- **When** the page renders
- **Then** a placeholder icon is shown next to "Open CRM"

### App name links to companies page

- **Given** the sidebar is displayed
- **When** the user clicks on the app name or placeholder logo
- **Then** the user navigates to `/companies`

## Developer Branding

### OE branding shown below app name

- **Given** the sidebar is displayed
- **When** the page renders
- **Then** below the app name, "Developed by" is shown followed by the Open Elements landscape logo

### OE branding links to website

- **Given** the sidebar is displayed
- **When** the user clicks on "Developed by" or the OE logo
- **Then** `https://open-elements.com` opens in a new browser tab

### OE logo uses dark-background variant

- **Given** the sidebar has a dark background (`oe-dark`)
- **When** the page renders
- **Then** the OE landscape logo is the dark-background variant (light text/graphics on transparent background)

## Desktop Sidebar

### Header layout on desktop

- **Given** the browser window is at desktop width (md breakpoint or larger)
- **When** the sidebar renders
- **Then** the header shows:
  - Row 1: placeholder logo icon + "Open CRM"
  - Row 2: "Developed by" + OE landscape logo
- **And** the header is separated from the nav links by a border

## Mobile Sidebar

### Header layout in mobile drawer

- **Given** the browser window is below md breakpoint
- **When** the user opens the mobile sidebar drawer
- **Then** the header shows the same layout as desktop:
  - Row 1: placeholder logo icon + "Open CRM"
  - Row 2: "Developed by" + OE landscape logo

## Styling

### Branding text is subdued

- **Given** the sidebar is displayed
- **When** the "Developed by" text is visible
- **Then** it is displayed in a small font size with a subdued color (lighter than the app name)

### Branding has hover effect

- **Given** the sidebar is displayed
- **When** the user hovers over the OE branding row
- **Then** a subtle visual change indicates it is clickable (e.g., opacity change)
