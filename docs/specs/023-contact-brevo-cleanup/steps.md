# Implementation Steps: Contact Brevo Cleanup

## Step 1: Remove syncedToBrevo, change brevoId to String

- [x] Create `V10__contact_brevo_cleanup.sql` — change brevo_id to VARCHAR(50), drop synced_to_brevo
- [x] Update `ContactEntity.java` — brevoId Long→String, remove syncedToBrevo
- [x] Update `ContactRepository.java` — findByBrevoId(String)
- [x] Update `ContactDto.java` — remove syncedToBrevo, add computed `brevo` field
- [x] Update `ContactService.java` — remove syncedToBrevo comment
- [x] Update `BrevoSyncService.java` — String.valueOf(brevoContact.id()), remove setSyncedToBrevo
- [x] Update `types.ts` — syncedToBrevo→brevo
- [x] Update `contact-detail.tsx` — contact.brevo, S.detail.brevo
- [x] Update i18n keys — syncedToBrevo→brevo in de.ts and en.ts
- [x] Update all backend tests (ContactDtoTest, ContactControllerTest, BrevoSyncServiceTest, HealthControllerTest)
- [x] Update all frontend tests (contact-detail, contact-form, contact-list)

**Acceptance criteria:**
- [x] Backend: 229 tests pass, 0 failures
- [x] Frontend: 150 tests pass, TypeScript clean
