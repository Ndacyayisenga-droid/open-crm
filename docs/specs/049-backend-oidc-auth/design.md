# Design: Backend OIDC Auth

## GitHub Issue

—

## Summary

Add JWT token validation to the Spring Boot backend using Spring Security OAuth2 Resource Server. All API endpoints require a valid Bearer token except the health endpoint and Swagger UI. The `UserService` is updated to extract user information (name, email) from the JWT token in the `SecurityContext` instead of returning hardcoded values. Existing tests are updated to use Spring Security Test mock tokens.

## Goals

- Validate JWT tokens on all API requests (except explicitly public endpoints)
- Extract authenticated user identity from JWT claims
- Keep health endpoint and Swagger UI publicly accessible
- Enable Swagger UI's "Authorize" button for testing protected endpoints
- Maintain test coverage with mock tokens

## Non-goals

- Role-based access control / permissions (all authenticated users have equal access)
- CORS configuration (frontend proxies server-side, Swagger UI on same origin)
- User storage in the database
- Frontend changes (handled by Spec 048)

## Technical Approach

### Spring Security Dependency

Add `spring-boot-starter-oauth2-resource-server` and `spring-security-test` to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Security Configuration

Create a `SecurityConfig` class with a `SecurityFilterChain` bean:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
```

**Rationale:** The backend is a pure Resource Server — it receives and validates tokens but does not issue them. `spring-boot-starter-oauth2-resource-server` with JWT configuration is the standard Spring approach for this. CSRF is disabled because the API is stateless and uses Bearer token authentication.

### Application Configuration

Add the OIDC issuer URI to `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI}
```

Spring Security auto-discovers the JWKS endpoint via the OIDC discovery document at `${OIDC_ISSUER_URI}/.well-known/openid-configuration` and validates token signatures against it.

### Swagger UI — Authorize Button

Configure SpringDoc to show an "Authorize" button that supports OIDC authentication. Add OpenAPI security scheme configuration:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("oidc"))
            .components(new Components()
                .addSecuritySchemes("oidc", new SecurityScheme()
                    .type(SecurityScheme.Type.OPENIDCONNECT)
                    .openIdConnectUrl(issuerUri + "/.well-known/openid-configuration")
                )
            );
    }
}
```

This adds an "Authorize" button to Swagger UI where users can initiate an OIDC login or paste a Bearer token to test protected endpoints.

### UserService Update

Replace the hardcoded dummy user with JWT claim extraction from the `SecurityContext`:

```java
@Service
public class UserService {

    public UserInfo getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        String name = jwt.getClaimAsString("name");
        String email = jwt.getClaimAsString("email");
        return new UserInfo(
            name != null ? name : "Unknown",
            email != null ? email : ""
        );
    }
}
```

The `UserInfo` record remains unchanged (name, email — no picture field needed in the backend).

The `CommentService` continues to call `userService.getCurrentUser().name()` for the comment author — no changes needed there.

### Docker Compose

Add `OIDC_ISSUER_URI` to the backend service's environment in `docker-compose.yml`:

```yaml
backend:
  environment:
    OIDC_ISSUER_URI: ${OIDC_ISSUER_URI}
```

In `docker-compose.override.yml`, the value resolves to `http://mock-oauth2:8080/default` (internal Docker hostname of the mock server) for local development. In Coolify, it points to the real Authentik instance.

### Test Updates

Existing tests use `@SpringBootTest` and will fail with 401 once Security is active. Update tests to use Spring Security Test utilities:

**For service-level tests** (e.g., `CommentServiceTest`): These don't go through the HTTP layer, but `UserService.getCurrentUser()` now reads from `SecurityContext`. Tests need to set up a mock security context:

```java
@BeforeEach
void setUp() {
    // Set up mock JWT in SecurityContext
    Jwt jwt = Jwt.withTokenValue("mock-token")
        .header("alg", "RS256")
        .claim("name", "Test User")
        .claim("email", "test@example.com")
        .build();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new JwtAuthenticationToken(jwt));
    SecurityContextHolder.setContext(context);
}
```

**For controller-level tests** (if using `@WebMvcTest` or `MockMvc`): Use `SecurityMockMvcRequestPostProcessors.jwt()`:

```java
mockMvc.perform(get("/api/companies")
    .with(jwt().jwt(builder -> builder
        .claim("name", "Test User")
        .claim("email", "test@example.com"))))
    .andExpect(status().isOk());
```

### Files Affected

**Backend (new):**
- `SecurityConfig.java` — SecurityFilterChain with public/protected route configuration
- `OpenApiConfig.java` — Swagger UI OIDC security scheme

**Backend (modified):**
- `pom.xml` — add `spring-boot-starter-oauth2-resource-server`, `spring-security-test`
- `application.yml` — add `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `UserService.java` — replace hardcoded user with JWT claim extraction
- All test files — add mock JWT/security context setup

**Infrastructure (modified):**
- `docker-compose.yml` — add `OIDC_ISSUER_URI` to backend service environment
- `docker-compose.override.yml` — override `OIDC_ISSUER_URI` for backend to mock server internal URL

## Dependencies

- **Spec 047 (OIDC Infrastructure)** — mock-oauth2-server and env vars must be in place
- **Spec 048 (Frontend OIDC Auth)** — frontend sends `Authorization: Bearer` header via proxy
- **spring-boot-starter-oauth2-resource-server** — new Maven dependency
- **spring-security-test** — new Maven test dependency

## Security Considerations

- JWT signature is validated against the provider's JWKS keys (fetched via discovery)
- Token issuer is validated against the configured `issuer-uri`
- Token expiry is validated automatically by Spring Security
- CSRF is disabled (stateless API with Bearer token auth)
- No secrets are stored in the backend — only the issuer URI is needed for validation

## Open Questions

None — all details resolved during design discussion.
