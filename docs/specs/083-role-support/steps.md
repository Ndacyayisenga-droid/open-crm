# Implementation Steps: Role Support

## Step 1: Mock OAuth2 Server — Add roles claim

- [x] Update `mock-oauth2-config.json` to include `"roles": ["CRM-ADMIN"]` in the test user's claims

**Acceptance criteria:**
- [ ] Mock OAuth2 server config contains the `roles` claim
- [ ] Project builds successfully

**Related behaviors:** Mock user has roles in JWT

---

## Step 2: Backend — JWT Authority Mapping

- [x] Add imports for `JwtAuthenticationConverter`, `JwtGrantedAuthoritiesConverter`, `SimpleGrantedAuthority`, `GrantedAuthority`, `ArrayList`, and `Collection` to `SecurityConfig.java`
- [x] Add a `jwtAuthenticationConverter()` bean that extracts the `roles` claim and maps each role to a `ROLE_`-prefixed `SimpleGrantedAuthority`, preserving default scope-based authorities
- [x] Wire the converter into the security filter chain via `.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))`

**Acceptance criteria:**
- [ ] Backend compiles successfully
- [ ] A JWT with `"roles": ["CRM-ADMIN"]` results in `ROLE_CRM-ADMIN` authority
- [ ] A JWT without a `roles` claim has no `ROLE_` authorities
- [ ] Default scope-based authorities are preserved

**Related behaviors:** Roles claim is mapped to Spring Security authorities, Multiple roles are mapped to multiple authorities, Missing roles claim results in no role authorities, Empty roles array results in no role authorities, Default scope-based authorities are preserved

---

## Step 3: Backend — Extend Test Utilities

- [x] Update `TestSecurityUtil.testJwt()` (no-arg) to include `.claim("roles", List.of("CRM-ADMIN"))`
- [x] Update `TestSecurityUtil.testJwt(String, String)` to include `.claim("roles", List.of("CRM-ADMIN"))`
- [x] Add `TestSecurityUtil.testJwt(String, String, List<String>)` overload accepting custom roles
- [x] Update `TestSecurityUtil.setSecurityContext()` (no-arg) to include `"roles"` claim with `["CRM-ADMIN"]`
- [x] Update `TestSecurityUtil.setSecurityContext(String, String)` to include `"roles"` claim with `["CRM-ADMIN"]`
- [x] Add `TestSecurityUtil.setSecurityContext(String, String, List<String>)` overload accepting custom roles

**Acceptance criteria:**
- [ ] Backend compiles successfully
- [ ] All existing tests still pass (roles added as default claim does not break existing test behavior)
- [ ] New overloads accept custom role lists

**Related behaviors:** Test JWT includes roles, Test security context includes roles

---

## Step 4: Frontend — Auth.js Session with Roles

- [x] In `frontend/src/auth.ts`, add `roles` scope to the authorization params: `"openid profile email offline_access roles"`
- [x] Extend the `Session` type declaration to include `roles: string[]`
- [x] In the `jwt` callback, on initial sign-in (`if (account)`), extract `profile.roles` as `string[]` and store in `t.roles`. Default to `[]` if missing or not an array.
- [x] In the `session` callback, pass `t.roles` (defaulting to `[]`) to `session.roles`

**Acceptance criteria:**
- [ ] Frontend compiles successfully
- [ ] Session type includes `roles: string[]`
- [ ] Roles from JWT profile are stored in session
- [ ] Missing or non-array roles default to `[]`

**Related behaviors:** Roles scope is requested on login, Unknown roles scope does not break login, Roles are stored in session from JWT, Multiple roles are stored in session, Missing roles claim defaults to empty array, Roles claim with non-array value defaults to empty array

---

## Step 5: Frontend — i18n Translations

- [x] In `frontend/src/lib/i18n/en.ts`, add `noRoles: "No roles assigned"` to the `user` section
- [x] In `frontend/src/lib/i18n/de.ts`, add `noRoles: "Keine Rollen zugewiesen"` to the `user` section

**Acceptance criteria:**
- [ ] Frontend compiles successfully
- [ ] Both language files have the `noRoles` key

