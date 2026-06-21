# Implementation Steps: Tags Backend

## Step 1: Flyway migration and Tag entity

- [x] Create `V12__create_tags.sql` migration with `tags`, `company_tags`, `contact_tags` tables
- [x] Create `tag/TagEntity.java` JPA entity
- [x] Create `tag/TagRepository.java` Spring Data repository
- [x] Add `@ManyToMany` tags relationship to `CompanyEntity`
- [x] Add `@ManyToMany` tags relationship to `ContactEntity`

**Acceptance criteria:**
- [x] `mvn compile` succeeds
- [x] Entity relationships correctly defined

---

## Step 2: Tag DTOs and Service

- [x] Create `tag/TagDto.java` response DTO
- [x] Create `tag/TagCreateDto.java` request DTO with `@NotBlank` validation
- [x] Create `tag/TagService.java` with CRUD operations and name uniqueness check
- [x] Add `tagIds` to `CompanyDto`, `CompanyCreateDto`, `CompanyUpdateDto`
- [x] Add `tagIds` to `ContactDto`, `ContactCreateDto`, `ContactUpdateDto`
- [x] Update `CompanyService` to handle tag assignment on create/update and include tagIds in DTO mapping
- [x] Update `ContactService` to handle tag assignment on create/update and include tagIds in DTO mapping

**Acceptance criteria:**
- [x] `mvn compile` succeeds

---

## Step 3: Tag Controller

- [x] Create `tag/TagController.java` with CRUD endpoints
- [x] Add `@SecurityRequirement` and Swagger annotations

**Acceptance criteria:**
- [x] `mvn compile` succeeds

---

## Step 4: Tests for all behavioral scenarios

- [x] Tag CRUD tests (create, get, list, update, delete)
- [x] Tag validation tests (duplicate name, missing fields)
- [x] Company tag assignment tests (create with tags, update add/replace/remove/null)
- [x] Contact tag assignment tests
- [x] Cascade delete tests
- [x] Invalid tag ID tests
- [x] Backward compatibility tests
- [x] Update existing company/contact tests for tagIds in responses

**Acceptance criteria:**
- [x] `mvn clean verify` passes with all tests

---

## Step 5: Update project documentation

- [x] Update project-features.md, project-structure.md, project-architecture.md

**Acceptance criteria:**
- [x] Documentation reflects tags feature
