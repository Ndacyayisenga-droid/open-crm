# Behaviors: Component-Level Tests (Repository, Service, DTO)

---

# Company Module

## CompanyDto.fromEntity

### Maps all scalar fields from entity

- **Given** a CompanyEntity with id, name, email, website, street, houseNumber, zipCode, city, country, deleted=false, createdAt, and updatedAt all populated
- **When** `CompanyDto.fromEntity(entity, 5, 3)` is called
- **Then** the returned CompanyDto has every field matching the entity, contactCount=5, commentCount=3

### Sets hasLogo to true when logo bytes are present

- **Given** a CompanyEntity with `logo` set to a non-null byte array
- **When** `CompanyDto.fromEntity(entity, 0, 0)` is called
- **Then** the returned CompanyDto has `hasLogo=true`

### Sets hasLogo to false when logo bytes are null

- **Given** a CompanyEntity with `logo` set to null
- **When** `CompanyDto.fromEntity(entity, 0, 0)` is called
- **Then** the returned CompanyDto has `hasLogo=false`

### Handles null optional fields

- **Given** a CompanyEntity with only `name` set and all optional fields (email, website, street, houseNumber, zipCode, city, country) null
- **When** `CompanyDto.fromEntity(entity, 0, 0)` is called
- **Then** the returned CompanyDto has null for each optional field and `deleted=false`

## CompanyRepository

### Saves and retrieves a company

- **Given** a CompanyEntity with name "Test Corp" and city "Berlin"
- **When** the entity is persisted and then retrieved by ID
- **Then** the retrieved entity has name "Test Corp", city "Berlin", deleted=false, and non-null createdAt/updatedAt

### Rejects a company without a name (NOT NULL constraint)

- **Given** a CompanyEntity with name set to null
- **When** the entity is persisted and flushed
- **Then** a ConstraintViolationException or DataIntegrityViolationException is thrown

### Finds all companies (findAll)

- **Given** 3 persisted companies
- **When** `findAll()` is called
- **Then** the result contains exactly 3 entities

### Deletes a company by ID

- **Given** a persisted company
- **When** `deleteById(id)` is called and flushed
- **Then** `findById(id)` returns empty

### Supports Specification-based queries (JpaSpecificationExecutor)

- **Given** two companies: one with name "Alpha" and one with name "Beta"
- **When** `findAll` is called with a Specification filtering name LIKE "%lph%"
- **Then** only the "Alpha" company is returned

### existsById returns false for nonexistent ID

- **Given** no companies in the database
- **When** `existsById(randomUUID)` is called
- **Then** the result is false

## CompanyService

### create -- creates a company and returns DTO with zero counts

- **Given** a CompanyCreateDto with name "New Corp", email "info@new.com", city "Berlin"
- **When** `create(dto)` is called
- **Then** the returned CompanyDto has name "New Corp", email "info@new.com", city "Berlin", deleted=false, contactCount=0, commentCount=0, and a non-null id

### create -- persists to database

- **Given** a CompanyCreateDto with name "Persisted Corp"
- **When** `create(dto)` is called
- **Then** calling `getById` with the returned id retrieves the same company

### getById -- returns company with correct counts

- **Given** a company with 2 contacts and 3 comments
- **When** `getById(companyId)` is called
- **Then** the returned CompanyDto has contactCount=2 and commentCount=3

### getById -- throws 404 for nonexistent ID

- **Given** no company exists with the given UUID
- **When** `getById(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### update -- updates all fields

- **Given** an existing company with name "Old Name"
- **When** `update(id, dto)` is called with name "New Name" and city "Munich"
- **Then** the returned CompanyDto has name "New Name" and city "Munich"

### update -- throws 404 for nonexistent ID

- **Given** no company exists with the given UUID
- **When** `update(randomUUID, dto)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### delete -- soft-deletes a company without contacts

- **Given** a company with no contacts
- **When** `delete(id)` is called
- **Then** `getById(id)` returns a CompanyDto with deleted=true

