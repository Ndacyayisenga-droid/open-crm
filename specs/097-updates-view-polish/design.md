# Design: Updates view — visual polish & default landing route

## GitHub Issue

— (no issue yet; can be drafted after spec is finalized).

## Summary

Iterate on the Updates view introduced in spec 096. Each row gets a richer,
more scannable layout: a leading company logo or contact photo, the affected
entity name in bold, and the actor's avatar next to their name. The Updates
view also becomes the application's default landing route after login,
replacing the existing `/ → /companies` redirect.

This is a polish spec on top of an already-done feature (spec 096 is frozen).
It introduces one small backend change (two extra boolean fields on
`UpdateEntryDto`) and a frontend layout rewrite of the row component.

## Goals

- Make individual rows scannable at a glance: the user should recognize the
  affected company/person from the logo/photo alone, without reading the text.
- Surface the actor visually (avatar next to name), consistent with the
  `admin/users` view.
- Replace the current `/ → /companies` index redirect with `/ → /updates` so
  the activity feed becomes the default landing page after login.
- Keep the change consistent with brand and accessibility guidelines.

## Non-goals

- No new event types, no change to the dedupe/filter logic in `UpdatesService`.
- No paginate-beyond-N, no real-time push (still manual reload).
- No color change to the timestamp. The originally requested green coloring was
  dropped — see "Rationale: rejected ideas".
- No relabel of the page-size selector. "Pro Seite" / "Per page" stays.
- No empty-state CTA. A fresh, never-edited system still shows the existing
  empty state (`Bell` icon + "Noch keine Änderungen").

## Rationale: rejected ideas

These were considered during the grill session and explicitly rejected:

- **Green timestamp (`oe-green` `#5cba9e` on white).** Kontrast ~2.4:1, weit
  unter WCAG AA (4.5:1). Auch `oe-green-dark` (~3.7:1) verfehlt AA für
  normalen Text. Rejected to keep the view accessibility-compliant.
- **Combobox label "Update Count".** Würde Inkonsistenz mit allen anderen
  paginierten Listen erzeugen, die "Per page" / "Pro Seite" verwenden.

## Technical approach

### Backend — extend `UpdateEntryDto`

Two new boolean fields signal whether the affected entity has a renderable
image:

```java
public record UpdateEntryDto(
    UUID id,
    UpdateType type,
    UUID entityId,
    String entityName,
    boolean entityHasLogo,    // NEW — true iff entityId refers to a company with a logo
    boolean entityHasPhoto,   // NEW — true iff entityId refers to a contact with a photo
    UserDto user,
    Instant createdAt
) {}
```

Semantics:

- For `COMPANY_*` and `COMPANY_COMMENT_*` types: `entityHasLogo` reflects the
  current `CompanyEntity.getLogo() != null`. `entityHasPhoto` is always `false`.
- For `CONTACT_*` and `CONTACT_COMMENT_*` types: `entityHasPhoto` reflects the
  current `ContactEntity.getPhoto() != null`. `entityHasLogo` is always `false`.
- For `COMPANY_DELETED` / `CONTACT_DELETED`: both flags are `false`
  (`entityId` is null; no image possible).
- For `*_CREATED` / `*_UPDATED` / `*_COMMENT_*` where the entity can no
  longer be resolved (race condition): both flags are `false`.

Rationale: speculative `<img src=…>` rendering with `onError` fallback was
rejected because it produces 404 network noise per missing image and a brief
broken-image flash. A single backend flag is cheap (the entity is already
loaded in `resolveNames` for the name) and gives the frontend a clean signal.

Implementation note: `UpdatesService.resolveNames` already loads the
`CompanyEntity` / `ContactEntity` to compute `entityName`. The flags are
derived from the same entity instance — no additional repository calls.

### Backend — no further changes

- Endpoint contract is unchanged (`GET /api/updates?size=N`).
- Sort, filter, dedupe, name resolution: unchanged.
- Audit emission: unchanged.

### Frontend — default landing route

`frontend/src/app/(app)/page.tsx` currently:

```tsx
import { redirect } from "next/navigation";
export default function Home() {
  redirect("/companies");
}
```

Changes to:

```tsx
import { redirect } from "next/navigation";
export default function Home() {
  redirect("/updates");
}
```

A frische Empty-State auf `/updates` ist akzeptables Landing-Verhalten;
weitergeleitete User navigieren über die Sidebar zu Firmen/Kontakten/etc.

### Frontend — row layout

The list (`<ul>`) is kept; each `<li>` row is restructured into three
flex regions:

```
┌────────┬─────────────────────────────────────────────┬────────────────────────────────┐
│  LOGO  │  Aktionstext mit fettem Entity-Namen        │  [Avatar]  Autorname · Datum   │
│  32×32 │  z. B. „Firma <b>Name GmbH</b> wurde …"     │   20×20                        │
└────────┴─────────────────────────────────────────────┴────────────────────────────────┘
```

Tailwind sketch:

