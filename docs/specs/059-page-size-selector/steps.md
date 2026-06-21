# Implementation Steps: Page Size Selector

## Step 1: i18n — add "per page" translation key

- [x] Add `pagination.perPage` to `en.ts` for companies, contacts, and tags sections
- [x] Add `pagination.perPage` to `de.ts` for companies, contacts, and tags sections

**Acceptance criteria:**
- [x] Frontend builds successfully

**Related behaviors:** (foundation for UI steps)

---

## Step 2: Company list — page size selector

- [x] Add `pageSize` state initialized from `localStorage` key `pageSize.companies` (default 20)
- [x] Add `<Select>` dropdown with options 10, 20, 50, 100, 200 in pagination bar
- [x] On change: update state, persist to localStorage, reset page to 0
- [x] Replace hardcoded `size: 20` with `pageSize` in API call

**Acceptance criteria:**
- [x] Page size selector visible in company list pagination bar
- [x] Changing page size fetches correct number of records
- [x] Frontend builds successfully

**Related behaviors:** Default page size is 20, User selects a different page size, Page size persists across page reloads, Page size persists during filter changes, Page size persists during page navigation, All five options are available, Invalid localStorage value

---

## Step 3: Contact list — page size selector

- [x] Same changes as Step 2 but for contact-list.tsx with key `pageSize.contacts`

**Acceptance criteria:**
- [x] Page size selector visible in contact list pagination bar
- [x] Frontend builds successfully

**Related behaviors:** Page size is stored per list independently

---

## Step 4: Tag list — page size selector and range fix

- [x] Add `pageSize` state initialized from `localStorage` key `pageSize.tags` (default 20)
- [x] Add `<Select>` dropdown with options 10, 20, 50, 100, 200 in pagination bar
- [x] Replace hardcoded `20` in range calculation (start/end) with `pageSize`
- [x] Replace hardcoded `size: 20` in API call with `pageSize`

**Acceptance criteria:**
- [x] Page size selector visible in tag list pagination bar
- [x] Range display ("Showing X–Y of Z") updates correctly with page size
- [x] Frontend builds successfully

**Related behaviors:** Tag list range display updates correctly, Tag list range display on last page

---

## Step 5: Update INDEX.md

- [x] Set spec 059 status to `done`

**Acceptance criteria:**
- [x] INDEX.md reflects completed status

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Default page size is 20 | Frontend | Step 2 |
| User selects a different page size | Frontend | Step 2 |
| Page size persists across page reloads | Frontend | Step 2 |
| Page size is stored per list independently | Frontend | Steps 2, 3 |
| Page size persists during filter changes | Frontend | Step 2 |
| Page size persists during page navigation | Frontend | Step 2 |
| All five options are available | Frontend | Step 2 |
| Selecting the smallest page size | Frontend | Step 2 |
| Selecting the largest page size | Frontend | Step 2 |
| Record count updates with page size change | Frontend | Step 2 |
| Record count with page size larger than total | Frontend | Step 2 |
| Tag list range display updates correctly | Frontend | Step 4 |
| Tag list range display on last page | Frontend | Step 4 |
| Invalid localStorage value | Frontend | Step 2 |
| Page size change when on a page beyond new range | Frontend | Step 2 |
| Print view ignores page size setting | Frontend | N/A (not affected) |
| CSV export ignores page size setting | Frontend | N/A (not affected) |
| Comment lists are not affected | Frontend | N/A (not affected) |