### delete -- throws 409 when company has contacts

- **Given** a company with one associated contact
- **When** `delete(companyId)` is called
- **Then** a ResponseStatusException with status 409 (CONFLICT) is thrown
- **And** the company is not soft-deleted

### delete -- throws 404 for nonexistent ID

- **Given** no company exists with the given UUID
- **When** `delete(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### restore -- restores a soft-deleted company

- **Given** a soft-deleted company
- **When** `restore(id)` is called
- **Then** the returned CompanyDto has deleted=false

### restore -- is idempotent for non-deleted company

- **Given** a company with deleted=false
- **When** `restore(id)` is called
- **Then** the returned CompanyDto still has deleted=false

### restore -- throws 404 for nonexistent ID

- **Given** no company exists with the given UUID
- **When** `restore(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### list -- excludes soft-deleted by default

- **Given** 2 active companies and 1 soft-deleted company
- **When** `list(null, null, null, false, pageable)` is called
- **Then** the result page contains exactly 2 companies

### list -- includes soft-deleted when includeDeleted is true

- **Given** 2 active companies and 1 soft-deleted company
- **When** `list(null, null, null, true, pageable)` is called
- **Then** the result page contains exactly 3 companies

### list -- filters by name (partial, case-insensitive)

- **Given** companies named "Open Elements" and "Acme Corp"
- **When** `list("open", null, null, false, pageable)` is called
- **Then** the result page contains only "Open Elements"

### list -- filters by city (exact, case-insensitive)

- **Given** companies in cities "Berlin" and "Munich"
- **When** `list(null, "berlin", null, false, pageable)` is called
- **Then** the result page contains only the Berlin company

### list -- filters by country (exact, case-insensitive)

- **Given** companies in countries "Germany" and "Austria"
- **When** `list(null, null, "germany", false, pageable)` is called
- **Then** the result page contains only the Germany company

### uploadLogo -- stores logo data

- **Given** an existing company
- **When** `uploadLogo(id, pngBytes, "image/png")` is called
- **Then** `getLogo(id)` returns ImageData with the same bytes and content type "image/png"

### uploadLogo -- rejects invalid content type

- **Given** an existing company
- **When** `uploadLogo(id, gifBytes, "image/gif")` is called
- **Then** a ResponseStatusException with status 400 is thrown

### uploadLogo -- throws 404 for nonexistent company

- **Given** no company exists with the given UUID
- **When** `uploadLogo(randomUUID, bytes, "image/png")` is called
- **Then** a ResponseStatusException with status 404 is thrown

### getLogo -- throws 404 when no logo exists

- **Given** a company without a logo
- **When** `getLogo(id)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### deleteLogo -- removes logo data

- **Given** a company with an uploaded logo
- **When** `deleteLogo(id)` is called
- **Then** `getLogo(id)` throws 404

---

# Contact Module

## ContactDto.fromEntity

### Maps all scalar fields from entity

- **Given** a ContactEntity with id, firstName, lastName, email, position, gender=MALE, linkedInUrl, phoneNumber, birthday, syncedToBrevo=true, doubleOptIn=false, language=DE, createdAt, updatedAt all set
- **When** `ContactDto.fromEntity(entity, 7)` is called
- **Then** the returned ContactDto has every field matching the entity and commentCount=7

### Resolves company name and deleted status when company is set

- **Given** a ContactEntity with a company (name="Acme Corp", deleted=false)
- **When** `ContactDto.fromEntity(entity, 0)` is called
- **Then** the returned ContactDto has companyId matching the company's id, companyName="Acme Corp", companyDeleted=false

### Sets company fields to null/false when no company is set

- **Given** a ContactEntity with company=null
- **When** `ContactDto.fromEntity(entity, 0)` is called
- **Then** the returned ContactDto has companyId=null, companyName=null, companyDeleted=false

### Sets hasPhoto to true when photo bytes are present

- **Given** a ContactEntity with `photo` set to a non-null byte array
- **When** `ContactDto.fromEntity(entity, 0)` is called
- **Then** the returned ContactDto has `hasPhoto=true`

### Sets hasPhoto to false when photo bytes are null

- **Given** a ContactEntity with `photo` set to null
- **When** `ContactDto.fromEntity(entity, 0)` is called
- **Then** the returned ContactDto has `hasPhoto=false`

## ContactRepository

### Saves and retrieves a contact

- **Given** a ContactEntity with firstName "John", lastName "Doe"
- **When** the entity is persisted and then retrieved by ID
- **Then** the retrieved entity has firstName "John", lastName "Doe"

### Rejects a contact without firstName (NOT NULL constraint)

- **Given** a ContactEntity with firstName=null and lastName="Doe"
- **When** the entity is persisted and flushed
- **Then** an exception is thrown

### Rejects a contact without lastName (NOT NULL constraint)

- **Given** a ContactEntity with firstName="John" and lastName=null
- **When** the entity is persisted and flushed
- **Then** an exception is thrown

### existsByCompanyId returns true when contacts reference the company

- **Given** a company and a contact associated with that company
- **When** `existsByCompanyId(companyId)` is called
- **Then** the result is true

### existsByCompanyId returns false when no contacts reference the company

- **Given** a company with no associated contacts
- **When** `existsByCompanyId(companyId)` is called
- **Then** the result is false

### countByCompanyId returns correct count

- **Given** a company with 3 associated contacts
- **When** `countByCompanyId(companyId)` is called
- **Then** the result is 3

### countByCompanyId returns zero for company with no contacts

- **Given** a company with no contacts
- **When** `countByCompanyId(companyId)` is called
- **Then** the result is 0

## ContactService

### create -- creates a contact without company

- **Given** a ContactCreateDto with firstName "Jane", lastName "Doe", companyId=null
- **When** `create(dto)` is called
- **Then** the returned ContactDto has firstName "Jane", lastName "Doe", companyId=null, commentCount=0

### create -- creates a contact with a valid company

- **Given** an active company and a ContactCreateDto with that company's id
- **When** `create(dto)` is called
- **Then** the returned ContactDto has companyId matching the company and companyName set

### create -- throws 400 for nonexistent company ID

- **Given** a ContactCreateDto with a random UUID as companyId
- **When** `create(dto)` is called
- **Then** a ResponseStatusException with status 400 is thrown

### create -- throws 400 for soft-deleted company

- **Given** a soft-deleted company and a ContactCreateDto referencing it
- **When** `create(dto)` is called
- **Then** a ResponseStatusException with status 400 is thrown

### getById -- returns contact with comment count

- **Given** a contact with 2 comments
- **When** `getById(contactId)` is called
- **Then** the returned ContactDto has commentCount=2

### getById -- throws 404 for nonexistent ID

- **Given** no contact exists with the given UUID
- **When** `getById(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### update -- updates all fields

- **Given** an existing contact with firstName "Old"
- **When** `update(id, dto)` is called with firstName "New"
- **Then** the returned ContactDto has firstName "New"

### update -- rejects reassignment to soft-deleted company

- **Given** an existing contact and a soft-deleted company
- **When** `update(contactId, dtoWithSoftDeletedCompanyId)` is called
- **Then** a ResponseStatusException with status 400 is thrown

### update -- throws 404 for nonexistent contact ID

- **Given** no contact exists with the given UUID
- **When** `update(randomUUID, dto)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### delete -- hard-deletes a contact

- **Given** an existing contact
- **When** `delete(contactId)` is called
- **Then** `getById(contactId)` throws 404

### delete -- cascades deletion to comments

- **Given** a contact with 3 comments
- **When** `delete(contactId)` is called
- **Then** the contact and all 3 comments are deleted from the database

### delete -- throws 404 for nonexistent ID

- **Given** no contact exists with the given UUID
- **When** `delete(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### list -- returns all contacts when no filters applied

- **Given** 3 contacts
- **When** `list(null, null, null, null, null, pageable)` is called
- **Then** the result page contains 3 contacts

### list -- filters by firstName (partial, case-insensitive)

- **Given** contacts with firstNames "Alice" and "Bob"
- **When** `list("ali", null, null, null, null, pageable)` is called
- **Then** the result page contains only "Alice"

### list -- filters by lastName (partial, case-insensitive)

- **Given** contacts with lastNames "Smith" and "Jones"
- **When** `list(null, "smi", null, null, null, pageable)` is called
- **Then** the result page contains only "Smith"

### list -- filters by email (partial, case-insensitive)

- **Given** contacts with emails "alice@test.com" and "bob@test.com"
- **When** `list(null, null, "alice", null, null, pageable)` is called
- **Then** the result page contains only the Alice contact

### list -- filters by companyId

- **Given** 2 contacts for company A and 1 contact for company B
- **When** `list(null, null, null, companyAId, null, pageable)` is called
- **Then** the result page contains exactly 2 contacts

### list -- filters by language

- **Given** a contact with language=DE and a contact with language=EN
- **When** `list(null, null, null, null, DE, pageable)` is called
- **Then** the result page contains only the DE contact

### list -- filters by language UNKNOWN to find null-language contacts

- **Given** a contact with language=null and a contact with language=EN
- **When** `list(null, null, null, null, UNKNOWN, pageable)` is called
- **Then** the result page contains only the contact with null language

### uploadPhoto -- stores photo data (JPEG only)

- **Given** an existing contact
- **When** `uploadPhoto(id, jpegBytes, "image/jpeg")` is called
- **Then** `getPhoto(id)` returns ImageData with the same bytes and content type "image/jpeg"

### uploadPhoto -- rejects non-JPEG content type

- **Given** an existing contact
- **When** `uploadPhoto(id, pngBytes, "image/png")` is called
- **Then** a ResponseStatusException with status 400 is thrown

### uploadPhoto -- throws 404 for nonexistent contact

- **Given** no contact exists with the given UUID
- **When** `uploadPhoto(randomUUID, bytes, "image/jpeg")` is called
- **Then** a ResponseStatusException with status 404 is thrown

### getPhoto -- throws 404 when no photo exists

- **Given** a contact without a photo
- **When** `getPhoto(id)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### deletePhoto -- removes photo data

- **Given** a contact with an uploaded photo
- **When** `deletePhoto(id)` is called
- **Then** `getPhoto(id)` throws 404

---

# Comment Module

## CommentDto.fromEntity

### Maps all fields for a company comment

- **Given** a CommentEntity with id, text="Great company", author="UNKNOWN", company set (with id), contact=null, createdAt, updatedAt
- **When** `CommentDto.fromEntity(entity)` is called
- **Then** the returned CommentDto has text="Great company", author="UNKNOWN", companyId matching the company's id, contactId=null

### Maps all fields for a contact comment

- **Given** a CommentEntity with company=null, contact set (with id)
- **When** `CommentDto.fromEntity(entity)` is called
- **Then** the returned CommentDto has companyId=null and contactId matching the contact's id

### Handles entity with both company and contact null

- **Given** a CommentEntity with both company=null and contact=null
- **When** `CommentDto.fromEntity(entity)` is called
- **Then** the returned CommentDto has companyId=null and contactId=null

## CommentRepository

### Saves and retrieves a comment

- **Given** a CommentEntity with text "Hello", author "UNKNOWN", attached to a company
- **When** the entity is persisted and then retrieved by ID
- **Then** the retrieved entity has text "Hello" and author "UNKNOWN"

### Rejects a comment without text (NOT NULL constraint)

- **Given** a CommentEntity with text=null
- **When** the entity is persisted and flushed
- **Then** an exception is thrown

### Rejects a comment without author (NOT NULL constraint)

- **Given** a CommentEntity with author=null
- **When** the entity is persisted and flushed
- **Then** an exception is thrown

### findByCompanyId returns comments for the given company

- **Given** 3 comments for company A and 2 comments for company B
- **When** `findByCompanyId(companyAId, Pageable.unpaged())` is called
- **Then** the result page contains exactly 3 comments

### findByCompanyId returns empty page for company with no comments

- **Given** a company with no comments
- **When** `findByCompanyId(companyId, Pageable.unpaged())` is called
- **Then** the result page is empty

### findByContactId returns comments for the given contact

- **Given** 2 comments for contact A
- **When** `findByContactId(contactAId, Pageable.unpaged())` is called
- **Then** the result page contains exactly 2 comments

### deleteByContactId removes all comments for the contact

- **Given** 3 comments for contact A and 2 comments for contact B
- **When** `deleteByContactId(contactAId)` is called
- **Then** `findByContactId(contactAId, Pageable.unpaged())` returns an empty page
- **And** `findByContactId(contactBId, Pageable.unpaged())` still contains 2 comments

### countByCompanyId returns correct count

- **Given** a company with 4 comments
- **When** `countByCompanyId(companyId)` is called
- **Then** the result is 4

### countByContactId returns correct count

- **Given** a contact with 2 comments
- **When** `countByContactId(contactId)` is called
- **Then** the result is 2

### findByCompanyId respects pagination

- **Given** 5 comments for a company
- **When** `findByCompanyId(companyId, PageRequest.of(0, 2))` is called
- **Then** the result page has 2 elements, totalElements=5, totalPages=3

## CommentService

### addToCompany -- creates a comment on a company

- **Given** an existing company and a CommentCreateDto with text "Nice company"
- **When** `addToCompany(companyId, dto)` is called
- **Then** the returned CommentDto has text "Nice company", author "UNKNOWN", companyId matching the company, contactId=null

### addToCompany -- throws 404 for nonexistent company

- **Given** no company exists with the given UUID
- **When** `addToCompany(randomUUID, dto)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### addToCompany -- allows commenting on soft-deleted company

- **Given** a soft-deleted company
- **When** `addToCompany(companyId, dto)` is called
- **Then** the comment is created successfully

### addToContact -- creates a comment on a contact

- **Given** an existing contact and a CommentCreateDto with text "Met at conference"
- **When** `addToContact(contactId, dto)` is called
- **Then** the returned CommentDto has text "Met at conference", contactId matching the contact, companyId=null

### addToContact -- throws 404 for nonexistent contact

- **Given** no contact exists with the given UUID
- **When** `addToContact(randomUUID, dto)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### update -- updates comment text

- **Given** an existing comment with text "Original"
- **When** `update(commentId, CommentUpdateDto("Updated"))` is called
- **Then** the returned CommentDto has text "Updated"

### update -- throws 404 for nonexistent comment

- **Given** no comment exists with the given UUID
- **When** `update(randomUUID, dto)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### delete -- removes comment

- **Given** an existing comment
- **When** `delete(commentId)` is called
- **Then** calling `listByCompany` or `listByContact` no longer includes that comment

### delete -- throws 404 for nonexistent comment

- **Given** no comment exists with the given UUID
- **When** `delete(randomUUID)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### listByCompany -- returns paginated comments for a company

- **Given** a company with 3 comments
- **When** `listByCompany(companyId, PageRequest.of(0, 10))` is called
- **Then** the result page contains 3 CommentDto entries

### listByCompany -- throws 404 for nonexistent company

- **Given** no company exists with the given UUID
- **When** `listByCompany(randomUUID, pageable)` is called
- **Then** a ResponseStatusException with status 404 is thrown

### listByContact -- returns paginated comments for a contact

- **Given** a contact with 2 comments
- **When** `listByContact(contactId, PageRequest.of(0, 10))` is called
- **Then** the result page contains 2 CommentDto entries

### listByContact -- throws 404 for nonexistent contact

- **Given** no contact exists with the given UUID
- **When** `listByContact(randomUUID, pageable)` is called
- **Then** a ResponseStatusException with status 404 is thrown
