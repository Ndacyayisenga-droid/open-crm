# Implementation Steps: Contact–Company Cross-Navigation

## Step 1: Backend — Add companyDeleted to ContactDto

- [x] Add `companyDeleted` boolean field to `ContactDto` record
- [x] Resolve from `company.isDeleted()` in `fromEntity()`, default `false` when no company
- [x] Add `@Schema` annotation
- [x] Backend compiles and all 69 tests pass

## Step 2: Frontend type and i18n updates

- [x] Add `companyDeleted: boolean` to `ContactDto` in `types.ts`
- [x] Add `contacts.detail.showCompany` to DE ("zur Firma") and EN ("show company")
- [x] Add `companies.detail.showEmployees` to DE ("Alle Mitarbeiter") and EN ("show employees")

## Step 3: Contact detail — company link

- [x] Remove `companyDeleted` prop from `ContactDetail` — read from `contact.companyDeleted`
- [x] Active company: render as `<Link>` to `/companies/{companyId}` with "zur Firma" label
- [x] Archived company: static text + archived badge (no link)
- [x] No company: "—"
- [x] Simplify `contacts/[id]/page.tsx` — remove extra `getCompany()` call

## Step 4: Company detail — show employees link

- [x] Add "Alle Mitarbeiter" / "show employees" button with Users icon in header
- [x] Active company: links to `/contacts?companyId={id}`
- [x] Archived company: button rendered as disabled

## Step 5: Contact list — URL parameter filtering

- [x] Use `useSearchParams()` to read `companyId` from URL on mount
- [x] Initialize `companyIdFilter` state from URL parameter
- [x] Wrap ContactList page in `<Suspense>` for Next.js App Router compatibility

## Step 6: Update tests

- [x] Add `companyDeleted` to all contact test fixtures
- [x] Update archived badge test to use `contact.companyDeleted` instead of prop
- [x] Add `useSearchParams` mock to contact list tests
- [x] All 117 frontend tests pass

**Acceptance criteria:**
- [x] Backend: 69 tests pass, BUILD SUCCESS
- [x] Frontend: 117 tests pass, TypeScript compiles
- [x] Contact detail links to company when active
- [x] Company detail links to filtered contact list
- [x] Contact list reads companyId from URL
