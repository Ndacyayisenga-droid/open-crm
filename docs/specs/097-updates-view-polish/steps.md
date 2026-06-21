# Implementation Steps: Updates view — visual polish & default landing route

## Step 1: Extend `UpdateEntryDto` with two additive boolean fields

- [x] Modify `backend/src/main/java/com/openelements/crm/updates/UpdateEntryDto.java` to add two
      new boolean fields: `entityHasLogo` and `entityHasPhoto`. Update the Javadoc to describe
      what they mean and when they are `false`.

**Acceptance criteria:**

- [x] Record compiles with the new fields.
- [x] Existing callers fail to compile (good — they will be fixed in Step 2).
- [x] `mvn -pl backend -am compile` succeeds after Step 2 lands.

**Related behaviors:** Backwards compatibility (the DTO change is additive).

---

## Step 2: Derive flags in `UpdatesService.resolveNames`

- [x] Modify `backend/src/main/java/com/openelements/crm/updates/UpdatesService.java`:
  - In `resolveNames`, additionally collect `Map<UUID, Boolean>` for `companyHasLogo` and
    `contactHasPhoto`, populated from the same `findAllById` iterations already used to compute
    names.
  - For each entry, pass the appropriate flag through to the new `UpdateEntryDto` constructor.
  - `COMPANY_DELETED` / `CONTACT_DELETED` rows: both flags `false`.
  - Unresolved entities (race condition): both flags `false`.
  - Comment events use the parent's flag (logo for company comments, photo for contact comments).

**Acceptance criteria:**

- [x] `mvn -pl backend -am compile` succeeds.
- [x] The two existing iterations over companies/contacts are reused — no extra repository calls.

**Related behaviors:**
- Company event with logo / Company event without logo
- Contact event with photo / Contact event without photo
- `COMPANY_DELETED` event / `CONTACT_DELETED` event
- Entity no longer exists (race)
- Backwards compatibility (additive only)

---

## Step 3: Extend backend tests for the new DTO fields

- [x] Modify `backend/src/test/java/com/openelements/crm/updates/UpdatesServiceTest.java` to
      verify `entityHasLogo` / `entityHasPhoto` for: companies with and without logo, contacts
      with and without photo, `COMPANY_DELETED`, `CONTACT_DELETED`, `COMPANY_COMMENT_*` (parent
      with and without logo), `CONTACT_COMMENT_*` (parent with and without photo), and the race
      case where an entity vanished.
- [x] Add helper methods to set `logo` / `photo` bytes on the entities under test (use a small
      stub byte array — the field only needs to be non-null for the flag to flip).
- [x] Modify `backend/src/test/java/com/openelements/crm/updates/UpdatesControllerTest.java` to
      assert that the JSON response includes the two new fields with the expected values for at
      least one company-with-logo and one delete event.

**Acceptance criteria:**

- [x] `mvn -pl backend -am test -Dtest='UpdatesServiceTest'` passes.
- [x] `mvn -pl backend -am test -Dtest='UpdatesControllerTest'` passes.

**Related behaviors:**
- Company event with logo / without logo
- Contact event with photo / without photo
- `COMPANY_DELETED` / `CONTACT_DELETED` event
- Entity no longer exists (race)
- Backwards compatibility

---

## Step 4: Frontend type — extend `UpdateEntryDto`

- [x] Modify `frontend/src/lib/types.ts` and add `readonly entityHasLogo: boolean;` and
      `readonly entityHasPhoto: boolean;` to the `UpdateEntryDto` interface.

**Acceptance criteria:**

- [x] `pnpm --filter frontend type-check` (or equivalent) passes after the consumer updates in
      Steps 5–7.
- [x] Test fixtures that build `UpdateEntryDto` are updated to provide the new fields.

**Related behaviors:** Backwards compatibility.

---

## Step 5: Index redirect — `/` → `/updates`

- [x] Modify `frontend/src/app/(app)/page.tsx` to call `redirect("/updates")` instead of
      `redirect("/companies")`.

**Acceptance criteria:**

- [x] File compiles.
- [x] Step 8 adds a test that pins the redirect target.

**Related behaviors:** Redirect target / Authenticated user opens the app root / Unauthenticated
user opens the app root.

---

## Step 6: Frontend — `EntityImage` and `UserAvatar` helpers in `updates-client.tsx`

