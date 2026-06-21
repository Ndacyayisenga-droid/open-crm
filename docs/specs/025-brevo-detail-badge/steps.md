# Implementation Steps: Brevo Detail Badge

## Step 1: Backend + Frontend + Tests

- [x] Backend: Add computed `brevo` field to CompanyDto (brevoCompanyId != null)
- [x] Backend tests: CompanyDtoTest, CompanyControllerTest, HealthControllerTest updated
- [x] Frontend types: `brevo: boolean` added to CompanyDto
- [x] Frontend: Brevo tag below name in company-detail.tsx and contact-detail.tsx
- [x] Frontend: Removed CheckboxField for synced-to-Brevo from contact-detail.tsx
- [x] Frontend: i18n cleanup (removed old brevo key from contacts.detail)
- [x] Frontend tests: Tag rendering tests added, checkbox tests removed

**Acceptance criteria:**
- [x] Backend: 239 tests pass
- [x] Frontend: 155 tests pass, TypeScript clean
