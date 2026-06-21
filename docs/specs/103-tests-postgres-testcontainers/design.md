# Design: Backend tests on PostgreSQL via Testcontainers (drop H2)

## Summary

The backend currently runs all tests against H2 in-memory with `ddl-auto: create-drop` and Flyway disabled. The production database is PostgreSQL 17, and Flyway-managed migrations are the single source of truth for the production schema. This means:

- **Test schema** is generated from JPA entity annotations and never exercises the Flyway migrations.
- **Test SQL** runs against H2's dialect, not PostgreSQL — Postgres-specific features (check constraints, `BYTEA` semantics, `gen_random_uuid()`, `TIMESTAMPTZ`, `ON DELETE CASCADE` interactions) are not verified.
- **Entity ↔ schema drift** is undetectable. A migration that diverges from the entity model (or vice versa) only surfaces in production.

This spec drops H2 entirely and runs all backend tests against a real PostgreSQL 17 container started by Testcontainers, with Flyway-managed schema and `ddl-auto: validate` enforcing entity-model fidelity to the migrations. This is the **second of three currently planned spec initiatives** (image formats → test migration → Meilisearch) and the prerequisite for Spec 104 (Meilisearch) so that those tests benefit from a realistic Postgres-backed setup.

## Goals

- Backend tests run against PostgreSQL 17 (matching `postgres:17-alpine` in `docker-compose.yml`) via Testcontainers.
- Flyway migrations execute as part of the test bootstrap — the test schema is identical to the production schema.
- `ddl-auto: validate` is active so an entity-model change without a corresponding migration breaks the build.
- The H2 dependency is removed from `pom.xml` — no more "works on H2, fails on Postgres" surprises.
- All 9 existing test files keep passing after migration (any failures uncovered by Postgres are fixed in this spec or split into TODOs).
- Local dev test runs use Testcontainers' **container reuse** to keep iteration time short.
- CI (`.github/workflows/build.yml`) runs without changes — `ubuntu-latest` already has Docker available.

## Non-goals

- Splitting tests into "unit" and "integration" suites with separate Spring profiles. The current single `test` profile is preserved; every Spring-based test gets the same container.
- Adding parallel test execution (Maven Surefire `forkCount`/`reuseForks` tuning). Out of scope; can come later if test wall-clock becomes a problem.
- Refactoring tests to use `@Transactional` rollback where they currently don't. The migration only changes the database backend; per-test isolation behavior is preserved.
- Migrating Flyway migrations themselves. They already target PostgreSQL — that is the source of truth.
- Cross-version Postgres testing (PG 15, 16, 17). We pin to 17 to match production.
- Test fixtures, seed data scripts, or test-data builders. Each test sets up the data it needs; this spec doesn't change that contract.
- Frontend tests are unaffected.

## Technical approach

### 1. Maven dependency changes

**`backend/pom.xml`**:

Remove:

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

Add:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

Versions come from Spring Boot's BOM (`spring-boot-dependencies:3.5.13`); no explicit version needed.

`spring-boot-testcontainers` provides the `@ServiceConnection` auto-wiring — Spring Boot 3.1+ feature that registers the container's JDBC URL with the `DataSource` bean without manual `@DynamicPropertySource` plumbing.

### 2. Test infrastructure

A single shared abstract base class plus a static-field container pattern keeps the boot cost amortized across the entire test run.

**New class `backend/src/test/java/com/openelements/crm/AbstractDbTest.java`:**

```java
package com.openelements.crm;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractDbTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withReuse(true);

    static {
        POSTGRES.start();
    }
}
```

**Why a static field + `static{ start() }` instead of `@Container`:**

- `@Container` on a static field starts the container once per *class* by default — we want one container shared across **all** test classes in the run.
- The explicit `static { POSTGRES.start(); }` ensures the container is up before any subclass's Spring context boots.
- Reuse (`.withReuse(true)`) keeps the same Postgres container across `mvn test` runs locally if the dev opts in via `~/.testcontainers.properties` (`testcontainers.reuse.enable=true`). CI ignores reuse (fresh container per run).

**Why a single base class instead of per-test container fields:**

- Spring Boot reuses test contexts that have identical configuration. A single shared container means a single shared application context — boot cost paid once.
- Tests that extend `AbstractDbTest` automatically get the connection wired; no per-test boilerplate.

Existing tests with `@SpringBootTest` (8 of 9) extend `AbstractDbTest` and drop their own `@SpringBootTest`/`@ActiveProfiles` annotations. The 9th (`PreAuthorizeAnnotationTest`, a pure reflection test) is unchanged.

### 3. Test configuration

