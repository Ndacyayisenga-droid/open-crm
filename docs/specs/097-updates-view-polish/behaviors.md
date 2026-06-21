# Behaviors: Updates view — visual polish & default landing route

## Default landing route

### Authenticated user opens the app root

- **Given** a user is authenticated via OIDC
- **When** the user navigates to `/`
- **Then** the server-side handler redirects to `/updates` (no flash of any
  intermediate page)

### Unauthenticated user opens the app root

- **Given** no valid session exists
- **When** the user navigates to `/`
- **Then** the existing auth middleware redirects to the sign-in page (unchanged
  behaviour from spec 048), and after successful sign-in the user lands on
  `/updates`

## Backend — `UpdateEntryDto` shape

### Company event with logo

- **Given** a `CompanyEntity` whose `getLogo()` is non-null
- **When** an audit entry for that company is mapped to an `UpdateEntryDto`
  with `type ∈ {COMPANY_CREATED, COMPANY_UPDATED, COMPANY_COMMENT_CREATED,
  COMPANY_COMMENT_UPDATED, COMPANY_COMMENT_DELETED}`
- **Then** `entityHasLogo` is `true` and `entityHasPhoto` is `false`

### Company event without logo

- **Given** a `CompanyEntity` whose `getLogo()` is null
- **When** the same mapping happens
- **Then** `entityHasLogo` is `false` and `entityHasPhoto` is `false`

### Contact event with photo

- **Given** a `ContactEntity` whose `getPhoto()` is non-null
- **When** an audit entry for that contact is mapped to an `UpdateEntryDto`
  with `type ∈ {CONTACT_CREATED, CONTACT_UPDATED, CONTACT_COMMENT_CREATED,
  CONTACT_COMMENT_UPDATED, CONTACT_COMMENT_DELETED}`
- **Then** `entityHasPhoto` is `true` and `entityHasLogo` is `false`

### Contact event without photo

- **Given** a `ContactEntity` whose `getPhoto()` is null
- **When** the same mapping happens
- **Then** `entityHasPhoto` is `false` and `entityHasLogo` is `false`

### `COMPANY_DELETED` event

- **Given** an audit entry with `entityType=CompanyDto` and `action=DELETE`
- **When** it is mapped to an `UpdateEntryDto`
- **Then** `entityId` is `null`, `entityName` is `null`, `entityHasLogo` is
  `false`, and `entityHasPhoto` is `false`

### `CONTACT_DELETED` event

- **Given** an audit entry with `entityType=ContactDto` and `action=DELETE`
- **When** it is mapped to an `UpdateEntryDto`
- **Then** `entityId` is `null`, `entityName` is `null`, `entityHasLogo` is
  `false`, and `entityHasPhoto` is `false`

### Entity no longer exists (race)

- **Given** an audit entry whose `entityId` does not resolve to a current
  company / contact (e.g. concurrent delete between fetch and resolve)
- **When** it is mapped to an `UpdateEntryDto`
- **Then** `entityName` is `null`, `entityHasLogo` is `false`, and
  `entityHasPhoto` is `false`

### Backwards compatibility

- **Given** a client that does not yet know the new fields
- **When** it deserializes a response from `GET /api/updates`
- **Then** the unknown fields are ignored and the request continues to work
  (additive change; no existing field renamed or removed)

## Frontend — row leading image slot

### Company with logo

- **Given** an `UpdateEntryDto` with `type=COMPANY_UPDATED`,
  `entityId="…"`, `entityHasLogo=true`
- **When** the row renders
- **Then** the leading slot displays an `<img>` with `src` from
  `getCompanyLogoUrl(entityId)`, sized `h-8 w-8`, with `alt=""`, wrapped in
  a `<Link>` to `/companies/{entityId}` whose `aria-hidden` is `"true"` and
  `tabIndex` is `-1`

### Company without logo

- **Given** an `UpdateEntryDto` with `type=COMPANY_UPDATED`,
  `entityHasLogo=false`
- **When** the row renders
- **Then** the leading slot displays a `Building2` icon, sized `h-8 w-8`,
  styled `text-oe-gray-mid`, and is not wrapped in a link

### Contact with photo

- **Given** an `UpdateEntryDto` with `type=CONTACT_UPDATED`,
  `entityId="…"`, `entityHasPhoto=true`
- **When** the row renders
- **Then** the leading slot displays an `<img>` with `src` from
  `getContactPhotoUrl(entityId)`, sized `h-8 w-8`, `rounded-full`, with
  `alt=""`, wrapped in a hidden-from-a11y `<Link>` to `/contacts/{entityId}`

### Contact without photo

- **Given** an `UpdateEntryDto` with `type=CONTACT_UPDATED`,
  `entityHasPhoto=false`
- **When** the row renders
- **Then** the leading slot displays a `UserIcon` placeholder, sized
  `h-8 w-8`, styled `text-oe-gray-mid`, not wrapped in a link

### Company comment event uses parent logo

- **Given** an `UpdateEntryDto` with `type=COMPANY_COMMENT_CREATED`,
  `entityId="company-id"`, `entityHasLogo=true`
- **When** the row renders
- **Then** the leading slot displays the parent company's logo, wrapped in
  a hidden-from-a11y link to `/companies/{entityId}`

### Contact comment event uses parent photo

- **Given** an `UpdateEntryDto` with `type=CONTACT_COMMENT_CREATED`,
  `entityId="contact-id"`, `entityHasPhoto=true`
