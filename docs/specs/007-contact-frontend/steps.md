# Implementation Steps: Contact Frontend

## Step 1: Type Definitions & API Client

- [x] Add `ContactDto` interface to `frontend/src/lib/types.ts` with all fields (id, firstName, lastName, email, position, gender, linkedInUrl, phoneNumber, companyId, companyName, syncedToBrevo, doubleOptIn, language, createdAt, updatedAt) — all `readonly`
- [x] Add `ContactCreateDto` interface to `frontend/src/lib/types.ts` (firstName, lastName, language required; email, position, gender, linkedInUrl, phoneNumber, companyId optional)
- [x] Add `ContactListParams` interface to `frontend/src/lib/api.ts` (page, size, sort, firstName, lastName, email, companyId, language)
- [x] Add `getContacts(params)` function to `frontend/src/lib/api.ts` — GET `/api/contacts` with query params
- [x] Add `getContact(id)` function — GET `/api/contacts/{id}`
- [x] Add `createContact(data)` function — POST `/api/contacts`
- [x] Add `updateContact(id, data)` function — PUT `/api/contacts/{id}`
- [x] Add `deleteContact(id)` function — DELETE `/api/contacts/{id}`
- [x] Add `getCompaniesForSelect()` function — GET `/api/companies?includeDeleted=false&size=1000&sort=name,asc`, returns `content` array only

**Acceptance criteria:**
- [x] Project builds successfully (`pnpm build` in frontend)
- [x] TypeScript compiles with no errors
- [x] API functions follow existing patterns (fetch with `cache: "no-store"`, error handling)

**Related behaviors:** None directly — this step provides the foundation for all subsequent steps.

---

## Step 2: i18n Strings

- [x] Add `contacts` section to `frontend/src/lib/i18n/de.ts` with German translations for: title, newContact, empty, notFound, columns (firstName, lastName, company, actions), detail (title, edit, delete, fields, archivedBadge, brevo labels, commentsPlaceholder), form (createTitle, editTitle, field labels, placeholders, validation messages, save, cancel, noCompany, notSpecified), deleteDialog (title, description with permanent warning and comment loss, confirm, cancel), filter (firstName, lastName, email, company, language), pagination (previous, next, page), sort (label, options)
- [x] Add matching `contacts` section to `frontend/src/lib/i18n/en.ts` with English translations
- [x] Add `nav.contacts` entry to both translation files (DE: "Kontakte", EN: "Contacts")

**Acceptance criteria:**
- [x] Project builds successfully
- [x] Both language files have identical key structure for the `contacts` section
- [x] `nav.contacts` key exists in both files

**Related behaviors:** Displays German strings by default, Displays English strings when selected

---

## Step 3: Sidebar Navigation

- [x] Import `Users` icon from lucide-react in `frontend/src/components/sidebar.tsx`
- [x] Add contacts navigation item `{ label: t.nav.contacts, href: "/contacts", icon: <Users /> }` below the companies entry

**Acceptance criteria:**
- [x] Project builds successfully
- [x] "Kontakte" / "Contacts" entry appears in sidebar below "Firmen" / "Companies"
- [x] Active highlighting works on `/contacts` routes

**Related behaviors:** Shows contacts entry in sidebar, Highlights active contacts page

---

## Step 4: Contact List Page & Component

- [x] Create `frontend/src/app/contacts/page.tsx` — server component with `force-dynamic`, renders `ContactList`
- [x] Create `frontend/src/components/contact-list.tsx` — client component following CompanyList patterns:
  - State for data, loading, page, sort, filters (firstName, lastName, email, companyId, language)
  - Filter inputs: text inputs for firstName/lastName/email, Select for company (loaded via `getCompaniesForSelect()`), Select for language (DE/EN/all)
  - Sort dropdown: lastName ASC/DESC, firstName ASC/DESC, createdAt ASC/DESC
  - Pagination: Previous/Next buttons, page indicator
  - Table columns: First name, Last name, Company name
  - Row click navigates to `/contacts/{id}`
  - Delete button per row with `DeleteConfirmDialog` (permanent deletion warning + comment loss warning)
  - Empty state message with "New contact" button
  - Loading skeleton during fetch
  - All strings from `useTranslations().contacts`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] `/contacts` page renders contact list
