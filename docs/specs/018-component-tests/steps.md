# Implementation Steps: Component-Level Tests

## Step 1: DTO Conversion Tests (Plain Unit Tests)

- [x] Create `CompanyDtoTest.java` — 4 tests (scalar fields, hasLogo true/false, null optionals)
- [x] Create `ContactDtoTest.java` — 5 tests (scalar fields, company resolution, hasPhoto true/false)
- [x] Create `CommentDtoTest.java` — 3 tests (company comment, contact comment, both null)

**Acceptance criteria:**
- [x] All 12 DTO tests pass
- [x] No Spring context needed

---

## Step 2: Repository Tests (@DataJpaTest)

- [x] Create `CompanyRepositoryTest.java` — 6 tests (CRUD, NOT NULL, Specification, existsById)
- [x] Create `ContactRepositoryTest.java` — 7 tests (CRUD, NOT NULL, existsByCompanyId, countByCompanyId)
- [x] Create `CommentRepositoryTest.java` — 10 tests (CRUD, NOT NULL, findBy, deleteBy, count, pagination)

**Acceptance criteria:**
- [x] All 23 repository tests pass

---

## Step 3: Service Tests (@SpringBootTest)

- [x] Create `CompanyServiceTest.java` — 22 tests (CRUD, soft-delete, restore, list filters, logo operations)
- [x] Create `ContactServiceTest.java` — 24 tests (CRUD, company validation, list filters incl. UNKNOWN, photo operations)
- [x] Create `CommentServiceTest.java` — 13 tests (addTo, update, delete, listBy, 404 checks)

**Acceptance criteria:**
- [x] All 59 service tests pass
- [x] Full test suite passes: 192 tests, 0 failures
