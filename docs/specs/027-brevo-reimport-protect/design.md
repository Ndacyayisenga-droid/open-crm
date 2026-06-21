# Design: Protect User-Editable Fields During Brevo Re-Import

## GitHub Issue

_To be created_

## Prerequisites

| Spec | Dependency |
|------|-----------|
| 023 — Contact brevo cleanup | `brevoId` is String, `syncedToBrevo` removed |
| 026 — Brevo fields readonly | Defines which fields are Brevo-managed vs. user-editable |

## Summary

The Brevo sync currently overwrites all fields on existing contacts during re-import — including fields the user can manually edit (position, phone, LinkedIn, birthday, company, gender). This silently destroys manual edits. Since Brevo will be used primarily for mail delivery (first name, last name, email) going forward, the sync should only overwrite Brevo-managed fields on existing contacts and leave user-editable fields untouched.

## Goals

- On re-import of existing contacts, only overwrite Brevo-managed fields: firstName, lastName, email, language
- Preserve user-editable fields: position, phoneNumber, linkedInUrl, birthday, company, gender
- On first import of new contacts, still populate all available fields from Brevo

## Non-goals

- Changing which fields are Brevo-managed vs. user-editable (defined in Spec 026)
- Adding a UI to choose which fields to sync
- Protecting company fields during company re-import (companies have a different update pattern)

## Technical Approach

### Change in `BrevoSyncService.syncContact()`

The method already distinguishes between new and existing contacts via the `created` boolean. The fix splits the field assignment into two paths:

**Current code (lines 218-232)** — sets all fields unconditionally:

```java
entity.setFirstName(firstName != null ? firstName : "");
entity.setLastName(lastName != null ? lastName : "");
entity.setEmail(email != null ? email : brevoContact.email());
entity.setPhoneNumber(sms);
entity.setPosition(jobTitle);
entity.setLinkedInUrl(linkedIn);
entity.setBrevoId(String.valueOf(brevoContact.id()));
entity.setLanguage(mapLanguage(attrs.get("SPRACHE")));
final CompanyEntity company = resolveCompany(...);
entity.setCompany(company);
```

**New code** — splits by new vs. existing:

```java
// Always set Brevo-managed fields
entity.setFirstName(firstName != null ? firstName : "");
entity.setLastName(lastName != null ? lastName : "");
entity.setEmail(email != null ? email : brevoContact.email());
entity.setBrevoId(String.valueOf(brevoContact.id()));
entity.setLanguage(mapLanguage(attrs.get("SPRACHE")));

// Only set user-editable fields on first import
if (created) {
    entity.setPhoneNumber(sms);
    entity.setPosition(jobTitle);
    entity.setLinkedInUrl(linkedIn);

    final CompanyEntity company = resolveCompany(
            brevoContact.id(), contactToCompanyBrevoId, firmaManuell);
    entity.setCompany(company);
}
```

**Rationale:** This is the simplest possible change — a single `if (created)` guard around the user-editable fields. No new abstractions, no configuration, no field-level metadata. The `created` boolean already exists and correctly distinguishes first import from re-import.

### Fields classification

| Field | Category | Reimport behavior |
|-------|----------|-------------------|
| firstName | Brevo-managed | Always overwritten |
| lastName | Brevo-managed | Always overwritten |
| email | Brevo-managed | Always overwritten |
| language | Brevo-managed | Always overwritten |
| brevoId | Brevo-managed | Always set |
| position | User-editable | Only set on first import |
| phoneNumber | User-editable | Only set on first import |
| linkedInUrl | User-editable | Only set on first import |
| company | User-editable | Only set on first import |
| birthday | User-editable | Not mapped from Brevo (no source attribute) |
| gender | User-editable | Not mapped from Brevo (no source attribute) |

**Note:** `birthday` and `gender` are not currently mapped from Brevo at all, so no change is needed for those.

## Files Affected

| File | Change |
|------|--------|
| `backend/src/main/java/.../brevo/BrevoSyncService.java` | Guard user-editable fields with `if (created)` |
| `backend/src/test/java/.../brevo/BrevoSyncServiceTest.java` | Add/update tests for reimport field protection |

## Regression Risk

- **Low**: The change is a single conditional guard. Brevo-managed fields continue to be overwritten as before. New contact import is unaffected.
- **Company resolution**: `resolveCompany()` is no longer called for existing contacts. This means `FIRMA_MANUELL` and `linkedContactsIds` are only used on first import. This is intentional — company assignment is managed in the CRM going forward.

## Open Questions

None.