- [x] Filters, sorting, and pagination work
- [x] Delete from list works with confirmation dialog
- [x] Empty state shown when no contacts match

**Related behaviors:** Displays contacts on page load, Shows empty state when no contacts exist, Shows empty state for filtered results with no matches, Filters by first name, Filters by last name, Filters by email, Filters by company, Filters by language, Sorts contacts, Paginates results, Shows company name in list, Shows empty company column for unassociated contacts, Navigates to detail on row click, Shows permanent deletion warning, Warns about comment loss, Deletes contact on confirmation, Cancels deletion

---

## Step 5: Contact Detail Page & Component

- [x] Create `frontend/src/app/contacts/[id]/page.tsx` — server component with `force-dynamic`, fetches contact, shows `notFound()` on error
- [x] Create `frontend/src/components/contact-detail.tsx` — client component following CompanyDetail patterns:
  - Display all fields in grid layout: firstName, lastName, email, position, gender, phone, linkedInUrl, language
  - Company name — show "Archived" badge if company is soft-deleted (check via companyName presence + additional indicator)
  - Brevo fields: syncedToBrevo and doubleOptIn as disabled checkboxes with labels
  - Optional fields show dash "—" when null
  - Edit button navigates to `/contacts/{id}/edit`
  - Delete button with `DeleteConfirmDialog` (permanent warning + comment loss)
  - After delete, navigate to `/contacts`
  - Comments section: placeholder heading with disabled "Add comment" button
  - All strings from `useTranslations().contacts`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] `/contacts/{id}` renders contact detail
- [x] All fields displayed correctly, optional fields show dash
- [x] Brevo checkboxes are read-only
- [x] Edit and delete buttons work
- [x] 404 page shown for non-existent contact
- [x] Comments placeholder shown

**Related behaviors:** Displays all contact fields, Displays Brevo fields as read-only checkboxes, Shows archived badge for soft-deleted company, Handles missing optional fields gracefully, Shows comments placeholder, Navigates to edit page, Shows 404 for non-existent contact (detail), Shows permanent deletion warning, Warns about comment loss, Deletes contact on confirmation, Cancels deletion

---

## Step 6: Contact Form & Route Pages (Create + Edit)

- [x] Create `frontend/src/app/contacts/new/page.tsx` — server component rendering `ContactForm` in create mode
- [x] Create `frontend/src/app/contacts/[id]/edit/page.tsx` — server component fetching contact, rendering `ContactForm` in edit mode, `notFound()` on error
- [x] Create `frontend/src/components/contact-form.tsx` — client component following CompanyForm patterns:
  - Dual mode: create (no `contact` prop) and edit (`contact` prop provided)
  - Required fields: firstName (text input), lastName (text input), language (Select: DE/EN, no empty option)
  - Optional fields: email (text input), position (text input), gender (Select: MALE/FEMALE/DIVERSE + "Not specified" empty option), linkedInUrl (text input), phoneNumber (text input), companyId (Select loaded via `getCompaniesForSelect()` + "No company" empty option)
  - Client-side validation: firstName and lastName not blank, language selected
  - Validation error messages shown inline below fields
  - Server-side error displayed below form
  - Submit: POST (create) or PUT (edit), navigate to detail page on success
  - Cancel: navigate to `/contacts` (create) or `/contacts/{id}` (edit)
  - Edit mode pre-fills all fields with existing contact data
  - All strings from `useTranslations().contacts`

**Acceptance criteria:**
- [x] Project builds successfully
- [x] `/contacts/new` renders create form
- [x] `/contacts/{id}/edit` renders edit form with pre-filled data
- [x] Required field validation works (firstName, lastName, language)
- [x] Company dropdown shows only active companies + "No company" option
- [x] Gender dropdown has MALE/FEMALE/DIVERSE + "Not specified"
- [x] Submit creates/updates contact and navigates to detail
- [x] Cancel navigates correctly (list for create, detail for edit)
- [x] Server errors displayed below form
- [x] 404 shown for non-existent contact on edit page

**Related behaviors:** Creates a contact with all fields, Creates a contact with only required fields, Validates required fields, Validates language is selected, Company dropdown shows only active companies, Company dropdown allows empty selection, Shows server-side validation errors, Cancel navigates to contact list, Pre-fills form with existing data, Updates contact successfully, Validates required fields on edit, Cancel navigates to detail page, Shows 404 for non-existent contact (edit)

