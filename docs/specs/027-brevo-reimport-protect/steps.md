# Implementation Steps: Brevo Reimport Protection

## Step 1: Guard user-editable fields + Tests

- [x] BrevoSyncService: wrap position, phoneNumber, linkedInUrl, company assignment in `if (created)` guard
- [x] 7 new tests: position/phone/linkedIn/company NOT overwritten, Brevo-managed fields ARE overwritten, all fields on first import, email-matched contact treated as existing

**Acceptance criteria:**
- [x] Backend: 254 tests pass
