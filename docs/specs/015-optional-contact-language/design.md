# Design: Optional Contact Language

## GitHub Issue

—

## Summary

The language field on contacts is currently a required enum (DE/EN) across all layers. Sometimes the language of a contact is not known at creation time. This change makes the field nullable so that contacts can be created, edited, and displayed without a language. Null is displayed as "Unbekannt" (DE) / "Unknown" (EN) in the UI.

## Goals

- Allow contacts to exist without a language value
- Provide a clear "Unknown" representation in the UI
- Allow filtering for contacts without a language in the list view

## Non-goals

- Adding new language options beyond DE/EN
- Migrating existing contact data (all existing contacts keep their current language)

## Technical Approach

### Backend

**Database migration** (new Flyway migration):

```sql
ALTER TABLE contacts ALTER COLUMN language DROP NOT NULL;
```

No data migration needed — existing rows keep their values.

**Entity** (`ContactEntity.java`):

- Change `@Column(name = "language", nullable = false, length = 5)` to `nullable = true`
- Remove `Objects.requireNonNull` from the setter
- Allow `null` in getter/setter

**DTOs** (`ContactCreateDto.java`, `ContactUpdateDto.java`):

- Remove `@NotNull` annotation from the `language` field
- Change OpenAPI schema from `REQUIRED` to `NOT_REQUIRED`

**ContactDto** (`ContactDto.java`):

- Change OpenAPI schema from `REQUIRED` to `NOT_REQUIRED`
- Field type remains `Language` but is now nullable

**ContactService**:

- No changes needed — `entity.setLanguage(language)` already works with null once the setter allows it

**ContactController** (list filtering):

- The existing filter logic already handles `language == null` in the query parameter (it skips the filter). A new explicit filter value is needed to find contacts where the language column IS NULL.

**Rationale:** Using a schema migration (ALTER COLUMN) is the simplest approach. Since no data needs to change and there are no foreign key constraints on this column, the migration is safe and reversible.

### Frontend

**TypeScript types** (`types.ts`):

- Change `language: "DE" | "EN"` to `language: "DE" | "EN" | null` in `ContactDto`
- Change `language: "DE" | "EN"` to `language: "DE" | "EN" | null` in `ContactCreateDto`

**Contact form** (`contact-form.tsx`):

- Remove the required validation for language (remove asterisk from label, remove error check)
- Add an "Unknown" / "Unbekannt" option to the Select dropdown that maps to `null`
- No default value — user must actively choose (including "Unknown" as a valid choice)

**Contact detail** (`contact-detail.tsx`):

- When `contact.language` is null, display translated "Unbekannt" / "Unknown" instead of the raw value

**Contact list filter** (`contact-list.tsx`):

- Add an "Unbekannt" / "Unknown" option to the language filter dropdown
- This option filters for contacts where language IS NULL

**i18n** (`en.ts`, `de.ts`):

- Add translation key for "Unknown" / "Unbekannt" (for detail display and filter/form option)

### API contract change

The `language` field in request and response bodies changes from required to optional. Existing API clients that always send a language value continue to work without changes. Clients that omit the field or send `null` will create/update contacts with no language.

## Security Considerations

None — no new data is collected, no authorization changes.

## Open Questions

None.
