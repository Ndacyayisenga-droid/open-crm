# Implementation Steps: Company Comments

## Step 1: Backend — Remove author from CommentCreateDto and CommentUpdateDto

- [x] Remove `author` field and `@NotBlank` validation from `CommentCreateDto.java`
- [x] Remove `author` field and `@NotBlank` validation from `CommentUpdateDto.java`
- [x] Update `CommentService.addToCompany()` — set `entity.setAuthor("UNKNOWN")` instead of `request.author()`
- [x] Update `CommentService.addToContact()` — same change
- [x] Update `CommentService.update()` — do not overwrite author
- [x] Update `CommentControllerTest.java` — remove `"author"` from create request JSON bodies, verify author is `"UNKNOWN"` in responses
- [x] Update `CommentControllerTest.java` — update test JSON for comment updates (no author field)

**Acceptance criteria:**
- [x] `./mvnw clean verify` succeeds with all tests passing
- [x] POST `/api/companies/{id}/comments` with `{"text":"..."}` (no author) returns 201 with `author: "UNKNOWN"`
- [x] POST with `{"text":""}` returns 400

**Related behaviors:** CommentCreateDto ohne Author, Leerer Text wird abgelehnt, Author wird vom Backend gesetzt

---

## Step 2: Frontend — Comment types and API functions

- [x] Add `CommentDto` and `CommentCreateDto` interfaces to `src/lib/types.ts`
- [x] Add `getCompanyComments(companyId, page?)` function to `src/lib/api.ts`
- [x] Add `createCompanyComment(companyId, data)` function to `src/lib/api.ts`
- [x] Add new i18n strings to `de.ts` and `en.ts`: `placeholder`, `send`, `loadMore`, `sending`, `errorTitle`, `errorGeneric`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] New types and API functions compile correctly

**Related behaviors:** None directly — foundation

---

## Step 3: Frontend — Comment section component

- [x] Create `src/components/company-comments.tsx` — client component with:
  - Textarea for new comment + "Senden" button (disabled when empty/whitespace/sending)
  - Comment list (author, formatted date, text)
  - "Mehr laden" button when more pages exist
  - Skeleton loading state
  - Empty state "Keine Kommentare vorhanden"
  - Error dialog via AlertDialog on API failure
- [x] Replace comment placeholder in `src/components/company-detail.tsx` with `<CompanyComments companyId={company.id} />`

**Acceptance criteria:**
- [x] `pnpm build` succeeds
- [x] Comment section renders with all states (empty, loading, comments, error)
- [x] New comment appears at top of list without page reload
- [x] "Mehr laden" loads next page and appends

**Related behaviors:** Kommentare werden beim Laden angezeigt, Leere Kommentarliste zeigt Hinweis, Ladeindikator wird angezeigt, Datumsformat ist lesbar, Kommentar erfolgreich erstellen, Senden-Button deaktiviert bei leerem Text, Senden-Button deaktiviert während Sendens, Fehler beim Erstellen zeigt modalen Dialog, Neuer Kommentar erscheint ohne Seite neu zu laden, Erste 20 Kommentare werden initial geladen, Mehr laden fügt Kommentare hinzu, Mehr laden verschwindet wenn alle geladen, Kein Mehr-laden-Button bei weniger als 20, Neuer Kommentar nach Mehr-laden, Nur Whitespace im Eingabefeld, Sehr langer Kommentartext, Firma nicht gefunden

---

## Step 4: Backend Tests — Author and validation

- [x] Verify existing backend tests pass with updated CreateDto (no author field)
- [x] Verify POST with `{"text":"Ein Kommentar"}` returns `author: "UNKNOWN"`
- [x] Verify POST with `{"text":""}` returns 400
- [x] Verify update does not change author

**Acceptance criteria:**
- [x] `./mvnw clean verify` — all backend tests pass

**Related behaviors:** CommentCreateDto ohne Author, Leerer Text wird abgelehnt

---

## Step 5: Frontend Tests — Comment display and creation

- [x] Test: comments rendered with author, date, text when company has comments
- [x] Test: empty state shown when no comments
- [x] Test: loading skeleton shown while fetching
- [x] Test: date displayed in readable format
- [x] Test: send button disabled when text is empty
- [x] Test: send button disabled when only whitespace
- [x] Test: send button disabled while sending (shows "Wird gesendet...")
- [x] Test: successful create adds comment to top of list and clears textarea
- [x] Test: create request body contains only `text`, no `author`
- [x] Test: error dialog shown on API failure, text preserved in textarea
- [x] Test: "Mehr laden" button shown when more pages exist
- [x] Test: "Mehr laden" appends comments to existing list
- [x] Test: "Mehr laden" hidden when all comments loaded
- [x] Test: no "Mehr laden" when fewer than 20 comments

**Acceptance criteria:**
- [x] `pnpm test` passes
- [x] `pnpm build` succeeds

**Related behaviors:** All frontend scenarios from behaviors.md

---

## Step 6: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` — update Comments feature description (author auto-set, frontend integration)
- [x] Update `README.md` — add comment functionality to feature list if not already present

**Acceptance criteria:**
- [x] Documentation reflects current implementation state

**Related behaviors:** None — documentation

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Kommentare werden beim Laden angezeigt | Frontend | Steps 3, 5 |
| Leere Kommentarliste zeigt Hinweis | Frontend | Steps 3, 5 |
| Ladeindikator wird angezeigt | Frontend | Steps 3, 5 |
| Datumsformat ist lesbar | Frontend | Steps 3, 5 |
| Kommentar erfolgreich erstellen | Frontend | Steps 3, 5 |
| Senden-Button deaktiviert bei leerem Text | Frontend | Steps 3, 5 |
| Senden-Button deaktiviert während Sendens | Frontend | Steps 3, 5 |
| Author wird vom Backend gesetzt | Both | Steps 1, 4, 5 |
| Fehler beim Erstellen zeigt modalen Dialog | Frontend | Steps 3, 5 |
| Neuer Kommentar erscheint ohne Seite neu zu laden | Frontend | Steps 3, 5 |
| Erste 20 Kommentare werden initial geladen | Frontend | Steps 3, 5 |
| Mehr laden fügt Kommentare hinzu | Frontend | Steps 3, 5 |
| Mehr laden verschwindet wenn alle geladen | Frontend | Steps 3, 5 |
| Kein Mehr-laden-Button bei weniger als 20 | Frontend | Steps 3, 5 |
| Neuer Kommentar nach Mehr-laden | Frontend | Steps 3, 5 |
| Nur Whitespace im Eingabefeld | Frontend | Steps 3, 5 |
| Sehr langer Kommentartext | Frontend | Steps 3, 5 |
| Firma nicht gefunden | Frontend | Step 3 |
| CommentCreateDto ohne Author | Backend | Steps 1, 4 |
| Leerer Text wird abgelehnt | Backend | Steps 1, 4 |
