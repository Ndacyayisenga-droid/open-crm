# Implementation Steps: Page Serialization Fix

## Step 1: Enable VIA_DTO on backend

- [ ] Add `@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)` to `CrmApplication.java`
- [ ] Add required import for `EnableSpringDataWebSupport` and `PageSerializationMode`

**Acceptance criteria:**
- [ ] Backend compiles successfully
- [ ] No `PageImpl` serialization warning in logs when calling paginated endpoints
- [ ] JSON response contains nested `page` object instead of flat pagination fields

**Related behaviors:** No PageImpl warning in logs

---

## Step 2: Update frontend Page type and all usages

- [ ] Update `Page<T>` interface in `frontend/src/lib/types.ts` to use nested `page` object
- [ ] Update `frontend/src/components/company-list.tsx` — replace `data.totalElements`, `data.number`, `data.totalPages`, `data.first`, `data.last` with `data.page.*` equivalents
- [ ] Update `frontend/src/components/contact-list.tsx` — same pagination field updates
- [ ] Update `frontend/src/app/companies/print/page.tsx` — replace `result.last` with computed `result.page.number >= result.page.totalPages - 1`
- [ ] Update `frontend/src/app/contacts/print/page.tsx` — same print loop fix
- [ ] Update `frontend/src/components/company-comments.tsx` — replace `result.last` with computed check
- [ ] Update `frontend/src/components/contact-comments.tsx` — replace `result.last` with computed check

**Acceptance criteria:**
- [ ] Frontend compiles without TypeScript errors
- [ ] Company list pagination displays correct count and page numbers
- [ ] Contact list pagination displays correct count and page numbers
- [ ] Print views load all pages correctly
- [ ] Comment "load more" works correctly

**Related behaviors:** Company list returns new page structure, Contact list returns new page structure, Comment list returns new page structure, Company list pagination still works, Contact list pagination still works, Record count display still works, Company print view loads all pages, Contact print view loads all pages, CSV export fetches all pages, Empty result set, Single page of results

---

## Step 3: Update existing tests

- [ ] Update any frontend tests that mock `Page<T>` responses to use the new nested structure

**Acceptance criteria:**
- [ ] All existing tests pass with the new Page structure
- [ ] Frontend builds successfully

**Related behaviors:** All behavior scenarios (test infrastructure)

---

## Step 4: Update project documentation

- [ ] Update `specs/INDEX.md` to mark spec 044 as `done`

**Acceptance criteria:**
- [ ] INDEX.md reflects current status

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| No PageImpl warning in logs | Backend | Step 1 |
| Company list returns new page structure | Backend | Step 1 |
| Contact list returns new page structure | Backend | Step 1 |
| Comment list returns new page structure | Backend | Step 1 |
| Company list pagination still works | Frontend | Step 2 |
| Contact list pagination still works | Frontend | Step 2 |
| Record count display still works | Frontend | Step 2 |
| Company print view loads all pages | Frontend | Step 2 |
| Contact print view loads all pages | Frontend | Step 2 |
| CSV export fetches all pages | Frontend | Step 2 |
| Empty result set | Both | Step 1 + 2 |
| Single page of results | Both | Step 1 + 2 |
