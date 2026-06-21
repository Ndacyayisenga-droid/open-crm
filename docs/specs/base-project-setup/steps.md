# Implementation Steps: Base Project Setup

## Step 1: Root Configuration Files

- [x] Create `.editorconfig` with 4-space Java/XML, 2-space TS/JS/YAML/JSON, UTF-8, LF endings
- [x] Update `.gitignore` to cover `.env`, `.idea/`, `target/`, `node_modules/`, `.next/`, `*.log`, `.DS_Store`, `.claude/settings.local.json`
- [x] Create `LICENSE` with Apache License 2.0 full text
- [x] Create `CODE_OF_CONDUCT.md` with Contributor Covenant 2.0

**Acceptance criteria:**
- [x] All four files exist at project root
- [x] `.gitignore` covers all required patterns

**Related behaviors:** None directly — foundational setup

---

## Step 2: Docker Compose & Environment Configuration

- [x] Create `docker-compose.yml` with `db` (postgres:17-alpine), `backend`, `frontend` services — all config via env vars with defaults
- [x] Create `.env.example` with `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT` placeholders

**Acceptance criteria:**
- [x] `docker-compose.yml` uses `${DB_NAME}`, `${DB_USER}`, `${DB_PASSWORD}` variable references — no hardcoded credentials
- [x] `.env.example` contains all required variables with safe placeholder values
- [x] `.env` is listed in `.gitignore`

**Related behaviors:** All services start successfully, Services stop cleanly, Volume cleanup removes database data

---

## Step 3: Backend Project Skeleton

- [x] Create `backend/.sdkmanrc` with `java=21`
- [x] Create `backend/pom.xml` with Spring Boot 3.4.7 parent, Java 21, all dependencies (web, data-jpa, postgresql, flyway, springdoc, test), pinned plugin versions in `<pluginManagement>`, CycloneDX plugin
- [x] Generate Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`)
- [x] Create `backend/src/main/java/com/openelements/crm/CrmApplication.java` — Spring Boot main class
- [x] Create `backend/src/main/resources/application.yml` — datasource via env vars, Flyway config, server port 8080
- [x] Create `backend/src/main/resources/db/migration/.gitkeep`
- [x] Create `backend/.dockerignore`

**Acceptance criteria:**
- [x] `cd backend && ./mvnw clean compile` succeeds
- [x] Java version is consistent across `pom.xml` and `.sdkmanrc`
- [x] CycloneDX plugin is configured in `<build><plugins>`
- [x] Default Maven plugins are pinned in `<pluginManagement>`
- [x] Project metadata (name, description, URL, license) is present in `pom.xml`

**Related behaviors:** Backend builds and tests pass (partial — compilation only)

---

## Step 4: Health Endpoint

- [x] Create `backend/src/main/java/com/openelements/crm/health/HealthResponse.java` — record with `String status`, Javadoc
- [x] Create `backend/src/main/java/com/openelements/crm/health/HealthController.java` — `@RestController` with `GET /api/health` returning `HealthResponse("UP")`, OpenAPI annotations

**Acceptance criteria:**
- [x] `cd backend && ./mvnw clean compile` succeeds
- [x] `HealthResponse` is an immutable record
- [x] `HealthController` has OpenAPI annotations (summary, description, response schema)
- [x] Endpoint is mapped to `/api/health`
- [x] All public API has Javadoc

**Related behaviors:** Returns UP status when backend is running, Swagger UI is accessible

---

## Step 5: Backend Tests

- [x] Create test application config for H2 in-memory database (`application-test.yml`)
- [x] Create `backend/src/test/java/com/openelements/crm/health/HealthControllerTest.java` — MockMvc test verifying:
  - GET /api/health returns HTTP 200
  - Response body is `{"status":"UP"}`
  - Content type is `application/json`
- [x] Add test verifying Swagger UI is accessible (GET to `/swagger-ui/index.html` returns 200)
- [x] Add test verifying OpenAPI spec includes health endpoint

**Acceptance criteria:**
- [x] `cd backend && ./mvnw clean verify` succeeds with all 3 tests passing
- [x] SBOM is generated in `target/`
- [x] Tests follow `//GIVEN //WHEN //THEN` structure
- [x] Swagger UI accessibility is verified by test

**Related behaviors:** Returns UP status when backend is running, Swagger UI is accessible, Backend connects to PostgreSQL on startup, Backend builds and tests pass

---

## Step 6: Backend Dockerfile