---

## Step 7: Frontend Tests — Contact List

- [x] Create `frontend/src/components/__tests__/contact-list.test.tsx` following existing test patterns (renderWithProviders, vi.mock for api and next/navigation)
- [x] Test: renders contact table with firstName, lastName, company columns
- [x] Test: shows empty state when no contacts exist
- [x] Test: shows empty state when filters match nothing
- [x] Test: filters by firstName (verifies API called with firstName param)
- [x] Test: filters by lastName
- [x] Test: filters by email
- [x] Test: filters by company (select dropdown)
- [x] Test: filters by language (select dropdown)
- [x] Test: sorts contacts (select sort option, verify API param)
- [x] Test: paginates (next/previous buttons, page indicator)
- [x] Test: shows company name in table, empty for unassociated
- [x] Test: navigates to detail on row click
- [x] Test: delete button opens confirmation dialog with permanent warning and comment loss text
- [x] Test: confirming delete calls deleteContact API and refreshes list
- [x] Test: cancelling delete closes dialog without API call

**Acceptance criteria:**
- [x] All tests pass (`pnpm test`)
- [x] Project builds successfully

**Related behaviors:** Displays contacts on page load, Shows empty state when no contacts exist, Shows empty state for filtered results with no matches, Filters by first name, Filters by last name, Filters by email, Filters by company, Filters by language, Sorts contacts, Paginates results, Shows company name in list, Shows empty company column for unassociated contacts, Navigates to detail on row click, Shows permanent deletion warning, Warns about comment loss, Deletes contact on confirmation, Cancels deletion

---

## Step 8: Frontend Tests — Contact Detail

- [x] Create `frontend/src/components/__tests__/contact-detail.test.tsx`
- [x] Test: displays all contact fields (firstName, lastName, email, position, gender, phone, linkedInUrl, language, company)
- [x] Test: displays Brevo fields as disabled checkboxes (syncedToBrevo checked, doubleOptIn unchecked)
- [x] Test: shows archived badge when company is soft-deleted
- [x] Test: handles missing optional fields (shows dash)
- [x] Test: shows comments placeholder with disabled button
- [x] Test: edit button navigates to `/contacts/{id}/edit`
- [x] Test: delete button opens confirmation dialog with permanent warning + comment loss
- [x] Test: confirming delete calls API and navigates to `/contacts`
- [x] Test: cancelling delete closes dialog

**Acceptance criteria:**
- [x] All tests pass
- [x] Project builds successfully

**Related behaviors:** Displays all contact fields, Displays Brevo fields as read-only checkboxes, Shows archived badge for soft-deleted company, Handles missing optional fields gracefully, Shows comments placeholder, Navigates to edit page, Shows permanent deletion warning, Warns about comment loss, Deletes contact on confirmation, Cancels deletion

---

## Step 9: Frontend Tests — Contact Form (Create + Edit)

- [x] Create `frontend/src/components/__tests__/contact-form.test.tsx`
- [x] Test: create mode — submits with all fields, calls createContact, navigates to detail
- [x] Test: create mode — submits with only required fields (firstName, lastName, language)
- [x] Test: create mode — validates firstName required (shows error, no API call)
- [x] Test: create mode — validates lastName required
- [x] Test: create mode — validates language required
- [x] Test: create mode — company dropdown loads only active companies
- [x] Test: create mode — company dropdown has "No company" option selected by default
- [x] Test: create mode — gender dropdown has "Not specified" empty option
- [x] Test: create mode — displays server-side error on API failure
- [x] Test: create mode — cancel navigates to `/contacts`
- [x] Test: edit mode — pre-fills all fields with existing contact data
- [x] Test: edit mode — correct company selected in dropdown
- [x] Test: edit mode — submits changes, calls updateContact, navigates to detail
- [x] Test: edit mode — validates required fields on edit
- [x] Test: edit mode — cancel navigates to `/contacts/{id}`

**Acceptance criteria:**
- [x] All tests pass
- [x] Project builds successfully

**Related behaviors:** Creates a contact with all fields, Creates a contact with only required fields, Validates required fields, Validates language is selected, Company dropdown shows only active companies, Company dropdown allows empty selection, Shows server-side validation errors, Cancel navigates to contact list, Pre-fills form with existing data, Updates contact successfully, Validates required fields on edit, Cancel navigates to detail page