- [x] Modify `frontend/src/app/(app)/updates/updates-client.tsx`:
  - Import `getCompanyLogoUrl`, `getContactPhotoUrl` from `@/lib/api`.
  - Import `Trash2`, `Building2`, `User as UserIcon` from `lucide-react`.
  - Add a local `EntityImage` component that consumes `{ entry }` and returns the 32×32 leading
    slot per `design.md`:
    - `COMPANY_DELETED` / `CONTACT_DELETED` → `Trash2` icon, `aria-hidden="true"`, no link.
    - `COMPANY_*` with `entityHasLogo && entityId` → `<img>` from `getCompanyLogoUrl(entityId)`,
      `h-8 w-8 object-contain rounded`, `alt=""`, wrapped in a `<Link>` with
      `aria-hidden="true"` and `tabIndex={-1}` to `/companies/{entityId}`.
    - `COMPANY_*` without logo → `Building2` placeholder (`h-8 w-8 text-oe-gray-mid`).
    - `CONTACT_*` with `entityHasPhoto && entityId` → `<img>` from
      `getContactPhotoUrl(entityId)`, `h-8 w-8 rounded-full object-cover`, wrapped in an
      `aria-hidden` `<Link>` to `/contacts/{entityId}`.
    - `CONTACT_*` without photo → `UserIcon` placeholder (`h-8 w-8 text-oe-gray-mid`).
  - Add a local `UserAvatar` component that renders an `<img>` (size `h-5 w-5 rounded-full
    object-cover`, `alt=""`) when `user.avatarUrl` is set, or a fallback circle
    (`bg-oe-gray-lightest`, centered `UserIcon h-3 w-3`,
    `data-testid="updates-author-avatar-fallback"`, `aria-hidden="true"`) otherwise.

**Acceptance criteria:**

- [x] Type-check passes.
- [x] Components are pure and rely only on props (no extra fetches).

**Related behaviors:**
- Company with logo / Company without logo
- Contact with photo / Contact without photo
- Company comment event uses parent logo
- Contact comment event uses parent photo
- `COMPANY_DELETED` row / `CONTACT_DELETED` row
- `COMPANY_COMMENT_DELETED` keeps parent logo
- Author with avatar URL / Author without avatar URL

---

## Step 7: Frontend — three-region row layout & bold name

- [x] In `frontend/src/app/(app)/updates/updates-client.tsx`, restructure the `<li>` in the
      mapping over `entries`:
  - Use `flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:gap-3`.
  - Region 1: `<EntityImage entry={entry} />`.
  - Region 2: `<div className="min-w-0 flex-1">` containing the message. The linked entity name
    uses `font-bold text-oe-blue underline-offset-2 hover:underline`. When the name is plain
    (`rendered.link === null` but the template contained `{name}`), render the fallback display
    name wrapped in `<span className="font-bold">`. When the template has no `{name}` (the
    `*_DELETED` messages), render plain text.
  - Region 3: `<div className="flex shrink-0 items-center gap-2 text-sm text-oe-gray">` with the
    user avatar, the author name (via `formatBy(t.updates.by, …)`), an `aria-hidden="true"`
    middle dot, and the locale-formatted timestamp.
  - `renderTemplate` is extended so the caller can distinguish "template had `{name}` but link
    is null" from "template had no `{name}` at all" — either by returning an explicit
    `plainName: string | null` field, or by exposing whether the template contained `{name}`.
    Adjust the existing logic minimally; do not change the German/English copy.
- [x] Confirm the page-size selector label still reads `t.updates.perPage`. Make no rename.

**Acceptance criteria:**

- [x] Type-check passes.
- [x] The page-size combobox label is unchanged.
- [x] Loading skeleton mimics the three-region layout (32×32 placeholder + horizontal bar +
      right metadata skeleton) so the layout does not jump on load.

**Related behaviors:**
- Linkable entity name is bold and blue
- Plain entity name is bold but not blue
- `*_DELETED` message has no bold name
- Desktop layout / Mobile layout
- Empty state unchanged
- Loading state does not flicker layout
- Error state unchanged
- Image and name link are announced once
- Image link is not in the tab order
- Color contrast
- Label is unchanged

---

## Step 8: Test — `/` redirects to `/updates`

- [x] Add `frontend/src/app/(app)/__tests__/page.test.tsx` that imports the page component and
      asserts that calling it triggers `redirect("/updates")` via a mocked `next/navigation`.

**Acceptance criteria:**

- [x] Test passes via `pnpm --filter frontend test`.

**Related behaviors:** Redirect target.

---

## Step 9: Frontend tests — row layout, image slot, avatar, bold name

