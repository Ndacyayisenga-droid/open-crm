# Behaviors: Delete Comments

## Delete Button Display

### Delete icon visible on every comment

- **Given** a company or contact detail view with comments
- **When** the comments are displayed
- **Then** each comment shows a red X icon button

## Confirmation Dialog

### Confirmation dialog appears on click

- **Given** a comment is displayed with a delete icon
- **When** the user clicks the X icon
- **Then** a confirmation dialog appears with title, description, and confirm/cancel buttons

### Cancel aborts deletion

- **Given** the delete confirmation dialog is open
- **When** the user clicks "Cancel"
- **Then** the dialog closes
- **Then** the comment remains unchanged

### Confirm deletes the comment

- **Given** the delete confirmation dialog is open
- **When** the user clicks "Delete"
- **Then** the API call `DELETE /api/comments/{id}` is made
- **Then** the comment is removed from the list
- **Then** the dialog closes

## Comment Count Update

### Count decrements after deletion on company

- **Given** a company detail view shows "3 Kommentare" in the heading
- **When** the user deletes one comment
- **Then** the heading updates to "2 Kommentare"

### Count decrements after deletion on contact

- **Given** a contact detail view shows "5 Kommentare" in the heading
- **When** the user deletes one comment
- **Then** the heading updates to "4 Kommentare"

### Count reaches zero

- **Given** a company or contact has exactly 1 comment
- **When** the user deletes that comment
- **Then** the count updates to 0
- **Then** the comment list is empty

## Company Comments

### Delete comment from company detail view

- **Given** a company detail view with multiple comments
- **When** the user deletes a comment and confirms
- **Then** the comment is removed from the displayed list
- **Then** the remaining comments stay in their order
- **Then** the comment count decrements by 1

### Delete comment created by another user

- **Given** a comment was created by user A
- **When** user B clicks the delete icon and confirms
- **Then** the comment is deleted successfully

## Contact Comments

### Delete comment from contact detail view

- **Given** a contact detail view with multiple comments
- **When** the user deletes a comment and confirms
- **Then** the comment is removed from the displayed list
- **Then** the remaining comments stay in their order
- **Then** the comment count decrements by 1

## Error Handling

### API error during deletion

- **Given** the user confirms comment deletion
- **When** the API call fails (e.g., network error, 500)
- **Then** an error message is displayed
- **Then** the comment is not removed from the list
- **Then** the comment count is not decremented

### Comment already deleted (404)

- **Given** the user confirms comment deletion
- **When** the API returns 404 (comment was already deleted, e.g., by another user)
- **Then** the comment is removed from the local list (it no longer exists anyway)
- **Then** the comment count decrements by 1

## Load More Interaction

### Delete after loading more comments

- **Given** the user has loaded additional comments via "Load more"
- **When** the user deletes a comment from the initially loaded batch
- **Then** only that comment is removed
- **Then** all other loaded comments (including "load more" batch) remain visible

### Delete from loaded-more batch

- **Given** the user has loaded additional comments via "Load more"
- **When** the user deletes a comment from the second batch
- **Then** only that comment is removed
- **Then** all other loaded comments remain visible
