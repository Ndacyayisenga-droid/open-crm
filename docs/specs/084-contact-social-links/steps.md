# Implementation Steps: Contact Social Links

> **Note:** Migration uses V24 (not V23 as in the design) because V23 was used by Spec 082.

---

## Step 1: Database Migration

- [x] Create `V24__contact_social_links.sql`:
  1. Create `contact_social_links` table (id, contact_id FK, network_type, value, url, created_at)
  2. Migrate existing `linkedin_url` data to new table
  3. Drop `linkedin_url` column from `contacts`

**Acceptance criteria:**
- [x] Backend starts without errors
- [x] Migration applies successfully
- [x] Existing LinkedIn URLs are preserved in the new table

**Related behaviors:** Existing LinkedIn URL is migrated, Contact without LinkedIn is not affected

---

## Step 2: SocialNetworkType Enum and SocialLinkEntity

- [x] Create `SocialNetworkType` enum with URL construction and validation for all 8 networks (GITHUB, LINKEDIN, X, MASTODON, BLUESKY, DISCORD, YOUTUBE, WEBSITE)
- [x] Create `SocialLinkEntity` JPA entity (id, contactId, networkType, value, url, createdAt)
- [x] Add `@OneToMany(cascade = ALL, orphanRemoval = true)` relationship in `ContactEntity`
- [x] Remove `linkedInUrl` field from `ContactEntity`

**Acceptance criteria:**
- [x] Project compiles successfully

**Related behaviors:** All network-specific URL construction scenarios

---

## Step 3: DTOs

- [x] Create `SocialLinkDto` record (networkType, value, url)
- [x] Create `SocialLinkCreateDto` record (networkType, value)
- [x] Update `ContactDto`: replace `linkedInUrl` with `List<SocialLinkDto> socialLinks`, update `fromEntity()`
- [x] Update `ContactCreateDto`: replace `linkedInUrl` with `List<SocialLinkCreateDto> socialLinks`
- [x] Update `ContactUpdateDto`: replace `linkedInUrl` with `List<SocialLinkCreateDto> socialLinks`

**Acceptance criteria:**
- [x] Project compiles successfully

**Related behaviors:** None directly (infrastructure step)

---

## Step 4: Service Logic — Validation, URL Construction, CRUD

- [x] Update `ContactService.applyFields()`: remove linkedInUrl, add social links handling (clear + re-add pattern)
- [x] Add social link validation and URL construction using `SocialNetworkType` methods
- [x] Handle null socialLinks (preserve existing) vs empty list (remove all)
- [x] Update Brevo field protection: reject social link changes for Brevo contacts
- [x] Extend search specification to join `socialLinks` and match against `value`

**Acceptance criteria:**
- [x] Project compiles and existing tests pass (after fixing)

**Related behaviors:** All creation, update, deletion, search, and Brevo protection scenarios

---

## Step 5: Brevo Integration

- [x] Update `BrevoSyncService`: create LINKEDIN social link instead of setting linkedInUrl
- [x] Handle reimport: replace existing Brevo-managed LinkedIn social links

**Acceptance criteria:**
- [x] Project compiles

**Related behaviors:** Brevo import creates LinkedIn social link, Brevo reimport updates LinkedIn social link

---

## Step 6: CSV Export

- [x] Replace `LINKED_IN_URL` with `SOCIAL_LINKS` in `ContactExportColumn`
- [x] Extractor joins all social link URLs with commas

**Acceptance criteria:**
- [x] Project compiles

**Related behaviors:** Social links exported as comma-separated URLs, Contact with no social links has empty CSV cell

---

## Step 7: Fix Existing Backend Tests

- [x] Update all test files that reference `linkedInUrl` in ContactCreateDto/ContactUpdateDto/ContactDto constructors
- [x] Replace linkedInUrl parameter with socialLinks parameter in test fixtures

**Acceptance criteria:**
- [x] All existing backend tests pass

**Related behaviors:** None (maintenance step)

---

## Step 8: Frontend Types and i18n

- [x] Update `types.ts`: replace `linkedInUrl` with `socialLinks` array in ContactDto and ContactCreateDto
- [x] Add `SocialLinkDto` and `SocialLinkCreateDto` interfaces
- [x] Update i18n (en.ts, de.ts): replace LinkedIn-specific keys with social links section (network names, placeholders, section label)
- [x] Update CSV export column labels

**Acceptance criteria:**
- [x] Frontend compiles without type errors

**Related behaviors:** None directly (infrastructure step)

---

## Step 9: Frontend Detail View — Social Links Display

- [x] Replace LinkedIn `DetailField` with social links section in `contact-detail.tsx`
- [x] Group links by network in fixed display order (LinkedIn, GitHub, Mastodon, BlueSky, Discord, Website, X, YouTube)
- [x] Show network icon on first link of each group
- [x] Each link clickable (opens URL) and copyable
- [x] Hide networks with no links

**Acceptance criteria:**
- [x] Detail view shows social links correctly
- [x] Links are clickable and copyable

**Related behaviors:** Social links grouped by network with icons, Networks without links are hidden, Social links are clickable and copyable

