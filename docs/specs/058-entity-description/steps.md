# Implementation Steps: Entity Description Field

## Step 1: Database migration and backend entities

- [x] Create Flyway migration `V13__add_description.sql` with ALTER TABLE statements for both tables
- [x] Add `description` field to `CompanyEntity` with `@Column(columnDefinition = "TEXT")`
- [x] Add `description` field to `ContactEntity` with `@Column(columnDefinition = "TEXT")`
- [x] Add getter/setter for `description` on both entities

**Acceptance criteria:**
- [x] Migration runs successfully against the database
- [x] Application starts without errors
- [x] Project builds successfully

**Related behaviors:** Existing records get null description, Database Migration

---

## Step 2: DTOs and service layer

- [x] Add `String description` to `CompanyDto`, `CompanyCreateDto`, `CompanyUpdateDto`
- [x] Add `String description` to `ContactDto`, `ContactCreateDto`, `ContactUpdateDto`
- [x] Update `CompanyDto.fromEntity()` to map description
- [x] Update `ContactDto.fromEntity()` to map description
- [x] Update `CompanyService.create()` and `update()` to set description on entity
- [x] Update `ContactService.applyFields()` to set description on entity

**Acceptance criteria:**
- [x] Description field flows through create, read, and update operations via API
- [x] Project builds successfully

**Related behaviors:** Description via API, Create company with/without description, Edit company to add/change/remove description, Create contact with/without description, Edit contact to add/remove description

---

## Step 3: Frontend types and i18n

- [x] Add `description: string | null` to `CompanyDto` and `ContactDto` TypeScript interfaces
- [x] Add `description?: string | null` to `CompanyCreateDto` and `ContactCreateDto` interfaces
- [x] Add description translations to `en.ts` (detail + form sections for both entities)
- [x] Add description translations to `de.ts` (detail + form sections for both entities)

**Acceptance criteria:**
- [x] TypeScript compiles without errors
- [x] Frontend builds successfully

**Related behaviors:** (foundation for frontend display steps)

---

## Step 4: Detail views

- [x] Display description in `company-detail.tsx` between fields card and comments, only when non-empty, with `whitespace-pre-line`
- [x] Display description in `contact-detail.tsx` between fields card and comments, only when non-empty, with `whitespace-pre-line`

**Acceptance criteria:**
- [x] Description shown on detail view when present
- [x] Description hidden when null/empty
- [x] Line breaks preserved in display
- [x] Frontend builds successfully

**Related behaviors:** Description preserves line breaks, Create company/contact with description (detail view display), Edit company to add/remove description (detail view update)

---

## Step 5: Edit forms

- [x] Add `Textarea` field for description in `company-form.tsx` after tags, before image upload
- [x] Add `Textarea` field for description in `contact-form.tsx` after tags, before image upload
- [x] Wire description state and include in form data submission

**Acceptance criteria:**
- [x] Description can be entered, edited, and cleared in forms
- [x] Description persisted on save
- [x] Frontend builds successfully

**Related behaviors:** Create company with/without description, Edit company to add/change/remove description, Create contact with/without description, Edit contact to add/remove description

---

## Step 6: Backend tests — DTO, service, and behavioral scenarios

- [x] Add description mapping tests in `CompanyDtoTest` (present and null)
- [x] Add description mapping tests in `ContactDtoTest` (present and null)
- [x] Add description create/update tests in `CompanyServiceTest`
- [x] Add description create/update tests in `ContactServiceTest`
- [x] Add test: Brevo import does not affect existing description (`BrevoSyncServiceTest`)
- [x] Add test: Brevo import creates entity without description (`BrevoSyncServiceTest`)
- [x] Verify CSV export excludes description (existing export tests or new assertion)

**Acceptance criteria:**
- [x] All new tests pass
- [x] All existing tests still pass
- [x] Every backend behavioral scenario from behaviors.md is covered

**Related behaviors:** All backend scenarios from behaviors.md

---

## Step 7: Update project documentation

- [x] Update `specs/INDEX.md` status from `open` to `done`

**Acceptance criteria:**
- [x] INDEX.md reflects completed status

**Related behaviors:** —

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create company with description | Backend + Frontend | Steps 2, 5 |
| Create company without description | Backend + Frontend | Steps 2, 5 |
| Edit company to add description | Backend + Frontend | Steps 2, 5 |
| Edit company to change description | Backend + Frontend | Steps 2, 5 |
| Edit company to remove description | Backend + Frontend | Steps 2, 5 |
| Description preserves line breaks | Frontend | Step 4 |
| Description via API | Backend | Steps 2, 6 |
| Description not in CSV export | Backend | Step 6 |
| Description not in print view | Frontend | Step 4 (not added = excluded) |
| Create contact with description | Backend + Frontend | Steps 2, 5 |
| Create contact without description | Backend + Frontend | Steps 2, 5 |
| Edit contact to add description | Backend + Frontend | Steps 2, 5 |
| Edit contact to remove description | Backend + Frontend | Steps 2, 5 |
| Description preserves line breaks on contact | Frontend | Step 4 |
| Brevo import does not affect description | Backend | Step 6 |
| Brevo import creates entities without description | Backend | Step 6 |
| Existing records get null description | Backend | Step 1 |