- **When** the row renders
- **Then** the leading slot displays the parent contact's photo (`rounded-full`),
  wrapped in a hidden-from-a11y link to `/contacts/{entityId}`

### `COMPANY_DELETED` row

- **Given** an `UpdateEntryDto` with `type=COMPANY_DELETED`,
  `entityId=null`, `entityName=null`
- **When** the row renders
- **Then** the leading slot displays a `Trash2` icon (`h-8 w-8`,
  `text-oe-gray-mid`), is not wrapped in a link, and is marked
  `aria-hidden="true"`

### `CONTACT_DELETED` row

- **Given** an `UpdateEntryDto` with `type=CONTACT_DELETED`
- **When** the row renders
- **Then** the leading slot displays a `Trash2` icon, identical to the
  `COMPANY_DELETED` case

### `COMPANY_COMMENT_DELETED` keeps parent logo

- **Given** an `UpdateEntryDto` with `type=COMPANY_COMMENT_DELETED`,
  `entityId="company-id"` (parent still exists), `entityHasLogo=true`
- **When** the row renders
- **Then** the leading slot displays the parent company's logo (not a
  `Trash2` icon — only the comment was deleted, the parent remains)

## Frontend — message and bold name

### Linkable entity name is bold and blue

- **Given** an `UpdateEntryDto` where `entityId` is non-null and
  `entityName` is non-null and the type is not `*_DELETED`
- **When** the row renders
- **Then** the entity-name slot in the message is a `<Link>` styled
  `font-bold text-oe-blue` with `underline-offset-2 hover:underline`,
  pointing at the correct entity detail URL

### Plain entity name is bold but not blue

- **Given** an `UpdateEntryDto` where the entity could not be resolved
  (`entityName=null`) but the message template contains `{name}`
- **When** the row renders
- **Then** the fallback `"—"` is wrapped in `<span className="font-bold">`
  and is **not** a link

### `*_DELETED` message has no bold name

- **Given** an `UpdateEntryDto` with `type=COMPANY_DELETED` (template:
  "Eine Firma wurde gelöscht")
- **When** the row renders
- **Then** the message renders as regular-weight text with no bold name and
  no link

## Frontend — user avatar slot

### Author with avatar URL

- **Given** an `UpdateEntryDto` whose `user.avatarUrl` is a non-null string
- **When** the row renders
- **Then** the right-side region shows an `<img>` with `src=user.avatarUrl`,
  sized `h-5 w-5`, `rounded-full`, `object-cover`, with `alt=""`

### Author without avatar URL

- **Given** an `UpdateEntryDto` whose `user.avatarUrl` is `null`
- **When** the row renders
- **Then** the right-side region shows a `<div>` of size `h-5 w-5`,
  `rounded-full`, `bg-oe-gray-lightest`, containing a centered
  `UserIcon` of size `h-3 w-3`. The element carries
  `data-testid="updates-author-avatar-fallback"` for testing.

## Frontend — page-size selector label

### Label is unchanged

- **Given** the Updates page is rendered
- **When** the page-size combobox area is inspected
- **Then** its label is the existing translation key `updates.perPage`
  (`"Pro Seite"` / `"Per page"`) — **not** renamed to "Update Count"

## Frontend — responsive layout

### Desktop layout

- **Given** the viewport is at the `sm` Tailwind breakpoint or wider
- **When** a row renders
- **Then** the leading image, message, and right-side metadata are arranged
  side-by-side via `flex sm:flex-row sm:items-center sm:gap-3`

### Mobile layout

- **Given** the viewport is below the `sm` Tailwind breakpoint
- **When** a row renders
- **Then** the regions stack vertically; the leading image remains aligned
  with the top of the message block (no horizontal overflow)

## Accessibility

### Image and name link are announced once

- **Given** a row with a linkable entity (e.g. `COMPANY_UPDATED` with a
  resolvable entity)
- **When** a screen reader traverses the row
- **Then** the entity is announced exactly once — via the visible text link
  on the name. The image-wrapper link carries `aria-hidden="true"` and is
  skipped.

### Image link is not in the tab order

- **Given** a row with a linkable entity
- **When** the user tabs through the page
- **Then** the image-wrapper link is skipped (`tabIndex=-1`); focus moves
  directly to the name link

### Color contrast

- **Given** any rendered row
- **When** the foreground colors are measured against the white row
  background
- **Then** all text colors used (`text-oe-dark`, `text-oe-gray`,
  `text-oe-blue`) meet WCAG AA contrast (≥ 4.5:1) — and the timestamp is
  **not** rendered in `text-oe-green`

## Empty, loading, and error states

### Empty state unchanged

- **Given** the backend returns an empty `content` array
- **When** the page renders
- **Then** the existing empty state (`Bell` icon + `updates.empty`) is shown;
  no leading-image slot, no avatar, no row layout

### Loading state does not flicker layout

- **Given** the page is loading
- **When** the skeleton rows render
- **Then** the skeleton mimics the three-region row shape (leading slot +
  message + right metadata) so that subsequent real rows do not cause a
  visible layout shift

### Error state unchanged

- **Given** the backend call fails
- **When** the page renders
- **Then** the existing error state (`AlertCircle` icon + `updates.loadError`)
  is shown, unchanged

## Index redirect

### Redirect target

- **Given** a request to `/`
- **When** the `(app)` group `page.tsx` handler runs
- **Then** it issues a redirect to `/updates` (no longer to `/companies`)