---

## Step 10: Frontend Form — Social Links Editor

- [x] Replace LinkedIn text input with dynamic social links editor in `contact-form.tsx`
- [x] Network dropdown + text input + delete button per row
- [x] "+" button to add new row
- [x] Placeholder adapts to selected network
- [x] Empty rows filtered on save
- [x] Brevo contacts: social links section disabled/readonly

**Acceptance criteria:**
- [x] Form allows adding/removing social links
- [x] Correct data sent to API on save

**Related behaviors:** Add new social link via + button, Delete social link via X button, Network dropdown shows all 8 options, Placeholder adapts to selected network

---

## Step 11: Backend Tests — SocialNetworkType and Service

- [x] Unit tests for `SocialNetworkType`: URL construction for all 8 networks (username, @prefix, full URL, edge cases)
- [x] Unit tests for validation error cases (website without protocol, discord non-numeric, mastodon without instance, wrong domain URL, empty value, unknown network type)
- [x] Service integration tests: create with social links, update/replace/clear social links, null social links preserves existing, cascade delete
- [x] Service test: search finds contact by social link value
- [x] Service test: Brevo contact social links readonly

**Acceptance criteria:**
- [x] All backend tests pass
- [x] Every validation and URL construction behavior from behaviors.md is covered

**Related behaviors:** All Social Link Creation, Network-Specific URL Construction, Validation Error Cases, Social Link Update, Social Link Deletion, Search, Brevo scenarios

---

## Step 12: Backend Tests — Controller and DTO

- [x] Controller tests: POST/PUT with social links, GET returns social links
- [x] DTO conversion test: fromEntity maps social links correctly

**Acceptance criteria:**
- [x] All tests pass

**Related behaviors:** API layer coverage

---

## Step 13: Frontend Tests — Detail View and Form

- [x] Detail view tests: social links grouped display, hidden when empty, clickable links
- [x] Form tests: add/remove social link rows, network dropdown, placeholder changes, data submission

**Acceptance criteria:**
- [x] All frontend tests pass

**Related behaviors:** All Detail View Display and Edit Form scenarios

---

## Step 14: Update Project Documentation

- [x] Update `project-features.md` with social links feature
- [x] Update INDEX.md status to done

**Acceptance criteria:**
- [x] Documentation reflects current state

**Related behaviors:** None

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Create contact with GitHub link using username | Backend | Step 11 |
| Create contact with GitHub link using @-prefixed username | Backend | Step 11 |
| Create contact with GitHub link using full URL | Backend | Step 11 |
| Create contact with multiple links for same network | Backend | Step 11 |
| Create contact with links across multiple networks | Backend | Step 11 |
| Create contact with no social links | Backend | Step 11 |
| LinkedIn — username input | Backend | Step 11 |
| LinkedIn — /in/ prefix input | Backend | Step 11 |
| X — username input | Backend | Step 11 |
| X — @-prefixed username | Backend | Step 11 |
| Mastodon — full handle input | Backend | Step 11 |
| Mastodon — full URL input | Backend | Step 11 |
| BlueSky — standard handle | Backend | Step 11 |
| BlueSky — custom domain handle | Backend | Step 11 |
| Discord — numeric ID | Backend | Step 11 |
| YouTube — @handle input | Backend | Step 11 |
| YouTube — handle without @ prefix | Backend | Step 11 |
| Website — full URL | Backend | Step 11 |
| Website — input without protocol | Backend | Step 11 |
| Discord — non-numeric input | Backend | Step 11 |
| Mastodon — handle without instance | Backend | Step 11 |
| GitHub URL provided for LinkedIn network type | Backend | Step 11 |
| Empty value | Backend | Step 11 |
| Unknown network type | Backend | Step 11 |
| Replace all social links on update | Backend | Step 11 |
| Update contact without touching social links (null) | Backend | Step 11 |
| Clear all social links | Backend | Step 11 |
| Delete contact removes social links | Backend | Step 11 |
| Existing LinkedIn URL is migrated | Backend | Step 1 (verified by tests in Step 11) |
| Contact without LinkedIn is not affected | Backend | Step 1 |
| Find contact by GitHub username | Backend | Step 11 |
| Find contact by partial social link value | Backend | Step 11 |
| Brevo import creates LinkedIn social link | Backend | Step 11 |
| Brevo reimport updates LinkedIn social link | Backend | Step 11 |
| Brevo contact social links are readonly | Backend | Step 11 |
| Social links exported as comma-separated URLs | Backend | Step 11 |
| Contact with no social links has empty CSV cell | Backend | Step 11 |
| Social links grouped by network with icons | Frontend | Step 13 |
| Networks without links are hidden | Frontend | Step 13 |
| Social links are clickable and copyable | Frontend | Step 13 |
| Add new social link via + button | Frontend | Step 13 |
| Delete social link via X button | Frontend | Step 13 |
| Network dropdown shows all 8 options | Frontend | Step 13 |
| Placeholder adapts to selected network | Frontend | Step 13 |
| Social links not shown in print | Frontend | Step 13 |
