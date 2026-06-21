# Behaviors: Move Archive Toggle into Company Filter Row

## Layout

### All filters are in a single row

- **Given** the user views the company list
- **When** the page loads
- **Then** the name filter, Brevo dropdown, and archive toggle are in the same row
- **And** there is no separate row for the archive toggle

### Archive toggle still works

- **Given** the archive toggle is in the filter row
- **When** the user clicks the toggle
- **Then** archived companies are shown or hidden as before

### Layout wraps on small screens

- **Given** the user views the company list on a narrow screen
- **When** the filter row wraps
- **Then** the archive toggle wraps gracefully along with the other filter elements
