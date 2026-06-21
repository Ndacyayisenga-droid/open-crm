# Design: Fix Logout Flow

## Summary

Clicking the logout button in the sidebar does not properly end the user's session in production. The user is redirected to Authentik's `end_session_endpoint`, which shows a plain-text "Logout successful" page. When the user navigates back to the app, they are still logged in. This bug only occurs in production (HTTPS with Authentik) — local development with mock-oauth2-server works correctly.

## Reproduction

1. Deploy the app to production with Authentik as OIDC provider (HTTPS)
2. Log in via Authentik
3. Click "Logout" in the sidebar
4. Observe: Authentik shows plain-text "Logout successful" at `https://auth.open-elements.cloud/application/o/open-crm/end-session/...`
5. Navigate back to the app URL
6. Observe: User is still logged in, all data is visible

**Preconditions:** Production deployment with HTTPS, Authentik as OIDC provider, "Logout URI" field in Authentik OAuth2 Provider config is empty.

**Does NOT reproduce locally:** mock-oauth2-server runs on HTTP, so cookies lack the `Secure` attribute and deletion works without explicit attributes.

## Root Cause Analysis

Two independent issues combine to produce the broken experience:

### Issue 1: Cookie deletion without matching attributes

The logout route (`frontend/src/app/api/logout/route.ts`) deletes Auth.js session cookies like this:

```typescript
response.cookies.delete("authjs.session-token");
response.cookies.delete("__Secure-authjs.session-token");
```

In production under HTTPS, Auth.js sets the session cookie as `__Secure-authjs.session-token` with attributes `Secure; HttpOnly; Path=/; SameSite=lax`. When `cookies.delete()` is called without specifying these attributes, the browser's `Set-Cookie` header does not match the original cookie's attributes, and the browser ignores the deletion.

**Why it works locally:** Under HTTP, Auth.js uses `authjs.session-token` without `Secure` or `__Secure-` prefix. The simpler cookie is deleted successfully even without explicit attributes.

### Issue 2: Missing Logout URI in Authentik

The OIDC specification requires that the `post_logout_redirect_uri` parameter is pre-registered with the identity provider. In Authentik, this is configured in the "Logout URI" field of the OAuth2 Provider settings. This field is currently empty, so Authentik ignores the `post_logout_redirect_uri` query parameter and displays its own "Logout successful" page instead of redirecting back to the app.

## Fix Approach

### Code fix: Cookie deletion with correct attributes

**File:** `frontend/src/app/api/logout/route.ts`

Replace the bare `cookies.delete()` calls with attribute-matched deletions:

```typescript
const cookieOptions = {
  path: "/",
  secure: true,
  httpOnly: true,
  sameSite: "lax" as const,
};

response.cookies.delete({ name: "authjs.session-token", ...cookieOptions });
response.cookies.delete({ name: "__Secure-authjs.session-token", ...cookieOptions });
```

Both cookie names are deleted to cover both HTTP (local dev) and HTTPS (production) scenarios. Under HTTP, the `secure: true` attribute on the `authjs.session-token` delete may be ignored by the browser, but this is harmless — the cookie will either be deleted or was never set with `Secure` in the first place.

**Rationale:** Explicitly matching the cookie attributes ensures the browser recognizes the `Set-Cookie` deletion header as targeting the correct cookie, regardless of the deployment environment.

### Infrastructure fix: Register Logout URI in Authentik

In the Authentik admin panel, navigate to the OAuth2 Provider for Open CRM and set:

- **Logout URI:** `https://crm.playground.open-elements.cloud`

This allows Authentik to honor the `post_logout_redirect_uri` parameter and redirect the user back to the app after the OIDC session is terminated.

**This is a manual configuration change, not a code change.** It should be documented in the README's Authentik setup section.

### Documentation: Add Logout URI to README

Add a note to the Authentik setup instructions in `README.md` mentioning that the "Logout URI" must be set to the app's public URL for logout to redirect back correctly.

## Regression Risk

**Low.** The code change only adds explicit attributes to cookie deletion calls. The logout flow's structure and redirect logic remain unchanged.

- **Local development:** Unaffected. mock-oauth2-server continues to work. The `authjs.session-token` cookie (without `__Secure-` prefix) is still deleted.
- **Production:** Cookie deletion will now work correctly. The Authentik Logout URI configuration is independent of the code change.
- **No database changes, no API changes, no migration needed.**
