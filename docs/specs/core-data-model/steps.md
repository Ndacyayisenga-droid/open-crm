# Implementation Steps: Core Data Model

## Step 1: Add Validation Dependency and Flyway Migrations

- [x] Add `spring-boot-starter-validation` to `pom.xml`
- [x] Create `V1__create_companies.sql` — companies table with all columns, soft-delete flag, timestamps
- [x] Create `V2__create_contacts.sql` — contacts table with FK to companies, enums stored as VARCHAR, timestamps
- [x] Create `V3__create_comments.sql` — comments table with FKs to companies and contacts, CHECK constraint (exactly one FK non-null), ON DELETE CASCADE for contact FK

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [ ] Flyway migrations run successfully against PostgreSQL (via Docker Compose — to verify when Docker available)
- [x] All three tables are created with correct columns, constraints, and foreign keys

**Related behaviors:** Flyway migrations run successfully

---

## Step 2: Company Entity and Repository

- [x] Create `Gender` enum in `com.openelements.crm.contact` package (MALE, FEMALE, DIVERSE)
- [x] Create `Language` enum in `com.openelements.crm.contact` package (DE, EN)
- [x] Create `CompanyEntity` JPA class in `com.openelements.crm.company` with all fields, `@Entity`, `@Table`, `@Id`, `@GeneratedValue(UUID)`, `@Column` mappings, `@CreationTimestamp`, `@UpdateTimestamp`
- [x] Create `CompanyRepository` extending `JpaRepository<CompanyEntity, UUID>` and `JpaSpecificationExecutor<CompanyEntity>`

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] Entity has Javadoc on public API, equals/hashCode/toString
- [x] No Lombok usage

**Related behaviors:** None directly — foundation for Company CRUD

---

## Step 3: Contact and Comment Entities and Repositories

- [x] Create `ContactEntity` JPA class in `com.openelements.crm.contact` with all fields, `@ManyToOne` to CompanyEntity (optional), `@Enumerated` for gender and language
- [x] Create `ContactRepository` extending `JpaRepository` and `JpaSpecificationExecutor`
- [x] Create `CommentEntity` JPA class in `com.openelements.crm.comment` with nullable FKs to company and contact
- [x] Create `CommentRepository` extending `JpaRepository` with custom query methods for finding by companyId/contactId

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] Contact entity has optional ManyToOne to Company
- [x] All entities have Javadoc, equals/hashCode/toString

**Related behaviors:** None directly — foundation for Contact and Comment CRUD

---

## Step 4: Company DTOs and Service

- [x] Create `CompanyCreateRequest` record with validation annotations (`@NotBlank` on name)
- [x] Create `CompanyUpdateRequest` record with validation annotations
- [x] Create `CompanyResponse` record with all company fields
- [x] Create `CompanyService` with methods: create, getById, update, delete (soft), restore, list (paginated/filtered/sorted)
- [x] Implement JPA `Specification` for company filtering (name, city, country, includeDeleted)

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] DTOs are Java records with Javadoc and `@Schema` annotations
- [x] Service validates soft-delete preconditions (no contacts)
- [x] Service maps between entities and DTOs
- [x] Specification supports partial-match name filter (case-insensitive)

**Related behaviors:** All Company CRUD and soft-delete scenarios (service layer logic)

---

## Step 5: Company Controller

