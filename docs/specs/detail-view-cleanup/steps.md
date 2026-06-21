# Implementation Steps: Detail View Cleanup

## Step 1: Add i18n language display labels and address label

- [ ] In `frontend/src/lib/i18n/de.ts`, add language display labels (`languageDE`, `languageEN`, `languageUnknown`) to `contacts.detail` and add `address` label to `companies.detail`
- [ ] In `frontend/src/lib/i18n/en.ts`, add corresponding English labels

**Acceptance criteria:**
- [ ] TypeScript compiles without errors
- [ ] Both language files have matching keys

**Related behaviors:** Language shown as translated label (DE UI), Language shown as translated label (EN UI), Address label is translated

---

## Step 2: Contact detail — remove name fields and add language helper

- [ ] In `contact-detail.tsx`, remove the `<DetailField>` entries for `firstName` and `lastName`
- [ ] Add a `languageLabel()` helper function that maps "DE"/"EN"/null to translated labels
- [ ] Update the language `<DetailField>` to use `languageLabel()` instead of raw code

**Acceptance criteria:**
- [ ] No firstName/lastName detail fields rendered (header still shows full name)
- [ ] Language displays as "Deutsch"/"Englisch"/"Unbekannt" (DE UI) or "German"/"English"/"Unknown" (EN UI)
- [ ] Project builds successfully

**Related behaviors:** Name not shown as detail field, Language shown as translated label (DE UI), Language shown as translated label for EN contact (DE UI), Language shown as translated label (EN UI), Language shown as translated label for EN contact (EN UI), Null language shown as unknown (DE UI), Null language shown as unknown (EN UI)

---

## Step 3: Company detail — merge address fields into single block

- [ ] In `company-detail.tsx`, replace the five separate address `<DetailField>` entries with a single "Address" field
- [ ] Format address as multi-line block: line 1 = street + houseNumber, line 2 = zipCode + city, line 3 = country
- [ ] Show "—" when all address fields are null

**Acceptance criteria:**
- [ ] Address displays as formatted multi-line block
- [ ] Missing fields are handled per line-composition rules
- [ ] All-null address shows "—"
- [ ] Project builds successfully

**Related behaviors:** Full address displayed as multi-line block, Address without house number, Address without street (house number ignored), Address with only city and country, Address with only zip code, Address with only country, All address fields null, Address label is translated

---

## Step 4: Update frontend tests

- [ ] Update `contact-detail.test.tsx` to verify firstName/lastName fields are NOT rendered as DetailFields
- [ ] Add test for language display as translated label (DE language code → "Deutsch")
- [ ] Add test for null language → "Unbekannt"
- [ ] Update `company-detail.test.tsx` to verify merged address block renders correctly
- [ ] Add test for partial address (missing fields)
- [ ] Add test for all-null address showing "—"

**Acceptance criteria:**
- [ ] All new and updated tests pass
- [ ] All existing tests pass
- [ ] Every behavioral scenario from behaviors.md is covered

**Related behaviors:** All scenarios from behaviors.md

---

## Step 5: Update project documentation

- [ ] Update `specs/INDEX.md` to set spec 036 status to `done`

**Acceptance criteria:**
- [ ] INDEX.md reflects correct status