**Related behaviors:** Tooltip is localized

---

## Step 6: Frontend — Sidebar Roles Tooltip

- [x] In `frontend/src/components/sidebar.tsx`, in the `UserSection` component, wrap the user name `<p>` element with a `Tooltip` / `TooltipTrigger` / `TooltipContent`
- [x] Display `session.roles.join(", ")` if `session.roles` is non-empty, otherwise display `t.user.noRoles`

**Acceptance criteria:**
- [ ] Frontend compiles successfully
- [ ] Hovering over user name shows tooltip with roles
- [ ] Empty roles shows localized "No roles assigned"
- [ ] Multiple roles are comma-separated

**Related behaviors:** Tooltip shows roles for user with roles, Tooltip shows multiple roles, Tooltip shows "No roles assigned" for user without roles, Tooltip is localized

---

## Step 7: Backend — Security Config Integration Tests

- [x] Create `SecurityConfigRoleTest.java` in `backend/src/test/java/com/openelements/crm/`
- [x] Test: JWT with `roles: ["CRM-ADMIN"]` → authentication has `ROLE_CRM-ADMIN` authority
- [x] Test: JWT with `roles: ["CRM-ADMIN", "CRM-READONLY"]` → authentication has both `ROLE_CRM-ADMIN` and `ROLE_CRM-READONLY`
- [x] Test: JWT without `roles` claim → no `ROLE_` authorities
- [x] Test: JWT with `roles: []` → no `ROLE_` authorities
- [x] Test: JWT with both scopes and roles → both types of authorities present

**Acceptance criteria:**
- [ ] All tests pass
- [ ] Project builds successfully

**Related behaviors:** Roles claim is mapped to Spring Security authorities, Multiple roles are mapped to multiple authorities, Missing roles claim results in no role authorities, Empty roles array results in no role authorities, Default scope-based authorities are preserved

---

## Step 8: Backend — TestSecurityUtil Tests

- [x] Create `TestSecurityUtilTest.java` in `backend/src/test/java/com/openelements/crm/`
- [x] Test: `setSecurityContext()` produces security context with `roles` claim containing `["CRM-ADMIN"]`
- [x] Test: `setSecurityContext(name, email)` produces security context with `roles` claim containing `["CRM-ADMIN"]`
- [x] Test: custom roles overload works correctly
- [x] Test: empty roles list produces empty roles claim

**Acceptance criteria:**
- [ ] All tests pass
- [ ] Project builds successfully

**Related behaviors:** Test JWT includes roles, Test security context includes roles

---

## Step 9: Frontend — Sidebar Tooltip Tests

- [x] Extend `frontend/src/components/__tests__/sidebar.test.tsx` with new test cases
- [x] Extend `frontend/src/test/test-utils.tsx` to accept session option
- [x] Test: user name element is rendered (tooltip trigger exists)
- [x] Test: when session has roles `["CRM-ADMIN"]`, tooltip content shows "CRM-ADMIN"
- [x] Test: when session has roles `["CRM-ADMIN", "CRM-READONLY"]`, tooltip content shows "CRM-ADMIN, CRM-READONLY"
- [x] Test: when session has empty roles, tooltip content shows the localized "No roles assigned" text
- [x] Test: German locale shows "Keine Rollen zugewiesen" for empty roles
- Note: sidebar.test.tsx has a pre-existing `next/server` module resolution failure in Vitest (unrelated to this spec)

**Acceptance criteria:**
- [ ] All frontend tests pass
- [ ] Project builds successfully

**Related behaviors:** Tooltip shows roles for user with roles, Tooltip shows multiple roles, Tooltip shows "No roles assigned" for user without roles, Tooltip is localized

---

## Step 10: Update Project Documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — add role support feature
- [x] Update `.claude/conventions/project-specific/project-tech.md` — note JWT authority mapping
- [x] Update `.claude/conventions/project-specific/project-architecture.md` — document role flow from OIDC to frontend/backend
- [x] No changes needed to `project-structure.md` — no new directories or modules added
- [x] No changes needed to `README.md` — no user-facing setup changes
