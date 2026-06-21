# Implementation Steps: Count Columns

## Step 1: Backend — Repository Count Methods

- [x] Add `long countByCompanyId(UUID companyId)` to `CommentRepository`
- [x] Add `long countByContactId(UUID contactId)` to `CommentRepository`
- [x] Add `long countByCompanyId(UUID companyId)` to `ContactRepository`

## Step 2: Backend — CompanyDto + CompanyService with counts

- [x] Add `contactCount` and `commentCount` fields to `CompanyDto`
- [x] Add overloaded `fromEntity(entity, contactCount, commentCount)` factory method
- [x] Inject `CommentRepository` into `CompanyService`
- [x] Update all methods returning `CompanyDto` to resolve counts
- [x] `create()` returns counts as 0

## Step 3: Backend — ContactDto + ContactService with counts

- [x] Add `commentCount` field to `ContactDto`
- [x] Add overloaded `fromEntity(entity, commentCount)` factory method
- [x] Update `ContactService` to resolve comment counts via `CommentRepository`
- [x] `create()` returns count as 0

## Step 4: Frontend — Types and i18n

- [x] Add `contactCount: number` and `commentCount: number` to `CompanyDto` in `types.ts`
- [x] Add `commentCount: number` to `ContactDto` in `types.ts`
- [x] Add `companies.columns.contacts` and `companies.columns.comments` to DE + EN
- [x] Add `contacts.columns.comments` to DE + EN

## Step 5: Frontend — Company list count columns

- [x] Add "Kontakte"/"Contacts" and "Kommentare"/"Comments" columns to company-list.tsx
- [x] Column order: Name, Website, Contacts, Comments, Actions

## Step 6: Frontend — Contact list count column

- [x] Add "Kommentare"/"Comments" column to contact-list.tsx
- [x] Column order: First Name, Last Name, Company, Comments, Actions

## Step 7: Frontend — Company detail count displays

- [x] Show contact count in "show employees" link: "Alle Mitarbeiter (x)"
- [x] Pass `company.commentCount` as `totalCount` to CompanyComments

## Step 8: Frontend — Comment components count heading

- [x] Add optional `totalCount` prop to CompanyComments, display in heading
- [x] Pass `contact.commentCount` as `totalCount` to ContactComments
- [x] Add optional `totalCount` prop to ContactComments, display in heading

## Step 9: Frontend tests — Company list counts

- [x] Update company list tests for new columns and count rendering

## Step 10: Frontend tests — Contact list counts

- [x] Update contact list tests for new column and count rendering

## Step 11: Frontend tests — Company detail counts

- [x] Update company detail tests for count in show-employees link and comments heading

## Step 12: Frontend tests — Contact detail counts

- [x] Update contact detail tests for comment count in heading

## Step 13: Update documentation and INDEX.md

- [x] Update specs/INDEX.md to mark 011 as done
- [x] Update project-features.md with count column info