---

## Step 10: Frontend Tests — Sidebar & i18n

- [x] Add test for contacts entry in sidebar test file (or create new test)
- [x] Test: sidebar shows "Kontakte" in German
- [x] Test: sidebar shows "Contacts" in English
- [x] Test: contacts nav item highlighted when on `/contacts` route
- [x] Test: contact list renders German labels by default
- [x] Test: contact list renders English labels when language is English

**Acceptance criteria:**
- [x] All tests pass
- [x] Project builds successfully

**Related behaviors:** Shows contacts entry in sidebar, Highlights active contacts page, Displays German strings by default, Displays English strings when selected

---

## Step 11: Update Project Documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — update Contact Management to mention the frontend (list, detail, create, edit, delete with filtering/sorting/pagination, company association dropdown)
- [x] Update `.claude/conventions/project-specific/project-structure.md` — add `contacts/` routes under `src/app/`, add contact components under `src/components/`
- [x] Update `specs/INDEX.md` — set spec 007 status to `done`
- [x] Update `README.md` if user-facing behavior or navigation changed

**Acceptance criteria:**
- [x] Documentation files reflect the current state of the project
- [x] INDEX.md shows spec 007 as done
- [x] All new files and routes are documented in project-structure.md

**Related behaviors:** None — documentation step.

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Displays contacts on page load | Frontend | Step 4, Step 7 |
| Shows empty state when no contacts exist | Frontend | Step 4, Step 7 |
| Shows empty state for filtered results with no matches | Frontend | Step 4, Step 7 |
| Filters by first name | Frontend | Step 4, Step 7 |
| Filters by last name | Frontend | Step 4, Step 7 |
| Filters by email | Frontend | Step 4, Step 7 |
| Filters by company | Frontend | Step 4, Step 7 |
| Filters by language | Frontend | Step 4, Step 7 |
| Sorts contacts | Frontend | Step 4, Step 7 |
| Paginates results | Frontend | Step 4, Step 7 |
| Shows company name in list | Frontend | Step 4, Step 7 |
| Shows empty company column for unassociated contacts | Frontend | Step 4, Step 7 |
| Navigates to detail on row click | Frontend | Step 4, Step 7 |
| Displays all contact fields | Frontend | Step 5, Step 8 |
| Displays Brevo fields as read-only checkboxes | Frontend | Step 5, Step 8 |
| Shows archived badge for soft-deleted company | Frontend | Step 5, Step 8 |
| Handles missing optional fields gracefully | Frontend | Step 5, Step 8 |
| Shows comments placeholder | Frontend | Step 5, Step 8 |
| Navigates to edit page | Frontend | Step 5, Step 8 |
| Shows 404 for non-existent contact (detail) | Both | Step 5, Step 8 |
| Creates a contact with all fields | Both | Step 6, Step 9 |
| Creates a contact with only required fields | Both | Step 6, Step 9 |
| Validates required fields | Frontend | Step 6, Step 9 |
| Validates language is selected | Frontend | Step 6, Step 9 |
| Company dropdown shows only active companies | Frontend | Step 6, Step 9 |
| Company dropdown allows empty selection | Frontend | Step 6, Step 9 |
| Shows server-side validation errors | Both | Step 6, Step 9 |
| Cancel navigates to contact list | Frontend | Step 6, Step 9 |
| Pre-fills form with existing data | Frontend | Step 6, Step 9 |
| Updates contact successfully | Both | Step 6, Step 9 |
| Validates required fields on edit | Frontend | Step 6, Step 9 |
| Cancel navigates to detail page | Frontend | Step 6, Step 9 |
| Shows 404 for non-existent contact (edit) | Both | Step 6, Step 9 |
| Shows permanent deletion warning | Frontend | Step 4/5, Step 7/8 |
| Warns about comment loss | Frontend | Step 4/5, Step 7/8 |
| Deletes contact on confirmation | Both | Step 4/5, Step 7/8 |
| Cancels deletion | Frontend | Step 4/5, Step 7/8 |
| Shows contacts entry in sidebar | Frontend | Step 3, Step 10 |
| Highlights active contacts page | Frontend | Step 3, Step 10 |
| Displays German strings by default | Frontend | Step 2, Step 10 |
| Displays English strings when selected | Frontend | Step 2, Step 10 |
