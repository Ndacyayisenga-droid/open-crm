# Design: Fix Image Column Type Mismatch

## GitHub Issue

_(to be linked once created)_

## Summary

The implementation of Spec 013 (Image Upload) introduced a type mismatch between the Flyway migration and the JPA entity mapping. The migration `V5__add_images.sql` creates the `logo` and `photo` columns as `BYTEA` in PostgreSQL, but the JPA entities annotate the corresponding `byte[]` fields with `@Lob`. Hibernate maps `@Lob` on `byte[]` to `oid` (PostgreSQL Large Object) by default, which conflicts with the actual `BYTEA` column type. Hibernate's schema validation (`ddl-auto: validate`) detects this mismatch and prevents the application from starting. The entire application is unusable — no companies or contacts can be viewed or created.

## Reproduction

1. Start the application with `docker compose up`
2. Backend fails to start with:
   ```
   Schema-validation: wrong column type encountered in column [logo] in table [companies];
   found [bytea (Types#BINARY)], but expecting [oid (Types#BLOB)]
   ```
3. Frontend cannot reach the backend (`ENOTFOUND backend`) — all API calls fail

**Preconditions:** PostgreSQL database with migration V5 applied (columns exist as `BYTEA`).

## Root Cause Analysis

The `@Lob` annotation on a `byte[]` field tells Hibernate to use the database's large object type. On PostgreSQL, Hibernate maps this to `oid` — a reference to a PostgreSQL Large Object stored externally. However, the Flyway migration created the columns as `BYTEA`, which stores binary data inline in the row.

When `ddl-auto: validate` is active, Hibernate compares the expected column type (`oid`) against the actual column type (`bytea`) and throws a `SchemaManagementException`.

**Affected fields:**
- `CompanyEntity.logo` — `@Lob @Basic(fetch = FetchType.LAZY) byte[]`
- `ContactEntity.photo` — `@Lob @Basic(fetch = FetchType.LAZY) byte[]`

## Fix Approach

Remove `@Lob` from both fields and replace it with `@Column(length = 2_097_152)` (2 MB, matching the configured multipart upload limit). This is pure JPA — no Hibernate-specific annotations.

**Why this approach:**
- **JPA-standard only:** No dependency on Hibernate-specific annotations like `@JdbcTypeCode(SqlTypes.BINARY)`.
- **Portable:** `byte[]` with `@Column(length = ...)` maps correctly to both PostgreSQL (`BYTEA`) and H2 (`BINARY`), which are the two databases used in this project (H2 for tests, PostgreSQL for production).
- **No migration needed:** The existing Flyway migration `V5__add_images.sql` already creates `BYTEA` columns, which is exactly what Hibernate will expect after this fix.
- **No data impact:** All `logo` and `photo` values are currently `NULL` — no data conversion required.

**Alternatives considered:**
- `@JdbcTypeCode(SqlTypes.BINARY)` — Hibernate-specific, violates the JPA-only constraint.
- `@Column(columnDefinition = "bytea")` — PostgreSQL-specific DDL, would break H2 tests.
- New migration to change columns to `OID` — unnecessary complexity, `BYTEA` is the better fit for inline image storage.

**Files changed:**
- `backend/src/main/java/com/openelements/crm/company/CompanyEntity.java` — remove `@Lob`, add `@Column(length = 2_097_152)`
- `backend/src/main/java/com/openelements/crm/contact/ContactEntity.java` — remove `@Lob`, add `@Column(length = 2_097_152)`

## Regression Risk

- **Low.** Only JPA annotations change — no schema migration, no data transformation, no API changes.
- The `@Basic(fetch = FetchType.LAZY)` annotation remains, so lazy loading behavior is preserved.
- Rollback is trivial via `git revert`.

## Open Questions

- None