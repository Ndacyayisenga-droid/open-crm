# Behaviors: Comment Modal Dialog

## Opening the Modal

### Add Comment button is visible in comment section header

- **Given** the user is on a company or contact detail page
- **When** the comment section renders
- **Then** an "Add Comment" button is displayed next to the section title "Kommentare"

### Clicking the button opens the modal

- **Given** the user is on a detail page with the comment section visible
- **When** the user clicks the "Add Comment" button
- **Then** a modal dialog opens with a textarea, a submit button, and a title

### Inline textarea is no longer shown

- **Given** the user is on a company or contact detail page
- **When** the comment section renders
- **Then** there is no inline textarea or send button in the comment section — only the read-only comment list

## Modal Content

### Modal contains textarea with minimum height

- **Given** the modal is open
- **When** the user views the textarea
- **Then** the textarea has a minimum height of approximately 3 rows and is resizable vertically

### Submit button is disabled when text is empty

- **Given** the modal is open
- **When** the textarea is empty or contains only whitespace
- **Then** the submit button is disabled

### Submit button is enabled when text is entered

- **Given** the modal is open
- **When** the user types non-whitespace text into the textarea
- **Then** the submit button becomes enabled

## Keyboard Behavior

### Enter submits the comment

- **Given** the modal is open and the textarea contains non-empty text
- **When** the user presses Enter (without Shift)
- **Then** the comment is submitted and no newline is inserted

### Enter does nothing when text is empty

- **Given** the modal is open and the textarea is empty or contains only whitespace
- **When** the user presses Enter
- **Then** nothing happens — no submission, no newline

### Shift+Enter inserts a newline

- **Given** the modal is open and the textarea is focused
- **When** the user presses Shift+Enter
- **Then** a newline is inserted at the cursor position

### Escape closes the modal

- **Given** the modal is open
- **When** the user presses Escape
- **Then** the modal closes and no comment is submitted

## Successful Submission

### Modal closes after successful submit

- **Given** the modal is open and the user has entered comment text
- **When** the user submits the comment (via button or Enter) and the API responds successfully
- **Then** the modal closes automatically

### Comment list updates immediately

- **Given** the modal was open and a comment was successfully submitted
- **When** the modal closes
- **Then** the new comment appears at the top of the comment list without a page reload

### Textarea is cleared after successful submit

- **Given** a comment was successfully submitted and the modal closed
- **When** the user opens the modal again
- **Then** the textarea is empty

## Error Handling

### Error dialog appears on API failure

- **Given** the modal is open and the user submits a comment
- **When** the API returns an error
- **Then** an error AlertDialog is displayed on top of the modal with an error message

### Modal stays open on error

- **Given** the modal is open and the API returned an error
- **When** the user dismisses the error AlertDialog
- **Then** the comment modal is still open with the entered text preserved

### Text is preserved on error

- **Given** the modal is open and the user entered text
- **When** the API returns an error and the user dismisses the error dialog
- **Then** the previously entered text is still in the textarea, unchanged

## Submitting State

### Submit button shows loading state while sending

- **Given** the modal is open and the user submits a comment
- **When** the API request is in progress
- **Then** the submit button is disabled and shows a sending indicator (e.g., "Sende..." / "Sending...")

### Textarea is not editable while sending

- **Given** the modal is open and a comment submission is in progress
- **When** the user tries to type in the textarea
- **Then** the textarea does not accept input (disabled state)

### Enter key does nothing while sending

- **Given** the modal is open and a submission is in progress
- **When** the user presses Enter
- **Then** nothing happens — no duplicate submission

## Closing Without Submitting

### Closing the modal discards entered text

- **Given** the modal is open and the user has typed text
- **When** the user closes the modal (Escape or clicking outside)
- **Then** the modal closes and the text is discarded

### Reopening the modal shows empty textarea

- **Given** the user previously opened and closed the modal without submitting
- **When** the user opens the modal again
- **Then** the textarea is empty

## Reusability

### Same modal works for company comments

- **Given** the user is on a company detail page
- **When** the user opens the "Add Comment" modal and submits a comment
- **Then** the comment is created via `POST /api/companies/{id}/comments` and appears in the company's comment list

### Same modal works for contact comments

- **Given** the user is on a contact detail page
- **When** the user opens the "Add Comment" modal and submits a comment
- **Then** the comment is created via `POST /api/contacts/{id}/comments` and appears in the contact's comment list

## Internationalization

### Modal title is translated

- **Given** the language is set to German
- **When** the modal opens
- **Then** the title reads "Kommentar hinzufügen"

### Modal title is translated (English)

- **Given** the language is set to English
- **When** the modal opens
- **Then** the title reads "Add Comment"