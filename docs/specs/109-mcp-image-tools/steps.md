# Implementation Steps: MCP image tools

## Step 1: Image-result path in `McpToolSupport` (base layer)

- [ ] Add `McpImageLogic` functional interface returning `ImageData`
- [ ] Refactor private `invoke(...)` to share the four catch arms between text and image tools
- [ ] Add public `imageSpec(tool, logic)` factory and private `imageResult(ImageData)` helper (Base64 + `ImageContent`)

**Acceptance criteria:**
- [ ] Text `spec()` behavior unchanged
- [ ] `imageSpec()` success returns a single `ImageContent` (`isError=false`)
- [ ] Error mapping identical for both paths
- [ ] Project builds

**Related behaviors:** Image result mechanism (McpToolSupport)

---

## Step 2: Two CRM tools in `McpToolFactory`

- [ ] `getContactPhotoTool()` via `support.imageSpec`, distinguishing missing contact vs. no photo
- [ ] `getCompanyLogoTool()` likewise
- [ ] Append both to `toolSpecifications()`

**Acceptance criteria:**
- [ ] Catalog grows from 9 to 11 tools
- [ ] Both tools have a required `id` UUID property
- [ ] Project builds

**Related behaviors:** get_contact_photo, get_company_logo, Tool catalog

---

## Step 3: Tests

- [ ] Unit tests in `McpToolSupportTest` for `imageSpec` success + error mapping
- [ ] Integration coverage in `McpEndpointIntegrationTest` (catalog count 11, happy path, no-image, unknown-id, malformed id)

**Acceptance criteria:**
- [ ] All behavioral scenarios have a covering test
- [ ] `mvn test` passes
