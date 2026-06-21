# Implementation Steps: Backend OIDC Auth

## Step 1: Add dependencies and configure Spring Security

- [x] Add `spring-boot-starter-oauth2-resource-server` to `pom.xml`
- [x] Add `spring-security-test` (test scope) to `pom.xml`
- [x] Add `spring.security.oauth2.resourceserver.jwt.issuer-uri` to `application.yml`
- [x] Add JWT issuer-uri override for test profile in `application-test.yml`
- [x] Create `SecurityConfig.java` with `SecurityFilterChain` bean
- [x] Configure public access for health and Swagger endpoints, authenticated for all `/api/**`

**Acceptance criteria:**
- [x] `mvn compile` succeeds
- [x] SecurityConfig correctly permits health/swagger and requires auth for API endpoints

**Related behaviors:** Request with valid token succeeds, Request without token returns 401, Request with invalid token returns 401, Health endpoint accessible without token, Swagger UI accessible without token, OpenAPI docs accessible without token

---

## Step 2: Update UserService to extract user from JWT

- [x] Replace hardcoded dummy user in `UserService` with JWT claim extraction from `SecurityContext`
- [x] Handle missing name claim (fallback to "Unknown")
- [x] Handle missing email claim (fallback to empty string)
- [x] Throw `IllegalStateException` when no authentication is present

**Acceptance criteria:**
- [x] `mvn compile` succeeds
- [x] UserService reads from SecurityContext instead of returning static values

**Related behaviors:** UserService returns user from JWT claims, UserService handles missing name claim, UserService handles missing email claim, UserService throws without authentication, Comment author set from authenticated user

---

## Step 3: Configure Swagger UI OAuth2 authorize button

- [x] Create `OpenApiConfig.java` with OIDC security scheme
- [x] Add `@SecurityRequirement` annotations to all protected controllers

**Acceptance criteria:**
- [x] `mvn compile` succeeds
- [x] OpenAPI spec includes security scheme definition

**Related behaviors:** Swagger UI shows Authorize button, Protected endpoints callable after authorization in Swagger

---

## Step 4: Update Docker Compose for backend OIDC

- [x] Add `OIDC_ISSUER_URI` to backend service environment in `docker-compose.yml`
- [x] Add `OIDC_ISSUER_URI` override in `docker-compose.override.yml` for backend to use mock server internal URL

**Acceptance criteria:**
- [x] `docker-compose.yml` backend service has `OIDC_ISSUER_URI`
- [x] `docker-compose.override.yml` overrides it for mock server

**Related behaviors:** OIDC_ISSUER_URI passed to backend service, Override points to mock server internal URL

---

## Step 5: Update all existing tests with mock JWT security

- [x] Update all controller tests to use `.with(jwt().jwt(...))` for MockMvc requests
- [x] Update all service tests that depend on UserService to set up mock SecurityContext
- [x] Update comment author assertions from "Demo User" to test JWT claim value
- [x] Ensure health/swagger tests remain without JWT (public endpoints)

**Acceptance criteria:**
- [x] `mvn clean verify` passes with all existing tests
- [x] No test regressions

**Related behaviors:** Service tests work with mock security context, Existing tests pass with security enabled

---

## Step 6: Add security-specific behavioral tests

- [x] Test: request without token returns 401 for protected endpoints
- [x] Test: request with valid JWT returns 200
- [x] Test: health endpoint returns 200 without token
- [x] Test: Swagger/OpenAPI accessible without token
- [x] Test: all CRUD endpoints return 401 without token
- [x] Test: CSV export returns 401 without token
- [x] Test: image endpoints return 401 without token
- [x] Test: Brevo sync returns 401 without token
- [x] Test: comment author set from JWT name claim
- [x] Test: different users create comments with their own names

**Acceptance criteria:**
- [x] All new tests pass
- [x] `mvn clean verify` passes

**Related behaviors:** Request without token returns 401, Request with valid token succeeds, Request with expired token returns 401, All CRUD endpoints are protected, CSV export endpoint is protected, Image endpoints are protected, Brevo sync endpoint is protected, Comment author set from authenticated user, Different users create comments with their own names

---

## Step 7: Update project documentation

- [x] Update `project-features.md` with backend auth feature
- [x] Update `project-tech.md` with new dependencies
- [x] Update `project-structure.md` with new config files
- [x] Update `project-architecture.md` with backend auth flow

**Acceptance criteria:**
- [x] Documentation reflects backend OIDC auth implementation

**Related behaviors:** N/A (documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Request with valid token succeeds | Backend | Step 6 |
| Request without token returns 401 | Backend | Step 6 |
| Request with invalid token returns 401 | Backend | Step 6 |
| Request with expired token returns 401 | Backend | Step 6 |
| All CRUD endpoints are protected | Backend | Step 6 |
| CSV export endpoint is protected | Backend | Step 6 |
| Image endpoints are protected | Backend | Step 6 |
| Brevo sync endpoint is protected | Backend | Step 6 |
| Health endpoint accessible without token | Backend | Step 6 |
| Swagger UI accessible without token | Backend | Step 6 |
| OpenAPI docs accessible without token | Backend | Step 6 |
| Swagger UI shows Authorize button | Backend | Step 3 |
| Protected endpoints callable after authorization in Swagger | Backend | Step 3 |
| UserService returns user from JWT claims | Backend | Step 6 |
| UserService handles missing name claim | Backend | Step 6 |
| UserService handles missing email claim | Backend | Step 6 |
| UserService throws without authentication | Backend | Step 6 |
| Comment author set from authenticated user | Backend | Step 6 |
| Different users create comments with their own names | Backend | Step 6 |
| Token validated against JWKS | Backend | Step 1 (integration) |
| Token from wrong issuer rejected | Backend | Step 1 (integration) |
| Service tests work with mock security context | Backend | Step 5 |
| Existing tests pass with security enabled | Backend | Step 5 |
| OIDC_ISSUER_URI passed to backend service | Infra | Step 4 |
| Override points to mock server internal URL | Infra | Step 4 |
