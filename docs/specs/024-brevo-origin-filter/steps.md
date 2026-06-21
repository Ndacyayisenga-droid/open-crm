# Implementation Steps: Brevo Origin Filter

## Step 1: Backend + Frontend + Tests

- [x] Backend: Add `Boolean brevo` param to CompanyController/CompanyService list()
- [x] Backend: Add `Boolean brevo` param to ContactController/ContactService list()
- [x] Backend: Specification clauses for brevoCompanyId/brevoId IS (NOT) NULL
- [x] Frontend: Add `brevo` to CompanyListParams and ContactListParams
- [x] Frontend: Add brevoFilter state and Select dropdown to company-list.tsx
- [x] Frontend: Add brevoFilter state and Select dropdown to contact-list.tsx
- [x] Frontend: Add brevoFilter i18n keys to de.ts and en.ts
- [x] Backend tests: 8 new tests (4 controller + 4 service)
- [x] Frontend tests: 2 new tests (brevo filter combobox renders)

**Acceptance criteria:**
- [x] Backend: 237 tests pass
- [x] Frontend: 152 tests pass, TypeScript clean
