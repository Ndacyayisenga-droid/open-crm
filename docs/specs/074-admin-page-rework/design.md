# Design: Admin Page Rework

## Summary

The admin page currently has inconsistent styling across its panels (Health Status and Brevo Import) and lacks a way to easily obtain the JWT access token for Swagger UI. This spec adds a Bearer Token panel and unifies the layout and styling of all admin cards.

## Goals

- Add a Bearer Token card that displays the current user's access token for use in Swagger UI
- Unify styling across all admin page cards (Health, Brevo Settings, Brevo Import, Bearer Token)
- Remove duplicate headings and inconsistent widths

## Non-goals

- Changing any backend endpoints
- Adding new API endpoints for token retrieval (the token is already in the Auth.js session)
- Token refresh functionality from the admin page

## Technical Approach

### Bearer Token Card

A new card on the admin page that displays the JWT access token from the Auth.js session.

**Content:**
- `CardTitle`: "Bearer Token" (or i18n equivalent)
- Token display: masked by default (e.g., `••••••••••••••••`), revealed via a "Show"/"Hide" toggle button
- Copy button: copies the raw token to clipboard with checkmark feedback (same pattern as `detail-field.tsx` copy action)
- Remaining validity: displays how long until the token expires (e.g., "Valid for 4:32 min"), derived from the JWT's `exp` claim. The session already stores `expiresAt` in the JWT callback.

**Token source:** The access token is available in the Auth.js session via `session.accessToken`. The page uses `useSession()` to access it. The `expiresAt` timestamp needs to be exposed in the session callback — currently it is stored in the JWT token but not forwarded to the session object.

**Session change:** Add `expiresAt` to the session in `auth.ts`:

```typescript
// In the session callback, add:
session.expiresAt = t.expiresAt as number | undefined;
```

And extend the Session type:

```typescript
declare module "next-auth" {
  interface Session {
    accessToken?: string;
    idToken?: string;
    expiresAt?: number;
  }
}
```

**Remaining validity display:** A client-side countdown or static display showing the time until `expiresAt`. A simple approach: compute `expiresAt * 1000 - Date.now()` on render and display as "X min" or "Expired". Update every 10 seconds via `setInterval` to keep it roughly current.

### Unified Card Layout

**Current problems:**
- `HealthStatus` uses `max-w-md`, `border-oe-gray-light`, `shadow-md`
- `BrevoSync` uses `max-w-2xl` and has its own `<h1>` heading
- Cards have different widths and border styles

**Fix:**
- All cards use full content width (no `max-w-*` constraints)
- All cards use the same `Card` component with consistent `CardHeader` / `CardTitle` / `CardContent` styling
- Remove the `<h1>` from inside `BrevoSync` — the admin page has its own `<h1>` ("Admin")
- The two Brevo cards get "Brevo" in their `CardTitle`: "Brevo Settings" and "Brevo Import"
- `HealthStatus` removes its own `max-w-md` and `shadow-md`
- The admin page renders all cards in a `space-y-6` vertical stack at full width

### Card Order

1. Server Health
2. Bearer Token
3. Brevo Settings
4. Brevo Import

### Internationalization

New i18n keys in `de.ts` and `en.ts`:

```
admin.token.title: "Bearer Token" / "Bearer Token"
admin.token.show: "Anzeigen" / "Show"
admin.token.hide: "Verbergen" / "Hide"
admin.token.copy: "Kopieren" / "Copy"
admin.token.copied: "Kopiert" / "Copied"
admin.token.validFor: "Gültig für" / "Valid for"
admin.token.expired: "Abgelaufen" / "Expired"
admin.token.noToken: "Kein Token verfügbar" / "No token available"
```

Update existing Brevo i18n keys for card titles:
```
brevo.settings.title: "Brevo-Einstellungen" / "Brevo Settings"
brevo.sync.title: "Brevo-Import" / "Brevo Import"
```

### Brand Guidelines

- Cards: default shadcn/ui `Card` with `border-oe-gray-light`
- CardTitle: `font-heading text-lg text-oe-dark` (consistent across all cards)
- Buttons: OE Green (`bg-oe-green hover:bg-oe-green-dark text-white`) for primary actions
- Error/expired text: OE Red (`text-oe-red`)

## Files Affected

- `frontend/src/app/(app)/admin/page.tsx` — rework layout, add Bearer Token card
- `frontend/src/components/health-status.tsx` — remove `max-w-md` and `shadow-md`
- `frontend/src/components/brevo-sync.tsx` — remove `<h1>`, remove `max-w-2xl`, update card titles
- `frontend/src/auth.ts` — expose `expiresAt` in session, extend Session type
- `frontend/src/lib/i18n/de.ts` — add token i18n keys, update Brevo titles
- `frontend/src/lib/i18n/en.ts` — add token i18n keys, update Brevo titles

## Security Considerations

- The access token is already available in the browser via the Auth.js session cookie (it is an encrypted JWT cookie, but the session API exposes it client-side). Displaying it in the UI does not introduce a new attack surface — it is already accessible via `useSession()`.
- The token is masked by default to prevent shoulder-surfing.
- No token is stored in localStorage or any persistent client-side storage beyond the session cookie.