- [x] Create `CompanyController` with all endpoints: GET list, GET by ID, POST create, PUT update, DELETE soft-delete, POST restore, GET comments
- [x] Add OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`) on all endpoints
- [x] Use `@Valid` on request bodies
- [x] Return appropriate HTTP status codes (201 for create, 204 for delete, 409 for conflict)

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] All endpoints mapped under `/api/companies`
- [x] Controller delegates to service layer, no business logic in controller

**Related behaviors:** All Company CRUD, soft-delete, restore, list scenarios (API layer)

---

## Step 6: Contact DTOs and Service

- [x] Create `ContactCreateRequest` record (without syncedToBrevo/doubleOptIn — read-only fields)
- [x] Create `ContactUpdateRequest` record (without syncedToBrevo/doubleOptIn)
- [x] Create `ContactResponse` record with all fields including `companyName`
- [x] Create `ContactService` with CRUD methods and company validation (exists, not soft-deleted)
- [x] Implement JPA `Specification` for contact filtering (firstName, lastName, email, companyId, language)

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] Create/Update DTOs do not include syncedToBrevo or doubleOptIn
- [x] Service rejects contacts referencing non-existent or soft-deleted companies
- [x] ContactResponse includes companyName (resolved from entity)

**Related behaviors:** All Contact CRUD scenarios (service layer logic)

---

## Step 7: Contact Controller

- [x] Create `ContactController` with all endpoints: GET list, GET by ID, POST create, PUT update, DELETE hard-delete, GET comments
- [x] Add OpenAPI annotations on all endpoints
- [x] Use `@Valid` on request bodies

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] All endpoints mapped under `/api/contacts`
- [x] DELETE returns 204 and physically removes contact

**Related behaviors:** All Contact CRUD and list scenarios (API layer)

---

## Step 8: Comment DTOs and Service

- [x] Create `CommentCreateRequest` record with `@NotBlank` on text and author
- [x] Create `CommentUpdateRequest` record with `@NotBlank` on text and author
- [x] Create `CommentResponse` record
- [x] Create `CommentService` with methods: addToCompany, addToContact, update, delete, listByCompany, listByContact
- [x] Comments on soft-deleted companies are allowed

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] Service validates that the target company/contact exists
- [x] List methods return paginated results sorted by createdAt DESC

**Related behaviors:** All Comment CRUD scenarios (service layer logic)

---

## Step 9: Comment Controller

- [x] Create `CommentController` with endpoints: PUT update, DELETE
- [x] Add comment creation endpoints to `CompanyController` (POST `/api/companies/{id}/comments`)
- [x] Add comment creation endpoints to `ContactController` (POST `/api/contacts/{id}/comments`)
- [x] Add OpenAPI annotations on all endpoints

**Acceptance criteria:**
- [x] `./mvnw clean compile` succeeds
- [x] Comment creation is nested under company/contact paths
- [x] Comment update/delete is at `/api/comments/{id}`

**Related behaviors:** All Comment CRUD scenarios (API layer)

---

## Step 10: Company E2E Tests

- [x] Create `CompanyControllerTest` with MockMvc tests covering:
  - Create company with all fields → 201
  - Create company with only name → 201
  - Create company without name → 400
  - Get company by ID → 200
  - Get non-existent company → 404
  - Update company → 200
  - Update non-existent company → 404
  - Update company with blank name → 400
  - Soft-delete company without contacts → 204
  - Soft-delete company with contacts → 409
  - Soft-delete non-existent company → 404
  - Restore soft-deleted company → 200
  - Restore non-deleted company → 200 (idempotent)
  - Restore non-existent company → 404
  - Get soft-deleted company by ID → 200 with deleted=true

**Acceptance criteria:**
- [x] `./mvnw clean verify` succeeds with all tests passing
- [x] Tests use `//GIVEN //WHEN //THEN` structure
- [x] Tests use `@Nested` and `@DisplayName`

**Related behaviors:** Create company with all fields, Create company with only required fields, Create company fails without name, Get company by ID, Get company with non-existent ID, Update company, Update company with non-existent ID, Update company fails with blank name, Soft-delete company without contacts, Soft-delete company with contacts fails, Soft-delete non-existent company, Restore soft-deleted company, Restore non-deleted company, Restore non-existent company, Get soft-deleted company by ID

---

## Step 11: Company List/Pagination/Filter/Sort Tests

- [x] Add tests to `CompanyControllerTest` covering:
  - Default pagination (20 per page)
  - Custom page size
  - Excludes soft-deleted by default
  - Includes soft-deleted with filter
  - Filter by name (partial, case-insensitive)
  - Filter by city
  - Filter by country
  - Sort by name
  - Sort by createdAt

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds with all tests passing
- [x] Pagination metadata (totalElements, totalPages, page, size) is verified

**Related behaviors:** List companies with default pagination, List companies with custom page size, List companies excludes soft-deleted by default, List companies includes soft-deleted with filter, Filter companies by name, Filter companies by city, Filter companies by country, Sort companies by name, Sort companies by creation date

---

## Step 12: Contact E2E Tests

- [x] Create `ContactControllerTest` with MockMvc tests covering:
  - Create contact with all fields → 201
  - Create contact without company → 201
  - Create contact with non-existent company → 400
  - Create contact with soft-deleted company → 400
  - Create contact without required fields → 400
  - Create contact with null gender → 201
  - Get contact by ID (with companyName) → 200
  - Update contact → 200
  - Update contact ignores Brevo fields → 200
  - Hard-delete contact (cascades comments) → 204
  - Delete non-existent contact → 404

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds with all tests passing

**Related behaviors:** Create contact with all fields, Create contact without company, Create contact with non-existent company fails, Create contact with soft-deleted company fails, Create contact fails without required fields, Create contact with null gender, Get contact by ID, Update contact, Update contact ignores Brevo fields, Hard-delete contact, Delete non-existent contact

---

## Step 13: Contact List/Pagination/Filter/Sort Tests

- [x] Add tests to `ContactControllerTest` covering (included in ContactControllerTest):
  - Default pagination
  - Filter by lastName (partial, case-insensitive)
  - Filter by firstName
  - Filter by email
  - Filter by companyId
  - Filter by language
  - Sort by lastName

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds with all tests passing

**Related behaviors:** List contacts with default pagination, Filter contacts by last name, Filter contacts by first name, Filter contacts by email, Filter contacts by company, Filter contacts by language, Sort contacts by last name

---

## Step 14: Comment E2E Tests

