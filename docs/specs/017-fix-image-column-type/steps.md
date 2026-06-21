# Implementation Steps: Fix Image Column Type Mismatch

## Step 1: Fix entity annotations

- [x] In `CompanyEntity.java`: remove `@Lob` from `logo` field, add `@Column(name = "logo", length = 2_097_152)` (replacing the bare `@Column(name = "logo")`)
- [x] In `ContactEntity.java`: remove `@Lob` from `photo` field, add `@Column(name = "photo", length = 2_097_152)` (replacing the bare `@Column(name = "photo")`)
- [x] Keep `@Basic(fetch = FetchType.LAZY)` on both fields
- [x] Remove unused `@Lob` import from both entity files

**Acceptance criteria:**
- [x] Project builds successfully
- [x] All existing backend tests pass (H2 schema creation works)
- [x] Image upload/download/delete tests still pass

**Related behaviors:** Backend starts successfully with H2, Backend starts successfully with PostgreSQL, Upload logo to company, Retrieve company logo, Delete company logo, Company list shows hasLogo flag correctly, Upload photo to contact, Retrieve contact photo, Delete contact photo, Contact list shows hasPhoto flag correctly, Company and contact CRUD unaffected, Existing NULL image values remain valid

---

## Step 2: Update project documentation

- [x] Update `specs/INDEX.md` — set status to "done"

**Acceptance criteria:**
- [x] INDEX.md reflects completed status

**Related behaviors:** (none — documentation step)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Backend starts successfully with PostgreSQL | Backend | 1 (verified by docker compose, not unit-testable) |
| Backend starts successfully with H2 | Backend | 1 (all tests use H2 with create-drop) |
| Upload logo to company | Backend | 1 (existing test: shouldUploadPngLogo) |
| Retrieve company logo | Backend | 1 (existing test: shouldGetLogoWithCorrectContentType) |
| Delete company logo | Backend | 1 (existing test: shouldDeleteLogo) |
| Company list shows hasLogo flag correctly | Backend | 1 (existing test: shouldSetHasLogoFalse) |
| Upload photo to contact | Backend | 1 (existing test: shouldUploadJpegPhoto) |
| Retrieve contact photo | Backend | 1 (existing test: shouldGetPhotoWithCorrectContentType) |
| Delete contact photo | Backend | 1 (existing test: shouldDeletePhoto) |
| Contact list shows hasPhoto flag correctly | Backend | 1 (existing test: shouldSetHasPhotoFalse) |
| Large file at exact size limit | Backend | 1 (covered by Spring multipart config, not explicitly tested) |
| Company and contact CRUD unaffected | Backend | 1 (all existing CRUD tests pass) |
| Existing NULL image values remain valid | Backend | 1 (existing tests create entities without images) |
