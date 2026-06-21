# Behaviors: Base Project Setup

## Health Endpoint

### Returns UP status when backend is running

- **Given** the Spring Boot backend is running
- **When** a GET request is made to `/api/health`
- **Then** the response status is 200
- **And** the response body is `{"status": "UP"}`
- **And** the content type is `application/json`

### Swagger UI is accessible

- **Given** the Spring Boot backend is running
- **When** a GET request is made to `/swagger-ui.html`
- **Then** the Swagger UI page is served successfully
- **And** the health endpoint is listed in the API documentation

## Frontend Health Display

### Shows green indicator when backend is healthy

- **Given** the backend is running and `/api/health` returns 200
- **When** the user loads the frontend main page
- **Then** a green status indicator is displayed
- **And** the text "Backend is running" (or i18n equivalent) is shown

### Shows red indicator when backend is unavailable

- **Given** the backend is not running or unreachable
- **When** the user loads the frontend main page
- **Then** a red status indicator is displayed
- **And** the text "Backend is unavailable" (or i18n equivalent) is shown

### Health status is checked once on page load

- **Given** the frontend main page is loaded
- **When** the health check completes (success or failure)
- **Then** no further health check requests are made automatically
- **And** the displayed status remains static until the page is reloaded

## API Proxying

### Frontend proxies API requests to backend

- **Given** the frontend is running with `BACKEND_URL` configured
- **When** the frontend server-side code fetches `/api/health`
- **Then** the request is proxied to `${BACKEND_URL}/api/health` via Next.js rewrites
- **And** the browser never makes a direct request to the backend

### Frontend handles missing BACKEND_URL gracefully

- **Given** the `BACKEND_URL` environment variable is not set
- **When** the frontend attempts to proxy an API request
- **Then** the request fails
- **And** the frontend displays the red (unavailable) indicator

## Docker Compose

### All services start successfully

- **Given** a valid `.env` file exists (copied from `.env.example`)
- **When** `docker compose up --build` is executed
- **Then** the PostgreSQL database starts and accepts connections
- **And** the Spring Boot backend starts and connects to the database
- **And** the Next.js frontend starts and can reach the backend
- **And** the frontend is accessible at `http://localhost:${FRONTEND_PORT:-4001}`
- **And** the backend is accessible at `http://localhost:${BACKEND_PORT:-9081}`

### Backend connects to PostgreSQL on startup

- **Given** Docker Compose is running with the `db` service
- **When** the backend service starts
- **Then** it connects to PostgreSQL using the configured credentials
- **And** Flyway runs (even if no migrations exist yet)
- **And** the application starts successfully

### Services stop cleanly

- **Given** Docker Compose services are running
- **When** `docker compose down` is executed
- **Then** all containers stop without errors

### Volume cleanup removes database data

- **Given** Docker Compose services have been running with data in PostgreSQL
- **When** `docker compose down -v` is executed
- **Then** all containers stop
- **And** the PostgreSQL data volume is removed

## Standalone Development

### Backend runs standalone without Docker

- **Given** Java 21 is installed and PostgreSQL is accessible
- **When** `./mvnw spring-boot:run` is executed in the `backend/` directory
- **Then** the backend starts on port 8080
- **And** `GET /api/health` returns 200 with `{"status": "UP"}`

### Frontend runs standalone without Docker

- **Given** Node.js 22 and pnpm are installed
- **And** `BACKEND_URL` is set to the backend's address (e.g., `http://localhost:8080`)
- **When** `pnpm dev` is executed in the `frontend/` directory
- **Then** the frontend starts on port 3000
- **And** the main page displays the health status from the backend

## Build & CI

### Backend builds and tests pass

- **Given** the backend source code is present
- **When** `./mvnw clean verify` is executed
- **Then** the build succeeds
- **And** all tests pass
- **And** an SBOM is generated

### Frontend builds successfully

- **Given** the frontend source code is present and dependencies are installed
- **When** `pnpm build` is executed
- **Then** the Next.js production build completes without errors

### Docker images build successfully

- **Given** both backend and frontend source code are present
- **When** `docker compose build` is executed
- **Then** both Docker images are built without errors
