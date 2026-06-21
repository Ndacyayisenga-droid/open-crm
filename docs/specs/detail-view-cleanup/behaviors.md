# Behaviors: Detail View Cleanup

## Contact: Name Fields Removed

### Name not shown as detail field

- **Given** a contact with firstName "Anna" and lastName "Schmidt"
- **When** the contact detail view is displayed
- **Then** the header shows "Anna Schmidt"
- **And** there is no DetailField labeled "First Name" / "Vorname"
- **And** there is no DetailField labeled "Last Name" / "Nachname"

## Contact: Language Display

### Language shown as translated label (DE UI)

- **Given** the UI language is set to German
- **And** a contact has language "DE"
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "Deutsch"

### Language shown as translated label for EN contact (DE UI)

- **Given** the UI language is set to German
- **And** a contact has language "EN"
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "Englisch"

### Language shown as translated label (EN UI)

- **Given** the UI language is set to English
- **And** a contact has language "DE"
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "German"

### Language shown as translated label for EN contact (EN UI)

- **Given** the UI language is set to English
- **And** a contact has language "EN"
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "English"

### Null language shown as unknown (DE UI)

- **Given** the UI language is set to German
- **And** a contact has language null
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "Unbekannt"

### Null language shown as unknown (EN UI)

- **Given** the UI language is set to English
- **And** a contact has language null
- **When** the contact detail view is displayed
- **Then** the language DetailField shows "Unknown"

## Company: Merged Address Block

### Full address displayed as multi-line block

- **Given** a company with street "Musterstraße", houseNumber "42", zipCode "12345", city "Berlin", country "Deutschland"
- **When** the company detail view is displayed
- **Then** a single "Address" / "Adresse" field shows:
  ```
  Musterstraße 42
  12345 Berlin
  Deutschland
  ```

### Address without house number

- **Given** a company with street "Hauptstraße", houseNumber null, zipCode "80331", city "München", country "Deutschland"
- **When** the company detail view is displayed
- **Then** the address field shows:
  ```
  Hauptstraße
  80331 München
  Deutschland
  ```

### Address without street (house number ignored)

- **Given** a company with street null, houseNumber "7", zipCode "10115", city "Berlin", country "Deutschland"
- **When** the company detail view is displayed
- **Then** the address field shows:
  ```
  10115 Berlin
  Deutschland
  ```
- **And** the house number "7" is not displayed

### Address with only city and country

- **Given** a company with street null, houseNumber null, zipCode null, city "Hamburg", country "Deutschland"
- **When** the company detail view is displayed
- **Then** the address field shows:
  ```
  Hamburg
  Deutschland
  ```

### Address with only zip code

- **Given** a company with street null, houseNumber null, zipCode "50667", city null, country null
- **When** the company detail view is displayed
- **Then** the address field shows:
  ```
  50667
  ```

### Address with only country

- **Given** a company with street null, houseNumber null, zipCode null, city null, country "Deutschland"
- **When** the company detail view is displayed
- **Then** the address field shows:
  ```
  Deutschland
  ```

### All address fields null

- **Given** a company with street null, houseNumber null, zipCode null, city null, country null
- **When** the company detail view is displayed
- **Then** the address field shows "—"

### Address label is translated

- **Given** the UI language is set to German
- **When** the company detail view is displayed
- **Then** the address field label shows "Adresse"

- **Given** the UI language is set to English
- **When** the company detail view is displayed
- **Then** the address field label shows "Address"

## Unchanged Behavior

### Edit forms remain unchanged

- **Given** the contact edit form is opened
- **When** the form is displayed
- **Then** firstName and lastName are still separate input fields

- **Given** the company edit form is opened
- **When** the form is displayed
- **Then** street, houseNumber, zipCode, city, and country are still separate input fields

### Print views remain unchanged

- **Given** the contact print view is opened
- **When** the print table is displayed
- **Then** the name column still shows the concatenated full name (no change)

### List tables remain unchanged

- **Given** the contact list table is displayed
- **Then** the Name column still shows the concatenated full name (no change)
