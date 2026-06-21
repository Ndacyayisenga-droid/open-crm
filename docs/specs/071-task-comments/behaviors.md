# Behaviors: Task Comments

## Create Comment on Task

### Happy path — add comment to task

- **Given** a task exists
- **When** the user opens the task detail view, clicks "Add Comment", enters text, and submits
- **Then** the comment is created with the entered text and the current user as author
- **And** the comment appears at the top of the comment list
- **And** the comment count in the heading increments by 1

### Author set from authenticated user

- **Given** the user is logged in as "Max Mustermann"
- **When** the user creates a comment on a task
- **Then** the comment's author is "Max Mustermann" regardless of any client-side data

### Empty text rejected

- **Given** a task exists
- **When** the user tries to submit a comment with blank text
- **Then** the submit button is disabled
- **And** no API call is made

### Comment on nonexistent task

- **Given** no task exists with the given ID
- **When** `POST /api/tasks/{id}/comments` is called
- **Then** the API returns 404 Not Found

## List Comments on Task

### Happy path — display comments

- **Given** a task has 3 comments
- **When** the user opens the task detail view
- **Then** the comments are displayed sorted by creation date descending (newest first)
- **And** each comment shows author, date, and text

### Empty state — no comments

- **Given** a task has no comments
- **When** the user opens the task detail view
- **Then** the comment section shows "Keine Kommentare vorhanden" / "No comments yet"
- **And** the "Add Comment" button is still available

### Pagination — load more

- **Given** a task has more than 20 comments
- **When** the user opens the task detail view
- **Then** the first 20 comments are displayed
- **And** a "Load more" button is shown
- **When** the user clicks "Load more"
- **Then** the next page of comments is appended to the list

### Comment count in heading

- **Given** a task has 5 comments
- **When** the user opens the task detail view
- **Then** the comment section heading shows "Kommentare (5)" / "Comments (5)"

### List comments for nonexistent task

- **Given** no task exists with the given ID
- **When** `GET /api/tasks/{id}/comments` is called
- **Then** the API returns 404 Not Found

## Delete Comment on Task

### Happy path — delete comment

- **Given** a task detail view with 3 comments
- **When** the user clicks the delete button on a comment and confirms
- **Then** the comment is permanently removed
- **And** the comment disappears from the list
- **And** the comment count in the heading decrements by 1

### Cancel delete

- **Given** a task detail view with comments
- **When** the user clicks the delete button on a comment but cancels the confirmation dialog
- **Then** the comment remains unchanged

### Delete nonexistent comment

- **Given** a comment ID that does not exist
- **When** `DELETE /api/comments/{id}` is called
- **Then** the API returns 404 Not Found

## Task Deletion Cascades to Comments

### Comments deleted when task is deleted

- **Given** a task has 3 comments
- **When** the task is deleted
- **Then** all 3 comments are permanently removed from the database

### Task without comments can be deleted

- **Given** a task has no comments
- **When** the task is deleted
- **Then** the task is permanently removed without error

## Data Model — XOR Constraint

### Comment linked to task only

- **Given** a comment is created for a task
- **When** the comment is stored in the database
- **Then** `task_id` is NOT NULL, `company_id` is NULL, and `contact_id` is NULL

### Constraint prevents multiple owners

- **Given** the database CHECK constraint
- **When** an INSERT is attempted with both `task_id` and `company_id` set
- **Then** the database rejects the insert with a constraint violation

### Constraint prevents no owner

- **Given** the database CHECK constraint
- **When** an INSERT is attempted with all three IDs set to NULL
- **Then** the database rejects the insert with a constraint violation

### Existing company/contact comments unaffected

- **Given** existing comments linked to companies and contacts
- **When** the migration adds `task_id` and updates the CHECK constraint
- **Then** all existing comments remain valid with `task_id = NULL`

## API Endpoints

### GET /api/tasks/{id}/comments

- **Given** a task with comments
- **When** `GET /api/tasks/{id}/comments` is called
- **Then** the response contains a paginated list of `CommentDto` objects
- **And** each DTO includes id, text, author, taskId, createdAt, updatedAt
- **And** companyId and contactId are NULL

### POST /api/tasks/{id}/comments

- **Given** a task exists
- **When** `POST /api/tasks/{id}/comments` is called with `{ "text": "Some comment" }`
- **Then** the response is 201 Created with the new `CommentDto`
- **And** the `taskId` field matches the task ID from the URL

### POST with blank text rejected

- **Given** a task exists
- **When** `POST /api/tasks/{id}/comments` is called with `{ "text": "" }`
- **Then** the response is 400 Bad Request

## TaskDto Comment Count

### Comment count included in TaskDto

- **Given** a task with 5 comments
- **When** `GET /api/tasks/{id}` is called
- **Then** the response includes `commentCount: 5`

### New task has zero comment count

- **Given** a newly created task
- **When** `GET /api/tasks/{id}` is called
- **Then** the response includes `commentCount: 0`