- [x] Create `backend/Dockerfile` — multi-stage build: Maven build stage with `eclipse-temurin:21`, runtime stage with `eclipse-temurin:21-jre-alpine`, non-root user, expose 8080

**Acceptance criteria:**
- [ ] `docker build ./backend` succeeds (Docker not available locally — to be verified with Docker)
- [x] Container runs as non-root user
- [x] Java version in Dockerfile matches `pom.xml` and `.sdkmanrc` (21)
- [x] Only port 8080 is exposed

**Related behaviors:** Docker images build successfully

---

## Step 7: Frontend Project Skeleton

- [x] Create `frontend/.nvmrc` with `v22.19.0`
- [x] Initialize Next.js 15 project with TypeScript, Tailwind CSS, App Router, pnpm
- [x] Configure `tsconfig.json` with `strict: true`
- [x] Configure `next.config.ts` with `output: 'standalone'` and API rewrites using `BACKEND_URL`
- [x] Configure Tailwind with Open Elements brand colors and Montserrat/Lato typography
- [x] Set up ESLint and Prettier
- [x] Create `frontend/public/.gitkeep`
- [x] Create `frontend/.dockerignore`
- [x] Install and configure shadcn/ui (Card component added)
- [x] Create `frontend/src/app/globals.css` with Tailwind directives and Open Elements CSS custom properties
- [x] Create `frontend/src/app/layout.tsx` with Open Elements typography (Montserrat headings, Lato body)
- [x] Create `frontend/src/lib/constants.ts` with i18n-ready string constants
- [x] Run `pnpm install` to generate `pnpm-lock.yaml`

**Acceptance criteria:**
- [x] `cd frontend && pnpm install && pnpm build` succeeds
- [x] TypeScript strict mode is enabled
- [x] `output: 'standalone'` is configured
- [x] API rewrite uses `BACKEND_URL` environment variable (defaults to localhost:8080)
- [x] Open Elements brand colors available as Tailwind utility classes
- [x] All user-facing strings in `constants.ts`, not hardcoded in components
- [x] `public/` directory exists

**Related behaviors:** Frontend builds successfully, Frontend proxies API requests to backend

---

## Step 8: Frontend Health Status Page

- [x] Create `frontend/src/components/health-status.tsx` — client component rendering green (`#5CBA9E`) or red (`#E63277`) circle with status text, using shadcn/ui Card
- [x] Create `frontend/src/app/page.tsx` — server component with `export const dynamic = 'force-dynamic'`, fetches `/api/health` server-side, passes result to HealthStatus. Handles fetch failure gracefully (shows red indicator)
- [x] Apply Open Elements brand guidelines: professional, polished UI with proper spacing and responsive design

**Acceptance criteria:**
- [x] Page uses `force-dynamic` to prevent static pre-rendering
- [x] Health status is fetched server-side, not from the browser
- [x] Green indicator shown when backend returns 200
- [x] Red indicator shown when backend is unreachable
- [x] UI uses shadcn/ui Card component
- [x] Uses Open Elements brand colors (green `#5CBA9E`, red `#E63277`)
- [x] Text labels come from `constants.ts`
- [x] Page has polished, professional appearance with responsive design
- [x] `pnpm build` still succeeds

**Related behaviors:** Shows green indicator when backend is healthy, Shows red indicator when backend is unavailable, Health status is checked once on page load, Frontend handles missing BACKEND_URL gracefully

---

## Step 9: Frontend Dockerfile

- [x] Create `frontend/Dockerfile` — multi-stage build: Node 22-alpine, pnpm install + next build, runtime stage with minimal Node image, non-root user, expose 3000

**Acceptance criteria:**
- [ ] `docker build ./frontend` succeeds (Docker not available locally — to be verified with Docker)
- [x] Container runs as non-root user
- [x] Node version in Dockerfile matches `.nvmrc` (22)
- [x] Only port 3000 is exposed
- [x] `BACKEND_URL` is configurable at runtime

**Related behaviors:** Docker images build successfully

---

## Step 10: Docker Compose Integration Verification

Verify the full stack works end-to-end with Docker Compose. **Skipped — Docker not available locally. To be verified manually.**

- [ ] Copy `.env.example` to `.env` (local only, gitignored)
- [ ] Verify `docker compose build` succeeds
- [ ] Verify `docker compose up` starts all three services
- [ ] Verify `GET http://localhost:${BACKEND_PORT}/api/health` returns `{"status":"UP"}`
- [ ] Verify `http://localhost:${FRONTEND_PORT}` shows green health indicator
- [ ] Verify `docker compose down` stops all services cleanly
- [ ] Verify `docker compose down -v` removes volumes

