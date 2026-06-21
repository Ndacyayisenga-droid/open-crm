# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (firstName, lastName, email, companyId, language, sort) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Currently, only `companyId` is read from the URL
on initial load, and the filter dropdown does not reflect the URL-driven filter value.

_Note: the two previous test-infrastructure TODO entries ("H2 Tests: Switch to Flyway + validate" and
"Testcontainers Integration Tests") have been consolidated into **Spec 103 — Tests on Postgres via Testcontainers**.
See `specs/103-tests-postgres-testcontainers/`._

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec.

## Webhook Integration Tests

Add integration tests for webhook firing that use an embedded HTTP server (e.g. MockWebServer or WireMock) to verify
that webhook calls are actually sent with the correct payload, headers, and timing. Unit tests with mocked HTTP client
are part of the initial implementation — these integration tests go beyond that.

**Context:** Identified during the grill session for Spec 075 (Webhook Support). Prerequisite: Spec 075 must be
implemented first.

## Meilisearch Docker-Healthcheck reaktivieren

The `meilisearch` service in `docker-compose.yml` deliberately ships **without** a Docker healthcheck because the
official meilisearch image (since v1.6+) strips `wget`, `curl`, and `nc` from the base — none of the usual HTTP
healthcheck patterns can run inside the container. Configuring one anyway breaks any dependent service that uses
`condition: service_healthy` (Coolify deploys fail with "container meilisearch is unhealthy"). The backend works
around this with `condition: service_started` and an in-process connect-retry loop (60 s budget) at bootstrap.

This TODO re-evaluates the situation periodically:

- If upstream re-introduces a probe utility (`meilisearch healthcheck` subcommand, or wget/curl back in the
  image), switch back to a real Docker healthcheck and flip the backend's `depends_on` to `service_healthy`.
- Alternatively, build a thin custom Docker image (`FROM getmeili/meilisearch:vX.Y` + `apt-get install -y wget`)
  and use that — pay a few MB image size and a base-image maintenance burden for a proper healthcheck.

Re-evaluation cadence: each time meilisearch is upgraded.

**Context:** Surfaced by the first production deploy of spec 104. See `meilisearch.md` § 1 and
`specs/104-meilisearch-global-search/design.md` § 1 for the architectural reasoning.

**Prerequisite:** Spec 104 (Meilisearch global search) must be merged.

## Cmd-K-Shortcut für globale Suche

Add a `Cmd+K` / `Ctrl+K` keyboard shortcut that opens the global search from anywhere in the app — either as
a fast-path navigation to the `/search` view or as an in-place command palette overlay. The decision between
the two UX variants is part of this future spec.

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md`). v1 ships only the
dedicated `/search` view with sidebar menu entry; the keyboard shortcut is a separate later spec.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## Synchroner Fan-Out für Company- und Tag-Umbenennungen in der globalen Suche

Add listeners on `OnObjectUpdate<CompanyDto>` and `OnObjectUpdate<TagDto>` (plus the corresponding delete events)
that immediately re-index all affected contact documents in Meilisearch via a batch
`POST /indexes/crm_contacts/documents`. This eliminates the stale `companyName` / `tagNames` values in contact
search results between renames and the next backend restart.

v1 of the global search deliberately ships without this — renames are rare, deploys happen regularly, and the
auto-bootstrap on startup repairs the staleness. Implement this hardening only once operational data shows the
staleness actually bothers users (e.g. complaints, support tickets, missed search hits after a rename).

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md` § 6.3). v1 uses
defer-to-reindex; this entry tracks the synchronous fan-out as the later hardening so it does not get lost.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## Auslagerung der globalen Suche in die Open-Elements-Libs

Move the reusable parts of the global search stack out of `open-crm` and into the shared libraries, so other
Open-Elements applications can adopt the same pattern with minimal code:

- **Backend** (`spring-services`): `MeilisearchClient`, the `SearchIndexEventListener` pattern based on
  `GenericDataEvent`, `SearchSettingsConfigurer`, and a generic indexer framework where each application only
  contributes a mapping function and the index settings per entity type.
