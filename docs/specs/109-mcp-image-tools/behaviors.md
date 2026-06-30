# Behaviors: MCP image tools (contact photo & company logo)

## Image result mechanism (McpToolSupport)

### Image tool returns an ImageContent result

- **Given** an `imageSpec` tool whose logic returns a non-null `ImageData`
- **When** the tool is invoked successfully
- **Then** the `CallToolResult` has `isError=false` and its content is a single
  `ImageContent` whose `data` is the Base64 encoding of `ImageData.data()` and
  whose `mimeType` equals `ImageData.contentType()`

### Image tool logs one access line without arguments

- **Given** an `imageSpec` tool and a resolved actor label
- **When** the tool is invoked successfully
- **Then** exactly one INFO access line is emitted containing the tool name and
  actor label and **not** the call arguments

### Image tool reuses the shared error mapping

- **Given** an `imageSpec` tool whose logic throws
- **When** the logic throws `IllegalArgumentException` / `NoSuchElementException` /
  `McpUnavailableException` / any other exception
- **Then** the result is, respectively, an invalid-argument / not-found /
  unavailable / generic-internal error result with `isError=true`, matching the
  behavior of text tools

### Image tools do not write an audit-log row

- **Given** any image tool
- **When** it is invoked (successfully or with an error)
- **Then** no row is written to `audit_log` (consistent with all MCP reads)

## get_contact_photo

### Returns the photo of a contact that has one

- **Given** a contact with a stored photo
- **When** `get_contact_photo` is called with that contact's `id`
- **Then** the result is an `ImageContent` with `mimeType` `image/jpeg` and
  Base64 data matching the stored photo bytes, and `isError=false`

### Errors when the contact has no photo

- **Given** a contact that exists but has no photo
- **When** `get_contact_photo` is called with that contact's `id`
- **Then** the result is a not-found error (`isError=true`) whose message
  indicates the contact has no photo

### Errors when the contact does not exist

- **Given** an `id` that matches no contact
- **When** `get_contact_photo` is called with that `id`
- **Then** the result is a not-found error (`isError=true`) whose message
  indicates the contact was not found

### Rejects a missing id

- **Given** no `id` argument (or an empty/blank `id`)
- **When** `get_contact_photo` is called
- **Then** the result is an invalid-argument error (`isError=true`)

### Rejects a malformed id

- **Given** an `id` argument that is not a valid UUID
- **When** `get_contact_photo` is called
- **Then** the result is an invalid-argument error (`isError=true`)

## get_company_logo

### Returns the logo of a company that has one

- **Given** a company with a stored logo of a given content type (e.g. `image/png`)
- **When** `get_company_logo` is called with that company's `id`
- **Then** the result is an `ImageContent` whose `mimeType` equals the stored
  logo content type and whose Base64 data matches the stored logo bytes, and
  `isError=false`

### Preserves the stored (non-JPEG) content type

- **Given** a company whose logo was stored as `image/png` (logos are not
  transcoded)
- **When** `get_company_logo` is called with that company's `id`
- **Then** the returned `ImageContent.mimeType` is `image/png`, not `image/jpeg`

### Errors when the company has no logo

- **Given** a company that exists but has no logo
- **When** `get_company_logo` is called with that company's `id`
- **Then** the result is a not-found error (`isError=true`) whose message
  indicates the company has no logo

### Errors when the company does not exist

- **Given** an `id` that matches no company
- **When** `get_company_logo` is called with that `id`
- **Then** the result is a not-found error (`isError=true`) whose message
  indicates the company was not found

### Rejects a missing or malformed id

- **Given** a missing, empty, or non-UUID `id` argument
- **When** `get_company_logo` is called
- **Then** the result is an invalid-argument error (`isError=true`)

## Tool catalog

### Both tools are advertised

- **Given** the MCP server is enabled
- **When** a client lists the available tools
- **Then** `get_contact_photo` and `get_company_logo` appear in the catalog, each
  with a required `id` (UUID) input property
