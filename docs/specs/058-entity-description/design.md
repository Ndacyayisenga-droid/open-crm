# Design: Entity Description Field

## GitHub Issue

—

## Summary

Companies and contacts currently have no way to store a persistent, editable profile note. Comments exist but serve as chronological log entries ("talked to him about X"). A description field provides a stable, freely editable text block that summarizes what a company or contact is about — a "profile note" rather than a diary entry.

Both `CompanyEntity` and `ContactEntity` will receive an optional free-text `description` field stored as PostgreSQL `TEXT`. The field is displayed in detail views (between the existing fields and the comments section) and editable via the existing edit forms.

## Goals

- Allow users to store a persistent free-text description on companies and contacts
- Display the description prominently in detail views
- Edit the description through existing edit forms

## Non-goals

- Rich text / Markdown formatting — plain text only
- Searchability — the description is not included in list search filters
- CSV export — description is excluded from exports
- Print view — description is excluded from print output
- Brevo sync — description is a purely local CRM field, not synced to or from Brevo

## Technical Approach

This is a straightforward field addition across the full stack. No new endpoints, components, or architectural patterns are introduced.

### Backend

**Entities:**
- Add `description` field to `CompanyEntity` and `ContactEntity`
- Type: nullable `String` with `@Column(columnDefinition = "TEXT")` — no length limit at the database level
- No validation constraints (optional, unbounded text)

**DTOs:**
- Add `String description` to all six DTO records:
  - `CompanyDto`, `CompanyCreateDto`, `CompanyUpdateDto`
  - `ContactDto`, `ContactCreateDto`, `ContactUpdateDto`
- No `@Size` or `@NotBlank` annotation — the field is optional and unbounded

**Service / Controller:**
- No changes to service or controller logic needed — existing CRUD mapping handles the new field automatically through the existing entity-to-DTO conversion

**Brevo sync:**
- No changes — `description` is not a Brevo-managed field and will not be touched during import or re-import

**Database migration:**
- New Flyway migration `V13__add_description.sql`:
  ```sql
  ALTER TABLE companies ADD COLUMN description TEXT;
  ALTER TABLE contacts ADD COLUMN description TEXT;
  ```
- Existing rows default to `NULL`

### Frontend

**Detail views** (`company-detail.tsx`, `contact-detail.tsx`):
- Display the description between the existing fields `Card` and the `Separator` / comments section
- Show only when the description is non-empty (no empty placeholder)
- Render as a simple text block (preserving line breaks with `whitespace-pre-line`)

**Edit forms** (`company-form.tsx`, `contact-form.tsx`):
- Add a `Textarea` field labeled "Beschreibung" / "Description" (i18n)
- Position: after the tags multi-select, before the image upload section
- No character limit indicator, no required marker

**TypeScript types:**
- Add `description: string | null` to the Company and Contact type definitions

## Data Model

No new entities or relationships. Two columns added to existing tables:

| Table | Column | Type | Nullable | Default |
|-------|--------|------|----------|---------|
| `companies` | `description` | `TEXT` | yes | `NULL` |
| `contacts` | `description` | `TEXT` | yes | `NULL` |

## Security Considerations

- The description is plain text — no HTML rendering, no injection risk
- Standard input sanitization applies (framework-level)
- No new authorization rules — description follows the same access control as other entity fields