```tsx
<li className="flex items-center gap-3 px-4 py-3" data-testid="updates-row">
  {/* Left: logo / photo / trash icon — fixed 32×32 slot */}
  <EntityImage entry={entry} />

  {/* Middle: message text — flex-grow */}
  <div className="min-w-0 flex-1">
    <span className="text-oe-dark">
      {before}
      {link ? (
        <Link href={link.href} className="font-bold text-oe-blue underline-offset-2 hover:underline">
          {link.text}
        </Link>
      ) : (
        <span className="font-bold">{plainName}</span>
      )}
      {after}
    </span>
  </div>

  {/* Right: avatar + author + timestamp */}
  <div className="flex shrink-0 items-center gap-2 text-sm text-oe-gray">
    <UserAvatar user={entry.user} />
    <span>{entry.user.name}</span>
    <span aria-hidden="true">·</span>
    <span>{new Date(entry.createdAt).toLocaleString()}</span>
  </div>
</li>
```

Components:

- **`EntityImage`** (new local component in `updates-client.tsx`):
  - If `type` is `COMPANY_DELETED` or `CONTACT_DELETED`:
    render `Trash2` icon (`h-8 w-8 text-oe-gray-mid`, inside the 32×32 slot).
  - Else if `type.startsWith("COMPANY_")` and `entityHasLogo` and `entityId`:
    render `<img src={getCompanyLogoUrl(entityId)} alt="" className="h-8 w-8 object-contain rounded" />`,
    wrapped in a `<Link href={`/companies/${entityId}`} aria-hidden="true" tabIndex={-1}>`.
  - Else if `type.startsWith("COMPANY_")`:
    render `Building2` placeholder (`h-8 w-8 text-oe-gray-mid`).
  - Else if `type.startsWith("CONTACT_")` and `entityHasPhoto` and `entityId`:
    analog with `getContactPhotoUrl(entityId)`, wrapped in `<Link href={`/contacts/${entityId}`} aria-hidden="true" tabIndex={-1}>`.
    Image is rendered with `rounded-full` for contact photos.
  - Else if `type.startsWith("CONTACT_")`:
    render `UserIcon` placeholder.
  - The image is wrapped in a `<Link>` to the same target as the name link.
    The wrapper link receives `aria-hidden="true"` and `tabIndex={-1}` so
    screen readers do not announce it twice and keyboard users do not need
    to tab through it.

- **`UserAvatar`** (new local component in `updates-client.tsx`):
  - If `user.avatarUrl`: `<img src={user.avatarUrl} alt="" className="h-5 w-5 rounded-full object-cover" />`.
  - Else: `<div className="flex h-5 w-5 items-center justify-center rounded-full bg-oe-gray-lightest text-oe-gray"><UserIcon className="h-3 w-3" /></div>`.
  - Pattern is the same as `admin/users` (32px there, 20px here).

### Name bolding & link

The existing `renderTemplate` helper returns `{ before, link, after }`. The
`link.text` slot becomes bold:

- When the entity name is linkable: `font-bold text-oe-blue` on the `<Link>`.
  Previously it was `font-medium text-oe-blue`.
- When the entity name is rendered as plain text (entity could not be
  resolved → `entityName === null` → fallback "—"): wrap in
  `<span className="font-bold">`.
- The `COMPANY_DELETED` / `CONTACT_DELETED` messages have no `{name}`
  placeholder; the whole text remains regular weight.

### Sidebar navigation — "Updates" as first item

The Updates nav entry was already inserted as the first main item in
spec 096. No change required.

### i18n

No new keys. The existing `updates.*` block is reused. The removed
`updates.perPage` rename idea means there is **no** localization change
for the page-size label.

The German `updates.by` template is unchanged ("von {user}"). Author name and
timestamp are still rendered after the message on the right.

## API design

`GET /api/updates` — unchanged endpoint, unchanged query parameters, unchanged
response shape **except**: each `UpdateEntryDto` in `content` now includes the
new `entityHasLogo: boolean` and `entityHasPhoto: boolean` fields.

Example response (delta highlighted):

```json
{
  "content": [
    {
      "id": "9a4f3e7c-...",
      "type": "COMPANY_COMMENT_CREATED",
      "entityId": "1b9d...",
      "entityName": "Open Elements GmbH",
      "entityHasLogo": true,          // NEW
      "entityHasPhoto": false,        // NEW
      "user": { "id": "...", "name": "Hendrik Ebbers", "email": "...", "avatarUrl": null },
      "createdAt": "2026-05-16T08:42:11.123Z"
    },
    {
      "id": "...",
      "type": "COMPANY_DELETED",
      "entityId": null,
      "entityName": null,
      "entityHasLogo": false,         // NEW
      "entityHasPhoto": false,        // NEW
      "user": { "id": "...", "name": "Some Admin", "email": "...", "avatarUrl": null },
      "createdAt": "2026-05-16T08:40:00.000Z"
    }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 2, "totalPages": 1 }
}
```

The change is **additive**. Existing clients that ignore unknown JSON fields
remain compatible.

## Data model

No schema changes. No migration. The new DTO fields are derived from the
already-loaded `CompanyEntity` / `ContactEntity` instances.

## Key flows

### Flow 1 — User opens the application

