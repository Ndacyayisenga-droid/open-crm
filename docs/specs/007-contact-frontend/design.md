# Design: Contact Frontend

## GitHub Issue

_To be created._

## Summary

Build the contact (person) management UI for Open CRM, analogous to the existing company frontend. The backend is fully implemented — this spec covers only the frontend. Users will be able to list, view, create, edit, and delete contacts through a responsive web interface. Contacts can optionally be linked to a company via a dropdown select.

## Goals

- Contact list page with filtering, sorting, and pagination
- Contact detail page showing all fields with edit and delete actions
- Create and edit forms with validation
- Company association via a simple `<select>` dropdown
- Sidebar navigation entry for contacts
- All UI strings in German and English (i18n)

## Non-goals

- **Autocomplete/search in company dropdown** — will be a separate spec when the number of companies grows
- **Full comment implementation** — only a placeholder (disabled button), same as current company detail. Depends on spec 005.
- **Brevo sync UI** — `syncedToBrevo` and `doubleOptIn` are displayed as read-only checkboxes in the detail view, but there is no UI to trigger or manage sync
- **Authentication / authorization** — no security layer exists yet

## Technical Approach

Follow the established patterns from the company frontend exactly:

- **Server Components** for page-level data fetching (`export const dynamic = "force-dynamic"`)
- **Client Components** (`"use client"`) for interactive views (list, detail, form)
- **API client** using `fetch()` directly with `baseUrl()` helper and `{ cache: "no-store" }`
- **Types** as `readonly` TypeScript interfaces
- **Strings** in the centralized `STRINGS` constant object
- **shadcn/ui** components for all UI elements (Button, Input, Label, Card, Table, Select, Skeleton, AlertDialog)
- **Reuse** existing `DeleteConfirmDialog` component

### Key Differences to Company Frontend

| Aspect | Company | Contact |
|--------|---------|---------|
| Delete behavior | Soft-delete with restore | Hard-delete, permanent |
| Archive toggle | Show/hide archived companies | Not applicable |
| Company association | N/A | Optional `<select>` dropdown |
| Additional fields | Address fields | Gender, language, LinkedIn, phone, position |
| Brevo fields | None | `syncedToBrevo`, `doubleOptIn` (read-only in detail) |
| List columns | Name, Website | First name, Last name, Company |
| Delete warning | Standard confirmation | Warns about permanent deletion + comment loss |

## Pages & Routes

| Route | Server Component | Client Component | Purpose |
|-------|-----------------|------------------|---------|
| `/contacts` | `app/contacts/page.tsx` | `ContactList` | List contacts with filters, sort, pagination |
| `/contacts/new` | `app/contacts/new/page.tsx` | `ContactForm` (create) | Create new contact |
| `/contacts/[id]` | `app/contacts/[id]/page.tsx` | `ContactDetail` | View contact details, actions |
| `/contacts/[id]/edit` | `app/contacts/[id]/edit/page.tsx` | `ContactForm` (edit) | Edit existing contact |

## Components

### `contact-list.tsx`

Client component with state for:
- **Filters:** firstName, lastName, email (text inputs), companyId (select with active companies), language (select DE/EN/all)
- **Sort:** lastName ASC/DESC, firstName ASC/DESC, createdAt ASC/DESC
- **Pagination:** page number, Previous/Next buttons, page indicator
- **Loading:** skeleton state during fetch

Table columns: First name, Last name, Company name. Row click navigates to detail. Delete button per row with confirmation dialog.

### `contact-detail.tsx`

Client component displaying all contact fields in a grid layout:
- First name, Last name, Email, Position, Gender, Phone, LinkedIn URL, Language
- Company name — if the associated company is soft-deleted, show a badge/label indicating "Archived"
- Brevo fields: `syncedToBrevo` and `doubleOptIn` as disabled checkboxes with labels
- Action buttons: Edit, Delete
- Comments section: placeholder with disabled "Add comment" button

### `contact-form.tsx`

