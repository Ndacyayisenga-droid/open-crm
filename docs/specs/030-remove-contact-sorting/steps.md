# Implementation Steps: Remove Contact Sorting

## Step 1: Remove sorting dropdown + cleanup

- [x] api.ts: remove sort from ContactListParams and query construction
- [x] contact-list.tsx: remove sort state, dropdown, API param, effect dependencies
- [x] i18n: remove contacts.sort block from DE and EN
- [x] Tests: remove sorting test block

**Acceptance criteria:**
- [x] Frontend: 161 tests pass, TypeScript clean
