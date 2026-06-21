# Behaviors: User Entity

## Auto-Creation on First Request

### User created on first authenticated request

- **Given** a user with OIDC sub "abc-123" has never made a request before
- **When** the user makes any authenticated API request
- **Then** a new UserEntity is created with sub "abc-123", name and email from the JWT
- **Then** the request proceeds normally

### Existing user is reused on subsequent requests

- **Given** a UserEntity with sub "abc-123" already exists
- **When** the same user makes another authenticated API request
- **Then** no new entity is created
- **Then** the existing UserEntity is returned

### Name is updated from JWT

- **Given** a UserEntity exists with name "Max Müller"
- **When** the user logs in with a JWT where the name claim is "Maximilian Müller"
- **Then** the UserEntity's name is updated to "Maximilian Müller"

### Email is updated from JWT

- **Given** a UserEntity exists with email "max@example.com"
- **When** the user logs in with a JWT where the email claim is "max.mueller@example.com"
- **Then** the UserEntity's email is updated to "max.mueller@example.com"

### Name and email unchanged skips update

- **Given** a UserEntity exists with name "Max" and email "max@example.com"
- **When** the user makes a request with the same name and email in the JWT
- **Then** the UserEntity is not modified (no unnecessary DB write)

## GET /api/users/me

### Returns current user info

- **Given** a UserEntity exists for the authenticated user
- **When** a GET request is sent to `/api/users/me`
- **Then** the response contains the user's id, name, email, hasAvatar, createdAt, updatedAt

### hasAvatar is false when no avatar

- **Given** the current user has no avatar
- **When** a GET request is sent to `/api/users/me`
- **Then** `hasAvatar` is `false`

### hasAvatar is true when avatar exists

- **Given** the current user has an avatar uploaded
- **When** a GET request is sent to `/api/users/me`
- **Then** `hasAvatar` is `true`

### Unauthenticated request is rejected

- **Given** no JWT token is provided
- **When** a GET request is sent to `/api/users/me`
- **Then** the response is 401 Unauthorized

## Avatar Upload

### Upload avatar

- **Given** the current user has no avatar
- **When** a PUT request is sent to `/api/users/me/avatar` with a JPEG image file
- **Then** the avatar is stored
- **Then** the response contains the updated UserDto with `hasAvatar: true`

### Replace existing avatar

- **Given** the current user has an existing avatar
- **When** a PUT request is sent with a new image file
- **Then** the old avatar is replaced
- **Then** the new avatar is served on subsequent GET requests

### Upload with invalid content type

- **Given** the user uploads a non-image file (e.g., a PDF)
- **When** the PUT request is processed
- **Then** the response is 400 Bad Request

## Avatar Retrieval

### Get avatar image

- **Given** the current user has an avatar (JPEG)
- **When** a GET request is sent to `/api/users/me/avatar`
- **Then** the response contains the image bytes with `Content-Type: image/jpeg`

### Get avatar when none exists

- **Given** the current user has no avatar
- **When** a GET request is sent to `/api/users/me/avatar`
- **Then** the response is 404 Not Found

## Avatar Deletion

### Delete avatar

- **Given** the current user has an avatar
- **When** a DELETE request is sent to `/api/users/me/avatar`
- **Then** the avatar is removed (set to null)
- **Then** the response is 204 No Content

### Delete avatar when none exists

- **Given** the current user has no avatar
- **When** a DELETE request is sent to `/api/users/me/avatar`
- **Then** the response is 204 No Content (idempotent)

## Sidebar Avatar Display

### Sidebar shows local avatar

- **Given** the current user has an avatar uploaded
- **When** the sidebar is displayed
- **Then** the user's local avatar is shown instead of the OIDC picture

### Sidebar shows fallback icon when no avatar

- **Given** the current user has no avatar
- **When** the sidebar is displayed
- **Then** the CircleUser icon is shown (existing behavior)

### Avatar upload from sidebar

- **Given** the sidebar is displayed
- **When** the user clicks on the avatar/icon area
- **Then** a file picker opens to upload a new avatar

### Avatar updates immediately after upload

- **Given** the user uploads a new avatar via the sidebar
- **When** the upload succeeds
- **Then** the sidebar avatar updates immediately without page reload

## UserInfo Removal

### CommentService uses UserEntity

- **Given** a user creates a comment
- **When** the comment is saved
- **Then** the author is set from `UserEntity.getName()` (not from a transient UserInfo)

## Migration

### Existing system works after migration

- **Given** the database has no users table
- **When** migration V16 runs
- **Then** the users table is created
- **Then** the application starts successfully
- **Then** the first authenticated request creates a UserEntity

## Multiple Users

### Separate entities for different users

- **Given** user A (sub "aaa") and user B (sub "bbb") both make requests
- **When** both users have made at least one request
- **Then** two separate UserEntity records exist
- **Then** each has their own avatar, name, and email

### Sub uniqueness enforced

- **Given** a UserEntity with sub "abc-123" exists
- **When** attempting to create another entity with the same sub
- **Then** the database rejects the duplicate (unique constraint)
