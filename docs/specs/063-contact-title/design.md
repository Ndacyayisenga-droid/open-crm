# Design: Contact Title Field

## GitHub Issue

—

## Summary

Some contacts have academic or professional titles (e.g., "Dr.", "Prof.", "Prof. Dr.") that should be captured and displayed alongside their name. An optional free-text `title` field is added to the Contact entity. The title is prepended to the name wherever it is displayed: detail view header, list table, print view. It is also included in CSV export as a separate column and searchable via the unified search.

## Goals

- Add an optional title field to contacts
- Display the title before the name in all views (detail, list, print)
- Include the title in CSV export
- Make the title searchable via unified search
- Editable in the create/edit form, positioned before the first name

## Non-goals

- Enum/dropdown for titles — free text is sufficient
- Brevo sync — title is a purely local CRM field
- Title for companies — only contacts

## Technical Approach

### Backend

**Entity:**
- Add `title` field to `ContactEntity`: nullable `String`, `@Column(length = 255)`

**Migration (V15):**
```sql
ALTER TABLE contacts ADD COLUMN title VARCHAR(255);
```

**DTOs:**
- Add `String title` to `ContactDto`, `ContactCreateDto`, `ContactUpdateDto`
- No validation constraints — optional, max 255 chars via `@Size(max = 255)`

**CSV export:**
- Add `TITLE` to `ContactExportColumn` enum — exports the title as a separate column

**Search (Spec 031 extension):**
- Add `title` to the unified search specification in `ContactService` — the search already covers `firstName`, `lastName`, `email`, and `companyName`; title is added as another `cb.like` in the OR clause

**Brevo sync:**
- No changes — title is not a Brevo-managed field and will not be touched during import

### Frontend

**Name display pattern:**
- Everywhere the name is currently shown as `` `${contact.firstName} ${contact.lastName}` ``, it becomes `` `${contact.title ? contact.title + ' ' : ''}${contact.firstName} ${contact.lastName}` ``
- Affected locations:
  - `contact-detail.tsx` — header `fullName` variable
  - `contact-list.tsx` — table Name column, delete dialog, photo alt text
  - `contacts/print/page.tsx` — print table Name column, photo alt text

**Detail view:**
- Title is displayed as part of the name in the header (e.g., "Dr. Max Müller")
- No separate field in the detail card grid — the title is only part of the name display

**Edit form** (`contact-form.tsx`):
- Add a `title` input field before the `firstName` field, in the same row
- Layout: 3-column grid — title (narrow), firstName, lastName
- Placeholder: "Titel" / "Title" (i18n)

**TypeScript types:**
- Add `title: string | null` to the Contact type definition

### i18n

| Key | EN | DE |
|-----|----|----|
| `contacts.fields.title` | "Title" | "Titel" |

## Key Files

| File | Change |
|------|--------|
| `backend/.../contact/ContactEntity.java` | Add `title` field |
| `backend/.../contact/ContactDto.java` | Add `title` |
| `backend/.../contact/ContactCreateDto.java` | Add `title` with `@Size(max = 255)` |
| `backend/.../contact/ContactUpdateDto.java` | Add `title` with `@Size(max = 255)` |
| `backend/.../contact/ContactExportColumn.java` | Add `TITLE` enum value |
| `backend/.../contact/ContactService.java` | Add `title` to unified search spec |
| `backend/.../db/migration/V15__add_contact_title.sql` | Migration |
| `frontend/src/components/contact-detail.tsx` | Prepend title to fullName |
| `frontend/src/components/contact-list.tsx` | Prepend title to name column |
| `frontend/src/components/contact-form.tsx` | Add title input before firstName |
| `frontend/src/app/contacts/print/page.tsx` | Prepend title to name column |
| `frontend/src/lib/types.ts` | Add `title` to Contact type |
| `frontend/src/lib/i18n/en.ts` | Add title label |
| `frontend/src/lib/i18n/de.ts` | Add title label |

## Security Considerations

- Free text stored as VARCHAR(255) — no HTML rendering, no injection risk
- Standard input sanitization applies
