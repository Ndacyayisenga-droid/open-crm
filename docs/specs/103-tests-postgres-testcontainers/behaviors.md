# Behaviors: Backend tests on PostgreSQL via Testcontainers

These behaviors describe observable properties of the test infrastructure after migration. Most are not "test cases" in the application-feature sense — they are invariants the new setup must satisfy.

## Test bootstrap

### A single PostgreSQL container is shared across the entire test run

- **Given** a clean local environment with Docker running
- **When** the developer runs `./mvnw test`
- **Then** exactly one `postgres:17-alpine` container is started by Testcontainers
- **And** the container ID is the same across all test classes executed in that run (verifiable via Testcontainers logs)
- **And** the container is stopped after the JVM exits (or kept warm if `testcontainers.reuse.enable=true` is set)

### Flyway runs once and applies all migrations

- **Given** the container has just started
- **When** the first `@SpringBootTest` context loads
- **Then** Flyway runs against the container's empty database
- **And** all migrations under `classpath:db/migration` apply cleanly
- **And** the test log shows the same migration count as `V*.sql` files in the repo

### `ddl-auto: validate` succeeds on a clean schema

- **Given** Flyway has just applied all migrations
- **When** Hibernate boots with `ddl-auto: validate`
- **Then** no schema-validation exception is thrown
- **And** the application context starts within the normal Spring Boot timeout

### `ddl-auto: validate` fails fast on an entity ↔ schema mismatch

- **Given** a developer adds a new field to a JPA entity without a corresponding Flyway migration
- **When** they run the test suite
- **Then** the Spring context fails to load with a `SchemaManagementException` (or equivalent Hibernate validation error)
- **And** the error message identifies the missing column or type mismatch
- **And** no tests execute

## Container connection wiring

### `@ServiceConnection` provides the JDBC URL without manual properties

- **Given** the new `AbstractDbTest` base class
- **When** a subclass `@SpringBootTest` boots
- **Then** the `DataSource` bean is configured against the running container
- **And** no `spring.datasource.url`, `spring.datasource.username`, or `spring.datasource.password` is set in `application-test.yml`

### Test profile is active

- **Given** any test extending `AbstractDbTest`
- **When** the Spring context loads
- **Then** the `test` profile is the active profile
- **And** `application-test.yml` overrides take effect (Flyway enabled, `ddl-auto: validate`, OAuth test issuer)

## Per-test isolation

### Tests do not leak data into each other

- **Given** two consecutive test methods that both create the same contact (same email, same name)
- **When** the suite runs them in order
- **Then** both pass independently
- **And** the second test starts with an empty `contacts` table

### `@AfterEach` truncate runs after every test method

- **Given** a test method that inserts rows into multiple tables
- **When** the method returns
- **Then** every application table is truncated via `TRUNCATE ... RESTART IDENTITY CASCADE`
- **And** the next test sees no leftover data

### `@Transactional` tests continue to roll back

- **Given** a test method annotated with `@Transactional`
- **When** the test inserts data and returns
- **Then** Spring's automatic rollback removes the data (as before)
- **And** the `@AfterEach` truncate is a no-op for the rolled-back data (harmless redundancy)

## H2 removal

### The H2 driver is not on the test classpath

- **Given** the migrated `pom.xml`
- **When** `mvn dependency:tree -Dscope=test` runs
- **Then** `com.h2database:h2` does not appear
- **And** the classpath has no `org.h2.Driver`

### The Postgres driver is on the test classpath

- **Given** the migrated `pom.xml`
- **When** `mvn dependency:tree -Dscope=test` runs
- **Then** `org.postgresql:postgresql` appears (already a production dependency)
- **And** `org.testcontainers:postgresql` appears at test scope

### No test file still references H2

- **Given** the migrated test sources
- **When** the developer greps `org.h2` or `H2Dialect` across `backend/src/test`
- **Then** there are no matches

## Existing tests

### All 9 existing test files pass against Postgres

- **Given** the migration is complete (PR ready to merge)
- **When** `./mvnw clean verify` runs
- **Then** every existing test passes
- **And** any test failures uncovered during migration were either fixed in the same PR or split into follow-up tickets with clear references

### `PreAuthorizeAnnotationTest` is unaffected

- **Given** the migration is complete
- **When** the test runs
- **Then** it executes without booting a Spring context
- **And** it does not require Docker

## CI integration

### CI pipeline runs unchanged

- **Given** the migration is merged
- **When** the GitHub Actions `build.yml` workflow triggers on push or PR
- **Then** the `backend` job uses the existing `./mvnw clean verify` command
- **And** no workflow YAML changes were needed
- **And** the job completes within a single-digit-percent slowdown vs. pre-migration timing

### CI runs a fresh container, no reuse

- **Given** a CI build
- **When** Testcontainers starts the Postgres container
- **Then** the container is freshly created (no `testcontainers.reuse.enable` is configured on the runner)
- **And** the container is destroyed at the end of the build

## Local developer experience

### Documentation tells developers about the Docker requirement

- **Given** the migration is merged
- **When** a new developer reads the project README (or developer-setup section)
- **Then** the document states that Docker must be running for `mvn test`
- **And** the document mentions the optional `testcontainers.reuse.enable=true` flag for faster local iteration

### Missing Docker produces a clear error

- **Given** a developer runs `mvn test` without Docker running
- **When** the test bootstrap attempts to start the container
- **Then** the error message clearly identifies that Docker is unavailable (Testcontainers' standard "Could not find a valid Docker environment" output is sufficient)

## Migrations are the source of truth

### A new migration is exercised by the test suite

- **Given** a developer adds a new `V33__some_change.sql` Flyway migration
- **When** they run the test suite
- **Then** the migration applies as part of the test bootstrap
- **And** any incompatibility with the entity model surfaces immediately via `ddl-auto: validate`

### Test schema and production schema are byte-identical at the migration level

- **Given** any test run and a fresh production deploy from the same commit
- **When** Flyway has finished on both
- **Then** the resulting schemas are identical (same DDL applied in the same order)
- **And** any Postgres-specific construct used in production (e.g., `gen_random_uuid()`, `TIMESTAMPTZ`, check constraints) is exercised by the test bootstrap
