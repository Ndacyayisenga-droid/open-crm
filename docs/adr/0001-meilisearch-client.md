# ADR-0001: Do not use the `meilisearch-java` SDK

- **Status:** Accepted
- **Date:** 2026-05-25

## Context

Spec [104 — Meilisearch global search](../specs/104-meilisearch-global-search/design.md)
introduces Meilisearch as the search backend for Open CRM. The backend needs an
HTTP client to talk to the Meilisearch REST API (health checks, index
management, document upserts, multi-search queries, scoped key minting, task
polling).

The obvious off-the-shelf option is the officially maintained
[`meilisearch-java`][meilisearch-java] SDK (`com.meilisearch.sdk:meilisearch-java`,
v0.20.1 at the time of this decision). Before committing to it, we evaluated
its design, transitive dependency footprint, and fit with the existing stack.

### Surface area we actually need

Eight endpoints in total: `GET /health`, `POST /keys`, `POST /indexes`,
`POST /indexes/{u}/documents`, `DELETE /indexes/{u}/documents/{id}`,
`PATCH /indexes/{u}/settings`, `GET /tasks/{id}`, `POST /multi-search`. All
request and response payloads are JSON.

### Existing project stack

- Spring Boot 3.4, with `RestClient` already on the classpath via
  `spring-boot-starter-web`.
- Jackson is the established JSON library; it is used everywhere else in the
  backend (controllers, DTO mapping, Brevo client, webhook client, sevdesk
  client).
- The project follows a deliberate pattern of wrapping external HTTP APIs in
  thin `RestClient`-based components rather than pulling in vendor SDKs — see
  `BrevoApiClient` (spec 016), `WebhookSender` (spec 075), the sevdesk client
  (spec 081). The sevdesk spec already documents the same rationale.
- No Kotlin code; the project is pure Java.
- The repository forbids Lombok in
  [`.claude/conventions/java.md`][java-conv]: *"Do not use Lombok. Use modern
  Java features (records, pattern matching) instead."*

### What `meilisearch-java` would bring in

Direct dependencies of `meilisearch-java:0.20.1`:

| GAV                                 | Scope   | Note                                            |
|-------------------------------------|---------|-------------------------------------------------|
| `com.squareup.okhttp3:okhttp:5.3.2` | compile | Second HTTP stack alongside Spring `RestClient` |
| `com.google.code.gson:gson:2.13.2`  | runtime | Second JSON library alongside Jackson           |
| `org.json:json:20251224`            | runtime | Third JSON library                              |
| `com.auth0:java-jwt:4.5.1`          | runtime | For scoped-key tenant tokens                    |

Resolved transitive closure (new artifacts relative to the existing
Spring Boot stack):

1. `com.meilisearch.sdk:meilisearch-java`
2. `com.squareup.okhttp3:okhttp` (→ `okhttp-jvm`)
3. `com.squareup.okio:okio` (→ `okio-jvm`)
4. **`org.jetbrains.kotlin:kotlin-stdlib`** — pulls a full Kotlin runtime into a
   pure-Java project
5. `com.google.code.gson:gson`
6. `com.google.errorprone:error_prone_annotations`
7. `org.json:json`
8. `com.auth0:java-jwt`

Plus a forced upgrade of `jackson-core` / `jackson-databind` to whatever
`java-jwt` requires (currently 2.21.0), which may conflict with the version
Spring Boot pins.

### Quality signals on the SDK itself

- 240 stars / 141 forks / 30 open issues / 11 open PRs — small for an
  "official" client.
- Releases lag the Meilisearch server by 1–2 versions; open PRs at the time of
  this ADR reference *"[Meilisearch v1.40]"* and *"[Meilisearch v1.41]"*
  features still pending.
- Uses **Lombok** internally — irrelevant for consumers at runtime, but
  signals different design taste from this codebase and contradicts our
  internal Java convention.
- Mixes **three** JSON libraries (Gson + org.json + Jackson) inside the SDK
  itself, each for different code paths. We would inherit all three even though
  we only want Jackson.
- Bundles a second HTTP client (OkHttp) when we already have one.

## Decision

We do **not** depend on `com.meilisearch.sdk:meilisearch-java`.

Instead, we maintain a hand-written
`com.openelements.crm.search.MeilisearchClient` (~230 lines) built on Spring's
`RestClient`. It covers only the eight endpoints we use and is documented
inline.

## Consequences

### Positive

- **Zero new production dependencies.** Maven coordinates added by this
  decision: 0. The Meilisearch integration adds *no* artefact to the
  resolved dependency tree.
- **No Kotlin runtime, no second HTTP stack, no second/third JSON library**
  in a pure-Java Spring Boot project. Smaller jar, simpler classpath, no
  Jackson version conflict surface.
- **Smaller CRA / supply-chain attack surface.** Fewer transitive packages to
  monitor, fewer CVE feeds to track. Aligned with the Open Elements Support &
  Care positioning around lean OSS dependency hygiene.
- **No Lombok in our build path via this integration.** Honors
  [`.claude/conventions/java.md`][java-conv].
- **Server-version lag is not our problem.** Calling the REST API directly
  means new Meilisearch features are usable the moment the server supports
  them; we are not gated on the SDK catching up.
- **Consistent with the rest of the codebase.** Brevo, sevdesk, webhook
  delivery all follow the same thin-`RestClient`-wrapper pattern. A new
  contributor only has to learn one shape.

### Negative

- **We own the code.** Bug fixes, retries, new endpoints, and protocol changes
  in Meilisearch are our responsibility — the SDK would absorb some of that.
  Mitigation: the surface is genuinely small, and the REST API is stable and
  well documented.
- **Manual JSON shaping.** We hand-build request bodies via Jackson `Map`s
  rather than typed DTOs. Mitigation: keep the client thin and let callers
  construct payloads — the spec keeps the call sites few.
- **No built-in scoped-key signing helpers** (the SDK exposes a JWT helper).
  Mitigation: we mint scoped keys server-side via `POST /keys` and store them
  in an `AtomicReference`; no JWT signing is needed on our side.

### Follow-ups

- If the surface ever grows substantially (e.g. we start using federated
  search, embedders, or a much larger settings surface), revisit this ADR. The
  break-even point is roughly when the wrapper exceeds ~600 lines or when we
  start re-implementing non-trivial SDK behavior (retries with backoff,
  streaming, multipart, etc.).
- Keep the comment on `specs/104-meilisearch-global-search/design.md:123`
  factual: the SDK is *officially maintained but version-lagging and brings a
  heavy transitive footprint*, not "community" (it lives under the
  `meilisearch/` GitHub org).

[meilisearch-java]: https://github.com/meilisearch/meilisearch-java

[java-conv]: ../../.claude/conventions/java.md
