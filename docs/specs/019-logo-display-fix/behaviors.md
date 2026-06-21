# Behaviors: Company Logo Display Fix

## Company List

### Logo is fully visible in list table

- **Given** a company with a wide rectangular logo (e.g., 400x100px)
- **When** the company list is displayed
- **Then** the logo is shown fully within the 32x32 thumbnail area without any cropping

### Logo has sharp corners in list table

- **Given** a company with an uploaded logo
- **When** the company list is displayed
- **Then** the logo thumbnail has no border-radius (sharp rectangular corners)

### Placeholder icon unchanged when no logo exists

- **Given** a company without a logo
- **When** the company list is displayed
- **Then** the Building2 placeholder icon is shown at the same size as before

## Company Detail

### Logo is fully visible in detail header

- **Given** a company with a tall narrow logo (e.g., 100x400px)
- **When** the company detail view is displayed
- **Then** the logo is shown fully within the 96x96 area without any cropping

### Logo has sharp corners in detail header

- **Given** a company with an uploaded logo
- **When** the company detail view is displayed
- **Then** the logo has no border-radius (sharp rectangular corners)

### Placeholder icon unchanged in detail view

- **Given** a company without a logo
- **When** the company detail view is displayed
- **Then** the Building2 placeholder icon is shown at the same size as before

## Company Form

### Logo preview is fully visible in form

- **Given** a company with a non-square logo
- **When** the company edit form is displayed
- **Then** the logo preview is shown fully within the 64x64 area without any cropping

### Logo preview has sharp corners in form

- **Given** a company with an uploaded logo
- **When** the company edit form is displayed
- **Then** the logo preview has no border-radius (sharp rectangular corners)

## Contact Photos (unchanged)

### Contact photos still use object-cover

- **Given** a contact with a non-square photo
- **When** the contact list or detail view is displayed
- **Then** the photo is displayed with cropping (object-cover) and circular shape (rounded-full) — no change from current behavior

## Edge Cases

### Square logo displays at full container size

- **Given** a company with a square logo (e.g., 200x200px)
- **When** the logo is displayed in any view (list, detail, form)
- **Then** the logo fills the entire container without any empty space

### Extremely wide logo displays small but complete

- **Given** a company with an extremely wide logo (e.g., 1000x50px)
- **When** the logo is displayed in the 32x32 list thumbnail
- **Then** the logo is scaled down to fit the width, appearing very small vertically, but fully visible

### SVG logo with transparent background

- **Given** a company with an SVG logo on a transparent background
- **When** the logo is displayed
- **Then** the logo is shown with the page background visible behind it (no special handling)
