# Design: CSV Export

## GitHub Issue

—

## Summary

Add CSV export functionality to the Company and Contact list views. Users can download the currently filtered dataset as a CSV file with dynamically selected columns. A modal dialog with checkboxes lets the user choose which fields to include. The CSV is generated on the backend using a dedicated library.

## Goals

- Allow users to export filtered Company and Contact data as CSV
- Provide column selection via a checkbox dialog in the frontend
- Generate the CSV on the backend for consistent formatting and performance
- Respect the same filters as the list view (name/search, Brevo, archive, language, companyId)

## Non-goals

- Excel (XLSX) export
- Scheduled/automated exports
- Export of comments or images
- Saving column selection preferences

## Technical Approach

### Backend

#### CSV Library

Add **Apache Commons CSV** to the backend dependencies. It is the most widely used Java CSV library, handles quoting/escaping correctly, and integrates well with Spring Boot.

**Rationale:** Commons CSV is mature, well-documented, and handles edge cases (commas in values, newlines, quoting) out of the box. OpenCSV is an alternative but Commons CSV has better API ergonomics for streaming writes.

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.12.0</version>
</dependency>
```

#### Column Enums

Define two enums for exportable columns:

```java
public enum CompanyExportColumn {
    NAME, EMAIL, WEBSITE, STREET, HOUSE_NUMBER, ZIP_CODE, CITY, COUNTRY,
    CONTACT_COUNT, COMMENT_COUNT, BREVO, CREATED_AT, UPDATED_AT
}

public enum ContactExportColumn {
    FIRST_NAME, LAST_NAME, EMAIL, POSITION, GENDER, LINKED_IN_URL,
    PHONE_NUMBER, COMPANY_NAME, BIRTHDAY, LANGUAGE,
    COMMENT_COUNT, BREVO, CREATED_AT, UPDATED_AT
}
```

Each enum value maps to a DTO field name (used as CSV header) and a value extractor function.

#### REST Endpoints

Two new endpoints following the existing controller patterns:

**`GET /api/companies/export`**
- Parameters: same filters as `GET /api/companies` (`name`, `includeDeleted`, `brevo`) plus `columns` (list of `CompanyExportColumn` enum values)
- Response: `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="companies.csv"`
- No pagination — returns all matching records
- Body: UTF-8 with BOM, comma-separated, technical field names as header row

**`GET /api/contacts/export`**
- Parameters: same filters as `GET /api/contacts` (`search`, `companyId`, `language`, `brevo`) plus `columns` (list of `ContactExportColumn` enum values)
- Response: `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="contacts.csv"`
- No pagination — returns all matching records
- Body: UTF-8 with BOM, comma-separated, technical field names as header row

**Rationale:** GET is appropriate because the export is a read operation. The column list is passed as a repeated query parameter (e.g., `?columns=NAME&columns=EMAIL`).

#### CSV Format

- **Separator:** Comma (`,`)
- **Encoding:** UTF-8 with BOM (`\uFEFF` prefix) for Excel compatibility on Windows
- **Header row:** Technical field names from enum (e.g., `name`, `email`, `website`)
- **Quoting:** Automatic via Commons CSV (quotes values containing commas, newlines, or quotes)
- **Empty results:** Returns valid CSV with header row only (no data rows)

#### Implementation Pattern

Reuse the existing `Specification`-based filtering from the service layer. Add a method that returns `List<CompanyDto>` / `List<ContactDto>` (unpaginated) using the same filter specs. The controller streams the CSV directly to the `HttpServletResponse` output stream.

```java
@GetMapping("/export")
public void exportCsv(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Boolean brevo,
    @RequestParam(defaultValue = "false") boolean includeDeleted,
    @RequestParam List<CompanyExportColumn> columns,
    HttpServletResponse response
) throws IOException {
    // Set response headers
    // Fetch all matching records (no pagination)
    // Write CSV using Commons CSV
}
```

### Frontend

#### Modal Dialog

When the user clicks the CSV button, a modal dialog opens with:
- Title: "CSV Export" (translated)
- Checkboxes for each available column, all checked by default
- Column labels translated to the current UI language
- "Select All" / "Deselect All" convenience controls
- "Download" button (primary action) and "Cancel" button

#### API Integration

Add a new API function that constructs the export URL with filter and column parameters, then triggers a browser download (e.g., via a temporary `<a>` element with `download` attribute, or `window.location`).

#### Button Placement

The CSV button is placed next to the print button in the header toolbar, with the same styling but a different icon (e.g., `Download` or `FileDown` from lucide-react) and translated label text.

The button is **disabled** when the table shows 0 results.

### Files Affected

**Backend (new files):**
- `CompanyExportColumn.java` — enum
- `ContactExportColumn.java` — enum

**Backend (modified):**
- `pom.xml` — add Commons CSV dependency
- `CompanyRestController.java` — add export endpoint
- `ContactRestController.java` — add export endpoint
- `CompanyService.java` — add unpaginated list method
- `ContactService.java` — add unpaginated list method

**Frontend (new files):**
- `CsvExportDialog` component — modal with column checkboxes

**Frontend (modified):**
- `frontend/src/lib/api.ts` — add export URL builder
- `frontend/src/lib/i18n/de.ts` — add CSV export translations
- `frontend/src/lib/i18n/en.ts` — add CSV export translations
- `frontend/src/components/company-list.tsx` — add CSV button
- `frontend/src/components/contact-list.tsx` — add CSV button

## API Design

### Company CSV Export

```
GET /api/companies/export?name=&includeDeleted=false&brevo=&columns=NAME&columns=EMAIL&columns=WEBSITE
```

**Response Headers:**
```
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="companies.csv"
```

**Response Body (example):**
```csv
name,email,website
"Acme Corp","info@acme.com","https://acme.com"
"Test GmbH","","https://test.de"
```

### Contact CSV Export

```
GET /api/contacts/export?search=&companyId=&language=&brevo=&columns=FIRST_NAME&columns=LAST_NAME&columns=EMAIL
```

**Response Headers:**
```
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="contacts.csv"
```

## Dependencies

- **Apache Commons CSV** — new Maven dependency for CSV generation
- Existing Spring Data JPA Specification infrastructure for filtering

## Open Questions

None — all details resolved during design discussion.
