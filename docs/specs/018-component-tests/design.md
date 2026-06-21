# Design: Component-Level Tests (Repository, Service, DTO)

## GitHub Issue

_(to be linked once created)_

## Summary

The backend currently has only controller-level integration tests (4 test classes using `@SpringBootTest` + `MockMvc`). These cover the HTTP layer but leave the repository, service, and DTO conversion layers untested in isolation. This spec adds comprehensive component tests for all three layers: plain unit tests for DTO `fromEntity` methods, `@DataJpaTest` tests for repositories, and `@SpringBootTest` integration tests for services. The goal is to catch bugs closer to their source and improve confidence in business logic.

## Goals

- Test every `fromEntity` method on response DTOs (CommentDto, CompanyDto, ContactDto) as plain unit tests without Spring context
- Test all repository interfaces (CompanyRepository, ContactRepository, CommentRepository) including custom query methods and inherited CRUD methods that exercise DB constraints
- Test all service public methods (CompanyService, ContactService, CommentService) including happy paths and error/edge cases
- Follow existing test conventions: `@Nested` classes, AAA pattern
- Achieve test coverage for every public method in the three layers

## Non-goals

- Testing controllers (already covered by existing tests)
- Testing validation annotations on create/update DTOs (covered by controller tests via MockMvc)
- Adding new dependencies or infrastructure (no Testcontainers, no Mockito)
- Code coverage thresholds or reporting tooling

## Technical Approach

### Layer 1: DTO Conversion Tests (Plain Unit Tests)

**Annotation:** None (no Spring context). Just JUnit 5 + direct instantiation.

**Strategy:** Construct entity objects manually using setters, call the static `fromEntity` method, and assert every field on the returned DTO. Since entity constructors are `protected` and there are no setters for `id`, `createdAt`, or `updatedAt`, tests use reflection to set those fields. This is acceptable for test code and avoids polluting the entity API.

**Test classes:**
- `CommentDtoTest` — tests `CommentDto.fromEntity(CommentEntity)`
- `CompanyDtoTest` — tests `CompanyDto.fromEntity(CompanyEntity, long, long)`
- `ContactDtoTest` — tests `ContactDto.fromEntity(ContactEntity, long)`

**Rationale:** `fromEntity` is a pure static method with no Spring dependencies. Launching a context would add seconds of startup time for no benefit.

### Layer 2: Repository Tests (`@DataJpaTest`)

**Annotation:** `@DataJpaTest` (no `@ActiveProfiles` needed)

**Strategy:** Use `@Autowired` to inject the repository under test. Use `TestEntityManager` (provided by `@DataJpaTest`) for arranging test data and flushing/clearing the persistence context to ensure queries hit the database rather than the first-level cache.

**Test isolation:** `@DataJpaTest` is `@Transactional` by default — each test runs in a transaction that is rolled back after the test completes.

**Why no `@ActiveProfiles("test")`:** `@DataJpaTest` automatically configures an embedded H2 database when H2 is on the test classpath. It replaces any configured DataSource with an embedded one. No profile annotation is needed.

**Test classes:**
- `CompanyRepositoryTest` — inherited CRUD + DB constraint verification (name NOT NULL) + Specification queries
- `ContactRepositoryTest` — `existsByCompanyId`, `countByCompanyId` + inherited CRUD constraints (firstName/lastName NOT NULL)
- `CommentRepositoryTest` — `findByCompanyId`, `findByContactId`, `deleteByContactId`, `countByCompanyId`, `countByContactId` + CRUD constraints (text/author NOT NULL) + pagination

### Layer 3: Service Tests (`@SpringBootTest`)

**Annotation:** `@SpringBootTest` + `@ActiveProfiles("test")`

**Strategy:** Services depend on multiple repositories and contain business logic with cross-entity validation (e.g., ContactService checks that a company exists and is not soft-deleted). Using the real Spring context with H2 provides the most realistic test environment.

**Test isolation:** `@BeforeEach` deletes all data via repositories in FK order (comments → contacts → companies). `@Transactional` is NOT used on service tests because some service methods are themselves `@Transactional` and the behavior under test includes transaction boundaries.

**Why not Mockito:** The codebase is small enough that full integration is fast. Mocks would add complexity without proportional value and miss integration bugs.

**Test classes:**
- `CompanyServiceTest` — `create`, `getById`, `update`, `delete` (soft-delete + 409 conflict), `restore`, `list` (with filters), `uploadLogo`, `getLogo`, `deleteLogo`
- `ContactServiceTest` — `create`, `getById`, `update`, `delete` (hard-delete + comment cascade), `list` (with filters including UNKNOWN language), `uploadPhoto`, `getPhoto`, `deletePhoto`, company validation (nonexistent, soft-deleted)
- `CommentServiceTest` — `addToCompany`, `addToContact`, `update`, `delete`, `listByCompany`, `listByContact`, existence validation

## File Structure

```
backend/src/test/java/com/openelements/crm/
    comment/
        CommentControllerTest.java  (existing)
        CommentDtoTest.java         (new)
        CommentRepositoryTest.java  (new)
        CommentServiceTest.java     (new)
    company/
        CompanyControllerTest.java  (existing)
        CompanyDtoTest.java         (new)
        CompanyRepositoryTest.java  (new)
        CompanyServiceTest.java     (new)
    contact/
        ContactControllerTest.java  (existing)
        ContactDtoTest.java         (new)
        ContactRepositoryTest.java  (new)
        ContactServiceTest.java     (new)
```

9 new test files. No changes to `application-test.yml` or `pom.xml` required.

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| DTO tests: no Spring context | Plain JUnit 5 | `fromEntity` is pure static — no Spring needed |
| DTO tests: reflection for entity IDs/timestamps | Private test helper | Avoids adding setters that weaken entity encapsulation |
| Repository tests: `@DataJpaTest` without profile | Auto-configured H2 | `@DataJpaTest` replaces DataSource automatically |
| Repository tests: `TestEntityManager` | Spring Boot test utility | Allows `persistAndFlush` + `clear()` to force real DB roundtrips |
| Service tests: real H2, no mocks | Full integration | Cross-repository logic needs real interactions |
| Service tests: `@BeforeEach` cleanup, not `@Transactional` | Manual delete in FK order | Avoids hiding transaction boundary bugs |

## Estimated Test Counts

| Test class | Estimated methods |
|------------|------------------|
| CommentDtoTest | 3 |
| CompanyDtoTest | 4 |
| ContactDtoTest | 5 |
| CommentRepositoryTest | 10 |
| CompanyRepositoryTest | 6 |
| ContactRepositoryTest | 7 |
| CommentServiceTest | 12 |
| CompanyServiceTest | 18 |
| ContactServiceTest | 18 |
| **Total** | **~83** |

## Dependencies

- JUnit 5 (already in `spring-boot-starter-test`)
- H2 (already a test dependency)
- No new dependencies

## Open Questions

- None
