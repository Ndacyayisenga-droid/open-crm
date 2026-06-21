# Implementation Steps: Remove Double Opt-In

## Step 1: Remove doubleOptIn from entire stack

- [x] Create `V9__remove_double_opt_in.sql` — drop column
- [x] Remove field, getter, setter from `ContactEntity.java`
- [x] Remove record component from `ContactDto.java` and `fromEntity()` mapping
- [x] Update `ContactService.java` — remove doubleOptIn mention from Javadoc
- [x] Remove `DOUBLE_OPT-IN` attribute mapping from `BrevoSyncService.java`
- [x] Remove `doubleOptIn` from `types.ts`
- [x] Remove checkbox display from `contact-detail.tsx`
- [x] Remove `doubleOptIn` i18n keys from `de.ts` and `en.ts`
- [x] Update all backend tests (ContactDtoTest, ContactControllerTest, BrevoSyncServiceTest, HealthControllerTest)
- [x] Update all frontend tests (contact-detail, contact-form, contact-list test files)

**Acceptance criteria:**
- [x] Backend: 227 tests pass, 0 failures
- [x] Frontend: 150 tests pass, TypeScript clean
- [x] No traces of doubleOptIn in source or test code
- [x] syncedToBrevo field unaffected
