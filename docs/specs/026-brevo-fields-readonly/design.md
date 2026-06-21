# Design: Make Brevo-Managed Contact Fields Read-Only

## GitHub Issue

_To be created_

## Prerequisites

| Spec | Dependency |
|------|-----------|
| 023 — Contact brevo cleanup | Contact API exposes computed `brevo: true/false` field, `brevoId` is String |

## Summary

When a contact was imported from Brevo, the fields first name, last name, email, and language are managed by Brevo and overwritten on every sync. Manual edits to these fields would be silently lost on the next import. This spec makes these fields read-only for Brevo contacts — both in the frontend edit form (disabled with hint text) and enforced in the backend API (400 error on attempted change).

## Goals

- Prevent editing of Brevo-managed fields (firstName, lastName, email, language) for Brevo contacts
- Show disabled fields with current values and a hint explaining why they are locked
- Reject API requests that attempt to change protected fields on Brevo contacts
- Keep all other fields (position, phone, LinkedIn, birthday, company, photo, gender) fully editable

## Non-goals

- Protecting fields on companies (companies are not edited via the same form pattern)
- Adding a mechanism to "detach" a contact from Brevo
- Changing the Brevo sync behavior (it writes directly to the entity, unaffected by this spec)

## Technical Approach

### Backend — Validation in `ContactService.update()`

Before applying fields, check if the contact has a `brevoId`. If so, compare the incoming values for the four protected fields against the current entity values. If any differ, throw a `ResponseStatusException` with status `400`.

```java
if (entity.getBrevoId() != null) {
    final List<String> violations = new ArrayList<>();
    if (!Objects.equals(request.firstName(), entity.getFirstName())) {
        violations.add("firstName");
    }
    if (!Objects.equals(request.lastName(), entity.getLastName())) {
        violations.add("lastName");
    }
    if (!Objects.equals(request.email(), entity.getEmail())) {
        violations.add("email");
    }
    if (request.language() != entity.getLanguage()) {
        violations.add("language");
    }
    if (!violations.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The fields " + String.join(", ", violations)
            + " are managed by Brevo and cannot be modified");
    }
}
```

**Rationale:** Comparing against current values (rather than blanket-rejecting the fields) allows the frontend to send the full DTO including the unchanged protected fields. This avoids requiring separate DTO shapes for Brevo vs. non-Brevo contacts. The error message clearly states which fields violated the constraint and why.

### Frontend — Contact form (`contact-form.tsx`)

The form receives the contact object (including `brevo: boolean`) as a prop when editing.

**Disabled state:** When `contact.brevo === true` and the form is in edit mode:
- `firstName`, `lastName`, `email`, and `language` inputs are rendered with `disabled={true}`
- Each disabled field shows a small hint text below it

**Hint text:** A small muted line (`text-xs text-oe-gray-mid`) below each disabled field:
- EN: "Managed by Brevo"
- DE: "Wird von Brevo verwaltet"

**Submission:** The form still sends the current (unchanged) values for the disabled fields. The backend accepts unchanged values without error.

**Create mode:** When creating a new contact, all fields are always editable regardless of any state (there is no Brevo association yet).

### Frontend — Translations (`en.ts`, `de.ts`)

Add a new key for the hint text:

```
contacts.form.managedByBrevo: "Managed by Brevo"  // EN
contacts.form.managedByBrevo: "Wird von Brevo verwaltet"  // DE
```

### Frontend — Error handling

If the backend returns 400 with the Brevo protection message (e.g., due to a race condition where a contact was synced from Brevo between loading the form and submitting), the existing generic error display in the form handles this — no special handling needed.

### Tests

| File | Change |
|------|--------|
| `ContactServiceTest.java` | Test: update Brevo contact with changed firstName → 400. Test: update Brevo contact with unchanged fields → success. Test: update non-Brevo contact with changed firstName → success. |
| `ContactControllerTest.java` | Integration test for 400 response on protected field change |
| `contact-form.test.tsx` | Test: fields disabled when `brevo = true` in edit mode. Test: hint text visible. Test: fields editable when `brevo = false`. Test: fields editable in create mode. |

## Files Affected

| File | Change |
|------|--------|
| `backend/.../contact/ContactService.java` | Add Brevo field protection in `update()` |
| `frontend/src/components/contact-form.tsx` | Disable fields + hint text for Brevo contacts |
| `frontend/src/lib/i18n/en.ts` | Add `managedByBrevo` key |
| `frontend/src/lib/i18n/de.ts` | Add `managedByBrevo` key |
| Backend test files (2) | Validation tests |
| Frontend test files (1) | Disabled state tests |

## Open Questions

None.