- **Frontend** (`@open-elements/ui` / `@open-elements/nextjs-app-layer`): the `/search` page shell, the
  grouped-results component, the highlight renderer, and the sidebar menu entry as prop-driven, reusable parts.

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md`). v1 keeps everything
local to `open-crm` to keep the initial scope manageable. Extraction happens once the implementation has settled
and the abstraction boundary is clear.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## GDPR-Abdeckung für Updates-View (Mitarbeiter-Aktivitätstransparenz)

Die geplante „Updates"-View (Activity Feed) zeigt jedem eingeloggten Benutzer, welcher Kollege wann welche Firma/
Person/Kommentar erstellt, geändert oder gelöscht hat. Das ist eine personenbezogene Aktivitätsverfolgung von
Mitarbeitenden durch andere Mitarbeitende und benötigt eine saubere rechtliche Grundlage — z. B. eine
Betriebsvereinbarung oder eine entsprechende Klausel im AV-Vertrag, die diese Transparenz abdeckt.

**Context:** Offene Frage aus der Grill-Session zur Updates-View-Spec. Die Spec wird mit der Annahme erstellt, dass
diese Grundlage geschaffen wird; das eigentliche Dokument/Vereinbarung ist ein separater organisatorischer Schritt.

## Awesome DB Backup

der DB_Backup Container soll aufgebohrt werden und ein REST API bereistellen durch den man Backups triggern kann und
sich Backups runterladen kann.
Es soll keine FUnktionalität geben um Backups zu löschen, da die Backups automatisch nach 7 Tagen gelöscht werden.
Alles soll additiv sein.
Es soll auch keine Funktionalität geben um Backups zu planen, da die Backups automatisch alle 24 Stunden erstellt
werden.

Das Backjend kann dann darauf zugreifen und im Frontend kann man im Admin Bereich funktionen zum triggern von Backups
und den Download des letzten backups bereitstellen.

## HEIC- und WebP-Support für Company-Logos

Extend the company-logo upload pipeline (`CompanyController.uploadLogo` / `CompanyService.updateLogo` via
`ImageData.of(file)`) to also accept HEIC and WebP uploads, transcoded to JPEG. Spec 102 deliberately ships
HEIC/WebP only for contact photos because the logo pipeline uses a different code path (`ImageData.of` helper
instead of manual content-type handling in the service) and bundling the change would have inflated spec 102's
scope.

Result is an inconsistent v1 UX: a user uploading their company logo from an iPhone (HEIC) sees an
"invalid format" error, even though uploading their own contact photo from the same iPhone works. This
TODO closes that gap. As part of the work, consider extracting the transcode logic from
`ContactPhotoTranscoder` into a shared helper so both pipelines share one source of truth instead of
diverging.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The decision to defer was a
scope-vs-consistency trade-off; logo uploads are far less frequent than contact-photo uploads, so the
inconsistency is bearable until this spec lands.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

## HEIC-Support-Status im Admin-Bereich anzeigen

Add a visual indicator in the admin section showing whether HEIC decoding is currently available
(i.e. whether `libheif` / `libheif-plugin-libde265` are present in the running container). Surfaces the
result of the `HeicSupportCheck` bean so operators detect at a glance after a deploy whether the native
dependency shipped correctly — without having to read startup logs.

Suggested UX: a small status row in the existing admin/status page (similar to the DB / Brevo health
panels), with a green check if `heicAvailable == true` and a red warning with tooltip ("HEIC uploads will
be rejected with 415 — check Dockerfile") if false. Could later be expanded into a generic
"optional-features" status panel for similar runtime-detected capabilities (WebP, PDF rendering, ...).

**Context:** Deferred from spec 102 (HEIC & WebP image format support). v1 ships with logs-only detection
— the visual admin indicator is the operational hardening so silent deploy regressions (forgotten
`libheif` install, base-image update stripping the package) are caught immediately rather than only when
the first user hits a 415.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

## HEIC- und WebP-Edge-Case-Testfixtures bereitstellen + verbleibende Tests aktivieren

Happy-path fixtures for all four formats are already in place under `backend/src/test/resources/images/`
(`test.jpeg`, `test.png`, `test.webp`, `test.heic`) — they unblock the basic decode/transcode tests in spec 102.
Several edge-case scenarios still ship `@Disabled` because they need specific variants:

- **Rotated HEIC** (EXIF orientation 6 — 90° CW) — exercises the upright-rotation path.
- **PNG with alpha** — exercises spec 101's flatten-on-white path (the existing `test.png` is opaque RGB).
- **Lossless WebP with alpha** — same flatten-on-white path on the WebP code path.
- **Animated WebP** — exercises "silent first frame only" behavior.
- **Oversize fixtures > 2 MB per format** — exercises the size-cap rejection. The four existing fixtures are
  all under 2 MB.
- **Probe sample `heic-probe/sample.heic`** (< 10 KB target) — bundled into the production JAR for the
  `HeicSupportCheck` startup probe. The existing 1 MB `test.heic` is too large to ship inside the production
  artifact.

When fixtures land, remove the corresponding `@Disabled` annotations.

Generation tools: `heif-enc` (libheif) for rotated/tiny HEIC, `cwebp -alpha_q` / animated `webpmux` for WebP
variants, ImageMagick `convert -alpha set` for alpha PNG, `dd if=/dev/urandom bs=1M count=3 > oversize.*` for
size-cap fixtures.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The four happy-path fixtures cover the
v1 decode contract; this entry tracks the remaining edge-case fixtures so the disabled scenarios become active
once produced.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

# Frontend mit PWA erweitern

Es soll einfach möglich sein, das Frontend als PWA zu installieren.

## Strikte Audience-Prüfung für JWT-Validierung (inkl. MCP-Endpoint)

The Spring Security Resource Server (`spring.security.oauth2.resourceserver.jwt`) currently validates only the JWT
signature via JWKS — it does **not** check the `aud` claim. As a result, any Authentik-issued token from the same
tenant (e.g. one issued for the `open-crm` web frontend client) is accepted on all `/api/*` endpoints, and the same
applies to the planned MCP endpoint.

For tighter isolation, configure an audience validator that requires `aud` to contain the expected client ID:

- `/api/*` (web/api-key paths) → require `aud=open-crm` (or whatever the existing frontend client is named).
- `/mcp/*` (new MCP endpoint) → require `aud=open-crm-mcp` so a leaked frontend token cannot access the MCP server
  and vice versa.

The MCP-Connector spec deliberately defers this and treats both clients as accepted; harden once the MCP endpoint
is in production and the operational impact of stricter audience checks has been evaluated.

**Context:** Surfaced during the grill session for the MCP-Connector spec. Treated as a follow-up so the initial
MCP rollout is not blocked.

**Prerequisite:** MCP-Connector spec must be merged.