- [x] Extend `frontend/src/app/(app)/updates/__tests__/updates-client.test.tsx`:
  - Update the `makeEntry` helper to default `entityHasLogo: false` and `entityHasPhoto: false`.
  - Add a test that `COMPANY_UPDATED` with `entityHasLogo: true` renders an `<img>` with `src`
    equal to `getCompanyLogoUrl(entityId)` (mock the helper to return a deterministic URL) and
    is wrapped in a link with `aria-hidden="true"` and `tabIndex={-1}` to
    `/companies/{entityId}`.
  - Add a test that `COMPANY_UPDATED` with `entityHasLogo: false` renders a `Building2` icon
    placeholder, not wrapped in a link.
  - Add a test for `CONTACT_UPDATED` with `entityHasPhoto: true` (round `<img>`, link to
    `/contacts/{id}` with `aria-hidden`/`tabIndex={-1}`).
  - Add a test for `CONTACT_UPDATED` with `entityHasPhoto: false` → `UserIcon` placeholder.
  - Add a test that `COMPANY_COMMENT_CREATED` with `entityHasLogo: true` shows the parent
    company's logo and links to `/companies/{id}`.
  - Add a test that `CONTACT_COMMENT_CREATED` with `entityHasPhoto: true` shows the parent
    contact's photo (round) and links to `/contacts/{id}`.
  - Add a test that `COMPANY_DELETED` renders a `Trash2` icon (queryable by a stable selector,
    e.g. `data-testid="updates-row-deleted-icon"` or by `aria-hidden` on the SVG wrapper) and is
    not wrapped in a link.
  - Add an analogous test for `CONTACT_DELETED`.
  - Add a test that `COMPANY_COMMENT_DELETED` with a resolvable parent (`entityId` set,
    `entityHasLogo: true`) renders the parent logo — **not** a `Trash2` icon.
  - Add a test that the entity-name link in a linkable row uses `font-bold` (assert the class).
  - Add a test that the fallback `"—"` (entity unresolved) is bold (`font-bold`) and is not a
    link.
  - Add a test that the row shows a `UserAvatar` with an `<img>` matching `avatarUrl` when set.
  - Add a test that the row shows the fallback avatar (`data-testid="updates-author-avatar-fallback"`)
    when `avatarUrl` is null.
  - Add a test that the page-size combobox label remains `Pro Seite` / `Per page` (smoke
    assertion against the rendered DOM).

**Acceptance criteria:**

- [x] `pnpm --filter frontend test --run updates-client` passes.
- [x] No `console.error` from network/onError fallbacks (the design uses backend flags, not
      `<img onError>`).

**Related behaviors:** all scenarios under "Frontend — row leading image slot", "Frontend —
message and bold name", "Frontend — user avatar slot", and "Frontend — page-size selector
label".

---

## Step 10: Update project documentation

- [x] `.claude/conventions/project-specific/project-features.md` — note the enriched Updates
      view rows and the `/ → /updates` default landing route, if a corresponding entry from
      spec 096 is present.
- [x] `.claude/conventions/project-specific/project-architecture.md` — no architectural change;
      only update if the existing description of the Updates feed mentions row layout.
- [x] `README.md` — only if the default landing route was previously documented.
- [x] No new dependencies; `project-tech.md` and `project-structure.md` need no changes.

**Acceptance criteria:**

- [x] Affected docs reflect the new behaviour.

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|---|---|---|
| Authenticated user opens the app root | Frontend | 5, 8 |
| Unauthenticated user opens the app root | Frontend | 5 (existing auth middleware unchanged), 8 |
| Company event with logo | Backend | 2, 3 |
| Company event without logo | Backend | 2, 3 |
| Contact event with photo | Backend | 2, 3 |
| Contact event without photo | Backend | 2, 3 |
| `COMPANY_DELETED` event | Backend | 2, 3 |
| `CONTACT_DELETED` event | Backend | 2, 3 |
| Entity no longer exists (race) | Backend | 2, 3 |
| Backwards compatibility | Both | 1, 4 |
| Company with logo (row) | Frontend | 6, 7, 9 |
| Company without logo (row) | Frontend | 6, 7, 9 |
| Contact with photo (row) | Frontend | 6, 7, 9 |
| Contact without photo (row) | Frontend | 6, 7, 9 |
| Company comment event uses parent logo | Frontend | 6, 7, 9 |
| Contact comment event uses parent photo | Frontend | 6, 7, 9 |
| `COMPANY_DELETED` row | Frontend | 6, 7, 9 |
| `CONTACT_DELETED` row | Frontend | 6, 7, 9 |
| `COMPANY_COMMENT_DELETED` keeps parent logo | Frontend | 6, 7, 9 |
| Linkable entity name is bold and blue | Frontend | 7, 9 |
| Plain entity name is bold but not blue | Frontend | 7, 9 |
| `*_DELETED` message has no bold name | Frontend | 7, 9 |
| Author with avatar URL | Frontend | 6, 7, 9 |
| Author without avatar URL | Frontend | 6, 7, 9 |
| Label is unchanged | Frontend | 7, 9 |
| Desktop layout | Frontend | 7 (CSS) |
| Mobile layout | Frontend | 7 (CSS) |
| Image and name link are announced once | Frontend | 6, 7, 9 |
| Image link is not in the tab order | Frontend | 6, 9 |
| Color contrast | Frontend | 7 (design uses oe-dark / oe-gray / oe-blue) |
| Empty state unchanged | Frontend | 7 |
| Loading state does not flicker layout | Frontend | 7 |
| Error state unchanged | Frontend | 7 |
| Redirect target | Frontend | 5, 8 |