**`backend/src/test/resources/application-test.yml`** is rewritten:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        # Postgres dialect is auto-detected — no explicit setting needed.
        # JDBC batching is fine for tests.
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://test-issuer.example.com
```

Notes:

- **No `spring.datasource.*` properties** — `@ServiceConnection` injects the JDBC URL, username, password from the running container.
- **`ddl-auto: validate`** means Hibernate compares the entity model to the (Flyway-built) schema on context start. Mismatches fail context loading.
- **Flyway runs once per context boot** — fast (single-digit ms for the existing 32 migrations on a hot container).

### 4. Test data isolation

**Per-test cleanup strategy is unchanged in behavior, but explicit:**

- Tests that use `@Transactional` (or `@DataJpaTest` style) get Spring's automatic rollback at test-method end. Their data never persists.
- Tests that do not use `@Transactional` (most of the existing `@SpringBootTest`-based ones do real HTTP-flow writes) must clean up explicitly. The pragmatic option used here: an `@AfterEach` hook in `AbstractDbTest` that runs a `TRUNCATE … RESTART IDENTITY CASCADE` over all application tables.

```java
@AfterEach
void truncateAll(@Autowired JdbcTemplate jdbc) {
    jdbc.execute("""
        TRUNCATE TABLE
          api_keys, audit_log, comments, contact_comments, contact_tags,
          contacts, company_comments, company_tags, companies, settings,
          social_links, task_comments, task_tags, tasks, tags, users,
          webhooks
        RESTART IDENTITY CASCADE
        """);
}
```

This table list mirrors the current Flyway-defined schema. Adding a new table in a future migration requires extending this list — flagged in `TODO.md` as a maintenance burden, but acceptable: 17 tables, low churn.

(Alternative considered: drop+recreate schema between tests. Rejected — too slow, ~50–100 ms per test cumulative on the existing suite.)

### 5. CI integration

`.github/workflows/build.yml` requires **no change**. `ubuntu-latest` GitHub runners ship with Docker available out of the box; Testcontainers uses it transparently. Tests run as part of the existing `./mvnw clean verify` invocation.

A documentation note in the spec's behaviors.md and a brief mention in the project `README.md` (or a new line in the existing developer-setup section) tells local devs:

- Docker Desktop (or any Docker-compatible daemon) must be running for `mvn test`.
- To enable container reuse for faster local iteration, add `testcontainers.reuse.enable=true` to `~/.testcontainers.properties`.

### 6. Code-quality side effects expected

The migration will surface latent issues that H2 hid. Each is in scope for this spec:

- **`ddl-auto: validate` mismatches** — any entity field with a Flyway column type mismatch (e.g., `String` mapped to `VARCHAR` where the migration declares `TEXT`, or missing `nullable = false` on a column with NOT NULL) will fail context loading. Fix the entity or fix the migration as appropriate, on a case-by-case basis during implementation.
- **Postgres-specific SQL semantics** — any test that asserts behavior that worked accidentally on H2 (e.g., case-insensitive `LIKE` defaults, NULL ordering, identity column generation) may need adjustment. Fixes land in the same PR; deeper issues that affect production code get their own follow-up tickets.
- **Time zone handling** — Postgres `TIMESTAMPTZ` columns are stored as UTC; the test config pins `hibernate.jdbc.time_zone = UTC` to match production-typical configuration. Any test that hard-codes a non-UTC timestamp expectation may need a rewrite.

These surfacing-of-real-bugs is the *value* of this spec — not a side effect to fight.

### 7. Implementation order

Single PR, sequenced as:

1. Add the three Testcontainers/Spring-Boot-Testcontainers dependencies. Keep H2 temporarily.
2. Add `AbstractDbTest` base class with the static container.
3. Rewrite `application-test.yml` for Postgres + Flyway + validate.
4. Migrate one test class as a smoke test (suggest: `UpdatesServiceTest` — small, isolated, exercises real DB). Verify it passes locally.
5. Migrate remaining 7 `@SpringBootTest` classes to extend `AbstractDbTest`.
6. Run full suite locally. Fix any `validate` failures and Postgres-specific test failures.
7. Remove the H2 dependency from `pom.xml`.
8. Update `README.md` (or developer-setup section) with the Docker requirement note.
9. Push, watch CI green, merge.

### 8. Performance

Local first-run cost: ~3–5 s container startup once, plus a few hundred ms for Flyway. Reuse cuts this to ~200 ms on subsequent runs. CI cost: ~3–5 s added to the backend job (currently ~1 minute), so a single-digit-percent slowdown. Acceptable.

Per-test overhead (cleanup `TRUNCATE`): single-digit ms. With the 9 test files in scope, end-to-end suite runtime grows by a few seconds at most.

## Dependencies

- `org.springframework.boot:spring-boot-testcontainers` (test scope) — auto-wiring of container connection info.
- `org.testcontainers:junit-jupiter` (test scope) — `@Testcontainers` lifecycle.
- `org.testcontainers:postgresql` (test scope) — `PostgreSQLContainer<?>`.
- Versions from `spring-boot-dependencies:3.5.13` BOM.
- **Removed:** `com.h2database:h2`.

No production-code dependencies change.

## Security considerations

- The test Postgres container runs only inside the CI runner / local Docker daemon; no exposed ports outside the runner. Credentials are container-generated and ephemeral.
- Tests do not load production data or production secrets.
- The container image (`postgres:17-alpine`) is the same one used in production via `docker-compose.yml`; no new supply-chain surface beyond the Testcontainers JARs themselves (well-known, widely-adopted library).

## GDPR / personal-data note

No change. Tests do not touch real personal data. The test database is wiped between tests and discarded at suite end.

## Open questions

- **Per-test truncate table list maintenance** — adding a new table in a future migration requires extending the `TRUNCATE` list. Realistic and low-churn but worth a one-line comment in `AbstractDbTest`. If this becomes a regular foot-gun, an alternative is reading `information_schema.tables` at runtime and truncating dynamically; not worth the complexity today.
- **`spring-services` test infrastructure** — the upstream library (`com.openelements.spring.base`) provides shared entities/services. If `spring-services` ships its own test base classes against H2, this spec doesn't touch them. A separate alignment (and possible upstream PR) is a candidate follow-up.
- **Surfacing of latent Postgres-specific bugs** — the migration is *expected* to find some. Whether a specific finding is fixed in this PR or split into a follow-up ticket is decided during implementation per-case.
- **JVM-only test for `PreAuthorizeAnnotationTest`** — does not need a container. It stays as-is (no inheritance from `AbstractDbTest`). If future contributors accidentally add DB-touching code without extending `AbstractDbTest`, the context-load failure will be immediate and clear.