Client component with dual mode (create/edit):
- **Required fields:** firstName, lastName, language
- **Optional fields:** email, position, gender, linkedInUrl, phoneNumber, companyId
- **Company select:** Loads all active (non-deleted) companies on mount via `getCompaniesForSelect()`. Shows "No company" as default/empty option.
- **Gender select:** Options MALE, FEMALE, DIVERSE, plus empty "Not specified" option
- **Language select:** DE, EN (required, no empty option)
- **Validation:** Client-side check for required fields (firstName, lastName not blank; language selected). Server errors displayed below form.
- **Submit:** Navigates to detail page on success
- **Cancel:** Navigates back (list if create, detail if edit)

## API Client Extensions

New functions in `frontend/src/lib/api.ts`:

```typescript
// Contact CRUD
getContacts(params: ContactListParams): Promise<Page<ContactDto>>
getContact(id: string): Promise<ContactDto>
createContact(data: ContactCreateDto): Promise<ContactDto>
updateContact(id: string, data: ContactCreateDto): Promise<ContactDto>
deleteContact(id: string): Promise<void>

// Company select helper (reusable)
getCompaniesForSelect(): Promise<CompanyDto[]>
// Fetches all active companies (includeDeleted=false, size=1000, sort=name,asc)
// Returns content array only
```

New type `ContactListParams`:

```typescript
interface ContactListParams {
  page?: number;
  size?: number;
  sort?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  companyId?: string;
  language?: string;
}
```

## Type Definitions

New interfaces in `frontend/src/lib/types.ts`:

```typescript
interface ContactDto {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string | null;
  readonly position: string | null;
  readonly gender: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly linkedInUrl: string | null;
  readonly phoneNumber: string | null;
  readonly companyId: string | null;
  readonly companyName: string | null;
  readonly syncedToBrevo: boolean;
  readonly doubleOptIn: boolean;
  readonly language: "DE" | "EN";
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface ContactCreateDto {
  readonly firstName: string;
  readonly lastName: string;
  readonly email?: string | null;
  readonly position?: string | null;
  readonly gender?: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly linkedInUrl?: string | null;
  readonly phoneNumber?: string | null;
  readonly companyId?: string | null;
  readonly language: "DE" | "EN";
}
```

## i18n Strings

Add `STRINGS.contacts` block in `frontend/src/lib/constants.ts` with German and English translations for:
- List view: title, newContact, empty state, column headers, filter labels, sort options, pagination
- Detail view: title, field labels, edit/delete buttons, archived badge, Brevo field labels, comments placeholder
- Form: create/edit titles, field labels, placeholders, validation messages, save/cancel buttons
- Delete dialog: title, permanent deletion warning including comment loss, confirm/cancel buttons

## Sidebar

Add a "Kontakte" / "Contacts" entry to the sidebar navigation in `frontend/src/components/sidebar.tsx`, below the existing "Firmen" / "Companies" entry. Use the `Users` icon from lucide-react.

## Files to Create

- `frontend/src/app/contacts/page.tsx`
- `frontend/src/app/contacts/new/page.tsx`
- `frontend/src/app/contacts/[id]/page.tsx`
- `frontend/src/app/contacts/[id]/edit/page.tsx`
- `frontend/src/components/contact-list.tsx`
- `frontend/src/components/contact-detail.tsx`
- `frontend/src/components/contact-form.tsx`

## Files to Modify

- `frontend/src/lib/api.ts` — add contact API functions and `getCompaniesForSelect()`
- `frontend/src/lib/types.ts` — add `ContactDto`, `ContactCreateDto`
- `frontend/src/lib/constants.ts` — add `STRINGS.contacts` block (DE/EN)
- `frontend/src/components/sidebar.tsx` — add contacts nav item

---

## Appendix: Implementation Deviations

### i18n approach changed

This spec was written before spec 006 (Frontend i18n) was implemented. At the time of writing, UI strings were managed in a centralized `STRINGS` object in `frontend/src/lib/constants.ts`.

Spec 006 replaced that approach with a proper i18n system using `frontend/src/lib/i18n/de.ts` and `frontend/src/lib/i18n/en.ts`, accessed via a `useTranslations()` hook from a React Context (`LanguageProvider`).

The contact frontend was implemented using the new i18n system. References to `constants.ts` and `STRINGS.contacts` in the sections above should be read as `i18n/de.ts` / `i18n/en.ts` with a `contacts` translation block in each file.
