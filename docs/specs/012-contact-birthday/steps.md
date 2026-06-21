# Implementation Steps: Contact Birthday Field

## Step 1: Database migration
- [x] Create `V4__add_contact_birthday.sql` with `ALTER TABLE contacts ADD COLUMN birthday DATE`

## Step 2: Backend entity + DTOs
- [x] Add `LocalDate birthday` field to `ContactEntity` with getter/setter
- [x] Add `LocalDate birthday` to `ContactCreateDto` and `ContactUpdateDto`
- [x] Add `LocalDate birthday` to `ContactDto` and update `fromEntity()`

## Step 3: Backend service
- [x] Add `birthday` parameter to `applyFields()` in `ContactService`
- [x] Pass `request.birthday()` in `create()` and `update()`

## Step 4: Frontend types + i18n
- [x] Add `birthday: string | null` to `ContactDto` in `types.ts`
- [x] Add `birthday?: string | null` to `ContactCreateDto`
- [x] Add `contacts.detail.birthday` and `contacts.form.birthday` to DE + EN

## Step 5: Frontend contact detail
- [x] Add locale-aware `formatBirthday()` function (DE: DD.MM.YYYY, EN: MM/DD/YYYY)
- [x] Display birthday as `DetailField` in contact detail view

## Step 6: Frontend contact form
- [x] Add birthday state and `<input type="date">` field
- [x] Include birthday in submit data (empty string → null)

## Step 7: Update tests
- [x] Add `birthday` to all contact test fixtures
- [x] All 117 frontend tests pass
- [x] All 69 backend tests pass
