# Behaviors: Translation Action for Descriptions and Comments

## Configuration / Feature Detection

### Translation configured — buttons visible

- **Given** the environment variables `TRANSLATION_API_URL`, `TRANSLATION_API_KEY`, and `TRANSLATION_MODEL` are all set
- **When** a user opens a company, contact, or task detail page
- **Then** translate buttons are visible on non-empty descriptions and on all comments with non-empty text

### Translation not configured — buttons hidden

- **Given** one or more of the translation environment variables are missing
- **When** a user opens a company, contact, or task detail page
- **Then** no translate buttons are shown anywhere on the page

### Translation not configured — warning logged

- **Given** one or more of the translation environment variables are missing
- **When** the backend application starts
- **Then** a warning is logged indicating that translation is not configured

### Settings endpoint returns configuration status

- **Given** all three translation environment variables are set
- **When** `GET /api/translate/settings` is called
- **Then** the response is `200 OK` with body `{ "configured": true }`

### Settings endpoint returns unconfigured status

- **Given** one or more translation environment variables are missing
- **When** `GET /api/translate/settings` is called
- **Then** the response is `200 OK` with body `{ "configured": false }`

## Translate Button Visibility

### Description with text — button shown

- **Given** translation is configured and a company/contact has a non-empty description
- **When** the detail page is displayed
- **Then** a translate icon button appears next to the description text

### Description empty — button hidden

- **Given** translation is configured and a company/contact has no description (null or empty)
- **When** the detail page is displayed
- **Then** no translate button is shown for the description

### Description whitespace only — button hidden

- **Given** translation is configured and a company/contact description contains only whitespace
- **When** the detail page is displayed
- **Then** no translate button is shown for the description

### Comment with text — button shown

- **Given** translation is configured and a comment has non-empty text
- **When** the comment is displayed on any detail page (company, contact, task)
- **Then** a translate icon button appears next to the delete button

### Comment with empty text — button hidden

- **Given** translation is configured and a comment has empty text
- **When** the comment is displayed
- **Then** no translate button is shown for that comment

## Translation Flow — Happy Path

### Translate description to English

- **Given** the UI language is set to English and a company description contains German text
- **When** the user clicks the translate button on the description
- **Then** a dialog opens showing a loading state, followed by the English translation of the description text

### Translate description to German

- **Given** the UI language is set to German and a contact description contains English text
- **When** the user clicks the translate button on the description
- **Then** a dialog opens showing a loading state, followed by the German translation of the description text

### Translate comment to current language

- **Given** the UI language is set to English and a comment contains German text
- **When** the user clicks the translate button on the comment
- **Then** a dialog opens showing a loading state, followed by the English translation of the comment text

### Text already in target language

- **Given** the UI language is set to German and a description is already in German
- **When** the user clicks the translate button
- **Then** the dialog shows the (unchanged or minimally rephrased) text — no error occurs

### Translation preserves line breaks

- **Given** a description or comment contains multiple lines of text
- **When** the user translates it
- **Then** the translated text in the dialog preserves the line break structure

## Translation Dialog

### Dialog shows loading state

- **Given** the user clicked the translate button
- **When** the API call is in progress
- **Then** the dialog is open and displays a loading indicator

### Dialog shows translated text

- **Given** the API call completed successfully
- **When** the dialog is displayed
- **Then** the translated text is shown with preserved line breaks (whitespace-pre-line)

### Copy to clipboard

- **Given** the dialog shows a translated text
- **When** the user clicks the copy button
- **Then** the translated text is copied to the clipboard and the button shows a check icon as feedback

### Close dialog

- **Given** the translation dialog is open
- **When** the user clicks the close button
- **Then** the dialog closes

## Error Cases

### API call fails — error in dialog

- **Given** the translation API returns an error (e.g., 500, timeout, network error)
- **When** the user clicks the translate button
- **Then** the dialog shows an error message instead of the translated text

### Translation not configured — POST returns 503

- **Given** translation is not configured on the backend
- **When** `POST /api/translate` is called
- **Then** the response is `503 Service Unavailable`

### Invalid target language — POST returns 400

- **Given** the request body contains a `targetLanguage` that is neither `de` nor `en`
- **When** `POST /api/translate` is called
- **Then** the response is `400 Bad Request`

### Empty text — POST returns 400

- **Given** the request body contains an empty or blank `text` field
- **When** `POST /api/translate` is called
- **Then** the response is `400 Bad Request`

### Unauthenticated request — returns 401

- **Given** no valid authentication token is provided
- **When** `GET /api/translate/settings` or `POST /api/translate` is called
- **Then** the response is `401 Unauthorized`

## All Entity Types Covered

### Company description translatable

- **Given** a company has a non-empty description and translation is configured
- **When** the company detail page is opened
- **Then** the translate button is visible on the description

### Contact description translatable

- **Given** a contact has a non-empty description and translation is configured
- **When** the contact detail page is opened
- **Then** the translate button is visible on the description

### Company comment translatable

- **Given** a company has comments and translation is configured
- **When** the company detail page is opened
- **Then** each comment with non-empty text has a translate button

### Contact comment translatable

- **Given** a contact has comments and translation is configured
- **When** the contact detail page is opened
- **Then** each comment with non-empty text has a translate button

### Task comment translatable

- **Given** a task has comments and translation is configured
- **When** the task detail page is opened
- **Then** each comment with non-empty text has a translate button