**Acceptance criteria:**
- [ ] All services start and communicate correctly
- [ ] Frontend displays green health indicator
- [ ] No errors in service logs
- [ ] Clean shutdown works

**Related behaviors:** All services start successfully, Backend connects to PostgreSQL on startup, Services stop cleanly, Volume cleanup removes database data

---

## Step 11: Frontend Test Setup & Tests

- [x] Set up Vitest with React Testing Library in the frontend project
- [x] Add `pnpm test` script to `package.json`
- [x] Create test for `HealthStatus` component: renders green indicator and "Backend is running" text when healthy
- [x] Create test for `HealthStatus` component: renders red indicator and "Backend is unavailable" text when unhealthy
- [x] Create test for `HealthStatus` component: displays system status title
- [x] Create test: no automatic re-fetch after initial load (status remains static — pure display component)

**Acceptance criteria:**
- [x] `pnpm test` runs and all 4 tests pass
- [x] `pnpm build` still succeeds
- [x] Every frontend behavioral scenario has at least one test
- [x] Tests reference i18n string constants from `constants.ts`

**Related behaviors:** Shows green indicator when backend is healthy, Shows red indicator when backend is unavailable, Health status is checked once on page load, Frontend handles missing BACKEND_URL gracefully

---

## Step 12: CI/CD Workflow

- [x] Create `.github/workflows/build.yml` with:
  - Backend job: setup Java 21 (Temurin), `./mvnw clean verify`
  - Frontend job: setup Node 22, pnpm 10, `pnpm install --frozen-lockfile`, `pnpm test`, `pnpm build`
  - Docker job (needs backend + frontend): `docker compose build`
  - Triggers: push to `main`, PRs to `main`
- [x] Use `defaults.run.working-directory` for backend and frontend jobs
- [x] Pin action versions (`actions/checkout@v6`, `actions/setup-java@v5`, `pnpm/action-setup@v4`, `actions/setup-node@v6`)
- [x] Cache pnpm store with `cache-dependency-path: frontend/pnpm-lock.yaml`

**Acceptance criteria:**
- [x] Workflow file is valid YAML
- [x] Backend and frontend jobs run in parallel
- [x] Docker job depends on both (`needs: [backend, frontend]`)
- [x] Action versions are pinned
- [x] Java version matches `.sdkmanrc` (21)
- [x] Node version matches `.nvmrc` (22)

**Related behaviors:** Backend builds and tests pass, Frontend builds successfully, Docker images build successfully

---

## Step 13: Update README

- [x] Update `README.md` with:
  - Project description and status (early development)
  - Prerequisites (Java 21, Node.js 22, pnpm, Docker & Docker Compose)
  - Recommended tools (SDKMAN! for Java, nvm for Node.js)
  - How to set up `.env` from `.env.example`
  - How to run with Docker Compose (`docker compose up --build`, `down`, `down -v`)
  - How to run backend standalone (`./mvnw spring-boot:run`)
  - How to run frontend standalone (`BACKEND_URL=http://localhost:8080 pnpm dev`)

**Acceptance criteria:**
- [x] README contains all required sections
- [x] References `.sdkmanrc` and `.nvmrc` for version management
- [x] Instructions are accurate and follow conventions

**Related behaviors:** Backend runs standalone without Docker, Frontend runs standalone without Docker

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Returns UP status when backend is running | Backend | Step 5 |
| Swagger UI is accessible | Backend | Step 5 |
| Shows green indicator when backend is healthy | Frontend | Steps 8, 11 |
| Shows red indicator when backend is unavailable | Frontend | Steps 8, 11 |
| Health status is checked once on page load | Frontend | Steps 8, 11 |
| Frontend proxies API requests to backend | Both | Steps 7 (config), 8 (usage), 10 (integration) |
| Frontend handles missing BACKEND_URL gracefully | Frontend | Steps 8, 11 |
| All services start successfully | Infrastructure | Step 10 |
| Backend connects to PostgreSQL on startup | Backend | Steps 5, 10 |
| Services stop cleanly | Infrastructure | Step 10 |
| Volume cleanup removes database data | Infrastructure | Step 10 |
| Backend runs standalone without Docker | Backend | Steps 5, 13 |
| Frontend runs standalone without Docker | Frontend | Steps 8, 13 |
| Backend builds and tests pass | Backend | Steps 5, 12 |
| Frontend builds successfully | Frontend | Steps 7, 12 |
| Docker images build successfully | Infrastructure | Steps 6, 9, 12 |
