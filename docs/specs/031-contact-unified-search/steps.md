# Implementation Steps: Contact Unified Search

## Step 1: Backend + Frontend + Tests

- [x] Backend: Replace firstName/lastName/email params with single `search` param in controller/service
- [x] Backend: Multi-word search with LEFT JOIN across firstName, lastName, email, company name
- [x] Backend tests: 5 search tests (name, email, company name, multi-word AND, contacts without company)
- [x] Frontend: Replace 3 filter inputs with single search input
- [x] Frontend: Update API client, i18n, tests

**Acceptance criteria:**
- [x] Backend: 259 tests pass
- [x] Frontend: 159 tests pass, TypeScript clean
