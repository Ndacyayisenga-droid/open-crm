# Implementation Steps: OpenAPI Parameter Docs

## Step 1: Add @Parameter annotations to all controllers

- [x] CompanyController: 13 @Parameter annotations
- [x] ContactController: 14 @Parameter annotations (search instead of firstName/lastName/email)
- [x] CommentController: 2 @Parameter annotations
- [x] BrevoSyncController: no params to annotate
- [x] Pageable params: hidden = true

**Acceptance criteria:**
- [x] Backend: 259 tests pass (pure annotation change)