1. User authenticates via OIDC.
2. Auth.js / Next.js redirects the user to `/`.
3. `frontend/src/app/(app)/page.tsx` server-redirects to `/updates`.
4. `/updates` renders as before, now with enriched rows.

### Flow 2 — Row rendering for a company-update event

1. Frontend receives `{ type: "COMPANY_UPDATED", entityId, entityName, entityHasLogo: true, … }`.
2. `EntityImage`: `entityHasLogo` is true → render `<img src="/api/companies/{id}/logo">` wrapped in a link to `/companies/{id}`, image carries `alt=""`, link carries `aria-hidden="true"`.
3. Message: `"Firma "` + bold-link `"Name GmbH"` (→ `/companies/{id}`) + `" wurde aktualisiert"`.
4. Right side: 20px user avatar (or fallback circle) + author name + middle dot + locale-formatted timestamp.

### Flow 3 — Row rendering for a deleted company

1. Frontend receives `{ type: "COMPANY_DELETED", entityId: null, entityName: null, entityHasLogo: false, … }`.
2. `EntityImage`: type is `COMPANY_DELETED` → render `Trash2` icon, no link wrapper.
3. Message: `"Eine Firma wurde gelöscht"` — no link, no bold.
4. Right side: avatar + author + timestamp as before.

## Dependencies

- **Existing backend:** `UpdatesService`, `CompanyEntity.getLogo()`,
  `ContactEntity.getPhoto()`.
- **Existing frontend helpers:** `getCompanyLogoUrl`, `getContactPhotoUrl`,
  `@open-elements/ui` (Select, Skeleton), `lucide-react` icons (`Trash2`,
  `Building2`, `User as UserIcon`).
- **Spec 096:** the entire Updates view scaffolding; this spec sits on top.
- **No new libraries.**

## Security considerations

- The new DTO fields (`entityHasLogo`, `entityHasPhoto`) reveal only whether
  an image exists on the related entity — information already visible on the
  companies/contacts list pages to the same audience (any authenticated user).
  No new data exposure.
- Image endpoints (`/api/companies/{id}/logo`, `/api/contacts/{id}/photo`) are
  unchanged and reuse the existing OIDC-protected fetchers.

## GDPR considerations

This spec does **not** change what data is logged or who sees it. The
underlying audit visibility model from spec 096 is unchanged. The two new
boolean fields are derived from currently-visible state and reveal no
additional personal information.

The legal-basis question (Betriebsvereinbarung / AV-clause for employee
activity transparency) from 096 remains tracked in `TODO.md`. This spec does
not affect it.

## Brand / UI

Follows the Open Elements brand guidelines:

- Colors: `text-oe-dark`, `text-oe-gray`, `text-oe-gray-mid`, `text-oe-blue`
  for the linked entity name. **No `text-oe-green` for the timestamp** —
  explicitly rejected due to insufficient contrast (see "Rationale: rejected
  ideas").
- Typography: existing `font-heading` for the page title; `font-bold` for the
  entity name in each row.
- Spacing: 12px horizontal padding (`px-4`), 12px vertical (`py-3`), 12px gap
  between regions (`gap-3`).
- Avatar fallback circle uses `bg-oe-gray-lightest`, matching `admin/users`.

## Accessibility

- The leading image and the entity-name text are two separate `<a>` elements
  pointing at the same target. The image link has `aria-hidden="true"` and
  `tabIndex={-1}` so screen readers and keyboard users encounter the target
  exactly once. The image itself carries `alt=""` (decorative — the linked
  text already names the entity).
- The user avatar (`<img>`) carries `alt=""` because the user name is
  rendered as adjacent text.
- The middle-dot separator (`·`) is wrapped in `aria-hidden="true"`.
- The `Trash2` placeholder for deleted entries is purely decorative
  (`aria-hidden="true"`); the localized message already conveys the action.
- Color contrast: all foreground colors used (`text-oe-dark`, `text-oe-gray`,
  `text-oe-blue`) clear WCAG AA on white. The rejected green coloring is
  documented above.

## Layout — responsive behavior

- **Desktop (sm+):** single row, three flex regions as sketched.
- **Mobile (<sm):** the row stacks. The middle region (message) breaks to a
  new line below the leading image; the right region (avatar + author +
  timestamp) breaks below the message. Implementation: replace the always-row
  flex with `flex flex-col sm:flex-row sm:items-center sm:gap-3`, and make
  the right region `sm:shrink-0`. The image stays inline with the message
  block top (no separate row for the image alone on mobile is needed because
  the image is only 32px tall).

## Open questions

- **Should the leading image have a rounded variant for companies?** Current
  proposal: companies use slightly rounded square (`rounded`), contacts use
  full circle (`rounded-full`). This matches the existing pattern in
  `companies-client.tsx` (square) and `admin/users` (circle) and visually
  distinguishes entity types. Confirm during implementation review.
- **Skeleton during loading.** Existing skeleton is `h-12 w-full`. With the
  new layout, skeleton rows should mimic the image-left layout so the layout
  does not jump on load. Suggested: a small 32×32 skeleton + a horizontal bar
  + a right-side skeleton block. Implementation detail, not part of the
  contract.
