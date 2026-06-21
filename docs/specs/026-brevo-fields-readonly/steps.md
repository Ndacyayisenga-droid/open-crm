# Implementation Steps: Brevo Fields Readonly

## Step 1: Backend + Frontend + Tests

- [x] Backend: Brevo field protection in ContactService.update() — reject changes to firstName, lastName, email, language
- [x] Backend tests: 7 service tests + 1 controller integration test
- [x] Frontend: Disable 4 fields + hint text for Brevo contacts in edit mode
- [x] Frontend: i18n key managedByBrevo in DE and EN
- [x] Frontend tests: 4 tests (disabled state, hint text, non-Brevo, create mode)

**Acceptance criteria:**
- [x] Backend: 247 tests pass
- [x] Frontend: 159 tests pass, TypeScript clean