- [x] Create `CommentControllerTest` with MockMvc tests covering:
  - Add comment to company → 201
  - Add comment to contact → 201
  - Add comment to non-existent company → 404
  - Add comment to non-existent contact → 404
  - Add comment without text → 400
  - Add comment without author → 400
  - Add comment to soft-deleted company → 201
  - List comments for company (sorted by createdAt desc) → 200
  - List comments for contact → 200
  - Update comment → 200
  - Update comment with blank text → 400
  - Delete comment → 204
  - Delete non-existent comment → 404

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds with all tests passing

**Related behaviors:** Add comment to company, Add comment to contact, Add comment to non-existent company, Add comment to non-existent contact, Add comment fails without text, Add comment fails without author, Add comment to soft-deleted company, List comments for company, List comments for contact, Update comment, Update comment fails with blank text, Delete comment, Delete non-existent comment

---

## Step 15: Cascade and Referential Integrity Tests

- [x] Add tests covering (included in CommentControllerTest):
  - Contact deletion cascades all comments
  - Company soft-delete preserves comments (still accessible)
  - Contact cannot reference soft-deleted company (create)
  - Contact cannot be moved to soft-deleted company (update)

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds with all tests passing
- [x] Cascade behavior verified at database level

**Related behaviors:** Contact deletion cascades comments, Company soft-delete preserves comments, Contact cannot reference soft-deleted company, Contact cannot be moved to soft-deleted company

---

## Step 16: OpenAPI Verification Test

- [x] Add test verifying all endpoints are documented in OpenAPI spec (extended existing HealthControllerTest):
  - Company endpoints (7)
  - Contact endpoints (6)
  - Comment endpoints (4)
  - Request/response schemas present

**Acceptance criteria:**
- [ ] `./mvnw clean verify` succeeds
- [x] OpenAPI spec at `/v3/api-docs` contains all endpoint paths

**Related behaviors:** All endpoints are documented in Swagger UI

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create company with all fields | Backend | Step 10 |
| Create company with only required fields | Backend | Step 10 |
| Create company fails without name | Backend | Step 10 |
| Get company by ID | Backend | Step 10 |
| Get company with non-existent ID | Backend | Step 10 |
| Update company | Backend | Step 10 |
| Update company with non-existent ID | Backend | Step 10 |
| Update company fails with blank name | Backend | Step 10 |
| Soft-delete company without contacts | Backend | Step 10 |
| Soft-delete company with contacts fails | Backend | Step 10 |
| Soft-delete non-existent company | Backend | Step 10 |
| Restore soft-deleted company | Backend | Step 10 |
| Restore non-deleted company | Backend | Step 10 |
| Restore non-existent company | Backend | Step 10 |
| Get soft-deleted company by ID | Backend | Step 10 |
| List companies with default pagination | Backend | Step 11 |
| List companies with custom page size | Backend | Step 11 |
| List companies excludes soft-deleted by default | Backend | Step 11 |
| List companies includes soft-deleted with filter | Backend | Step 11 |
| Filter companies by name | Backend | Step 11 |
| Filter companies by city | Backend | Step 11 |
| Filter companies by country | Backend | Step 11 |
| Sort companies by name | Backend | Step 11 |
| Sort companies by creation date | Backend | Step 11 |
| Create contact with all fields | Backend | Step 12 |
| Create contact without company | Backend | Step 12 |
| Create contact with non-existent company fails | Backend | Step 12 |
| Create contact with soft-deleted company fails | Backend | Step 12 |
| Create contact fails without required fields | Backend | Step 12 |
| Create contact with null gender | Backend | Step 12 |
| Get contact by ID | Backend | Step 12 |
| Update contact | Backend | Step 12 |
| Update contact ignores Brevo fields | Backend | Step 12 |
| Hard-delete contact | Backend | Step 12 |
| Delete non-existent contact | Backend | Step 12 |
| List contacts with default pagination | Backend | Step 13 |
| Filter contacts by last name | Backend | Step 13 |
| Filter contacts by first name | Backend | Step 13 |
| Filter contacts by email | Backend | Step 13 |
| Filter contacts by company | Backend | Step 13 |
| Filter contacts by language | Backend | Step 13 |
| Sort contacts by last name | Backend | Step 13 |
| Add comment to company | Backend | Step 14 |
| Add comment to contact | Backend | Step 14 |
| Add comment to non-existent company | Backend | Step 14 |
| Add comment to non-existent contact | Backend | Step 14 |
| Add comment fails without text | Backend | Step 14 |
| Add comment fails without author | Backend | Step 14 |
| Add comment to soft-deleted company | Backend | Step 14 |
| List comments for company | Backend | Step 14 |
| List comments for contact | Backend | Step 14 |
| Update comment | Backend | Step 14 |
| Update comment fails with blank text | Backend | Step 14 |
| Delete comment | Backend | Step 14 |
| Delete non-existent comment | Backend | Step 14 |
| Contact deletion cascades comments | Backend | Step 15 |
| Company soft-delete preserves comments | Backend | Step 15 |
| Contact cannot reference soft-deleted company | Backend | Step 15 |
| Contact cannot be moved to soft-deleted company | Backend | Step 15 |
| All endpoints are documented in Swagger UI | Backend | Step 16 |
| Flyway migrations run successfully | Backend | Step 1 |
