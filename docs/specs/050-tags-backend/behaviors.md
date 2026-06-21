# Behaviors: Tags Backend

## Tag CRUD

### Create a tag

- **Given** no tag with name "VIP" exists
- **When** `POST /api/tags` is called with `{ "name": "VIP", "description": "Important clients", "color": "#E63277" }`
- **Then** a tag is created and returned with status 201
- **And** the response includes `id`, `name`, `description`, `color`, `createdAt`, `updatedAt`

### Create a tag without description

- **Given** no tag with name "Lead" exists
- **When** `POST /api/tags` is called with `{ "name": "Lead", "color": "#5CBA9E" }`
- **Then** a tag is created with description `null`

### Create a tag with duplicate name fails

- **Given** a tag with name "VIP" already exists
- **When** `POST /api/tags` is called with `{ "name": "VIP", "color": "#000000" }`
- **Then** the response status is 409 Conflict

### Create a tag without name fails

- **Given** the API is available
- **When** `POST /api/tags` is called with `{ "color": "#5CBA9E" }`
- **Then** the response status is 400 Bad Request

### Create a tag without color fails

- **Given** the API is available
- **When** `POST /api/tags` is called with `{ "name": "Test" }`
- **Then** the response status is 400 Bad Request

### Get a tag by ID

- **Given** a tag with ID `{id}` exists
- **When** `GET /api/tags/{id}` is called
- **Then** the tag is returned with status 200

### Get a non-existent tag returns 404

- **Given** no tag with ID `{id}` exists
- **When** `GET /api/tags/{id}` is called
- **Then** the response status is 404

### List all tags (paginated)

- **Given** 25 tags exist
- **When** `GET /api/tags` is called without parameters
- **Then** the first page of 20 tags is returned, sorted by name ascending

### Update a tag

- **Given** a tag with ID `{id}` exists with name "VIP"
- **When** `PUT /api/tags/{id}` is called with `{ "name": "Premium", "color": "#5DB9F5" }`
- **Then** the tag is updated and returned with the new values

### Update a tag with duplicate name fails

- **Given** tags "VIP" and "Lead" exist
- **When** `PUT /api/tags/{leadId}` is called with `{ "name": "VIP", "color": "#000000" }`
- **Then** the response status is 409 Conflict

### Update a non-existent tag returns 404

- **Given** no tag with ID `{id}` exists
- **When** `PUT /api/tags/{id}` is called
- **Then** the response status is 404

### Delete a tag

- **Given** a tag with ID `{id}` exists
- **When** `DELETE /api/tags/{id}` is called
- **Then** the tag is permanently deleted
- **And** the response status is 204 or 200

### Delete a non-existent tag returns 404

- **Given** no tag with ID `{id}` exists
- **When** `DELETE /api/tags/{id}` is called
- **Then** the response status is 404

## Tag Assignment via Company

### Create company with tags

- **Given** tags "VIP" and "Lead" exist
- **When** `POST /api/companies` is called with `tagIds: [vipId, leadId]`
- **Then** the company is created with both tags assigned
- **And** the response `tagIds` contains both IDs

### Create company without tags

- **Given** tags exist
- **When** `POST /api/companies` is called without `tagIds` (or with `tagIds: null`)
- **Then** the company is created with no tags
- **And** the response `tagIds` is an empty list

### Update company: add tags

- **Given** a company exists with no tags
- **And** tag "VIP" exists
- **When** `PUT /api/companies/{id}` is called with `tagIds: [vipId]`
- **Then** the company now has tag "VIP" assigned

### Update company: replace tags

- **Given** a company exists with tags "VIP" and "Lead"
- **And** tag "Premium" exists
- **When** `PUT /api/companies/{id}` is called with `tagIds: [premiumId]`
- **Then** the company now has only tag "Premium" (VIP and Lead removed)

### Update company: remove all tags

- **Given** a company exists with tags "VIP" and "Lead"
- **When** `PUT /api/companies/{id}` is called with `tagIds: []`
- **Then** the company has no tags assigned

### Update company: null tagIds preserves existing tags

- **Given** a company exists with tag "VIP"
- **When** `PUT /api/companies/{id}` is called without `tagIds` (or with `tagIds: null`)
- **Then** the company still has tag "VIP" assigned (unchanged)

### Company response includes tagIds

- **Given** a company exists with tags "VIP" and "Lead"
- **When** `GET /api/companies/{id}` is called
- **Then** the response includes `tagIds: [vipId, leadId]`

### Company list response includes tagIds

- **Given** companies exist with various tags
- **When** `GET /api/companies` is called
- **Then** each company in the response includes its `tagIds`

## Tag Assignment via Contact

### Create contact with tags

- **Given** tags "VIP" and "Lead" exist
- **When** `POST /api/contacts` is called with `tagIds: [vipId, leadId]`
- **Then** the contact is created with both tags assigned

### Update contact: null tagIds preserves existing tags

- **Given** a contact exists with tag "VIP"
- **When** `PUT /api/contacts/{id}` is called without `tagIds` (or with `tagIds: null`)
- **Then** the contact still has tag "VIP" assigned (unchanged)

### Update contact: empty list removes all tags

- **Given** a contact exists with tags
- **When** `PUT /api/contacts/{id}` is called with `tagIds: []`
- **Then** the contact has no tags assigned

### Contact response includes tagIds

- **Given** a contact exists with tag "Lead"
- **When** `GET /api/contacts/{id}` is called
- **Then** the response includes `tagIds: [leadId]`

## Cascade Delete

### Deleting a tag removes company assignments

- **Given** tag "VIP" is assigned to 3 companies
- **When** `DELETE /api/tags/{vipId}` is called
- **Then** the tag is deleted
- **And** the 3 companies no longer have "VIP" in their `tagIds`
- **And** the companies themselves still exist

### Deleting a tag removes contact assignments

- **Given** tag "VIP" is assigned to 2 contacts
- **When** `DELETE /api/tags/{vipId}` is called
- **Then** the tag is deleted
- **And** the 2 contacts no longer have "VIP" in their `tagIds`

### Deleting a company removes its tag assignments

- **Given** a company has tags "VIP" and "Lead"
- **When** the company is deleted
- **Then** the entries in `company_tags` for this company are removed
- **And** the tags "VIP" and "Lead" still exist

### Deleting a contact removes its tag assignments

- **Given** a contact has tag "Lead"
- **When** the contact is deleted
- **Then** the entry in `contact_tags` for this contact is removed
- **And** the tag "Lead" still exists

## Invalid Tag IDs

### Create company with non-existent tag ID

- **Given** no tag with ID `{fakeId}` exists
- **When** `POST /api/companies` is called with `tagIds: [fakeId]`
- **Then** the response status is 400 Bad Request

### Update contact with non-existent tag ID

- **Given** no tag with ID `{fakeId}` exists
- **When** `PUT /api/contacts/{id}` is called with `tagIds: [fakeId]`
- **Then** the response status is 400 Bad Request

## Backward Compatibility

### Existing frontend can update company without tagIds

- **Given** a company exists with tag "VIP"
- **When** the existing frontend sends a PUT request without a `tagIds` field
- **Then** the company's tags remain unchanged (VIP still assigned)

### Existing frontend can create company without tagIds

- **Given** the existing frontend sends a POST request without a `tagIds` field
- **When** the company is created
- **Then** the company has no tags (empty list)
