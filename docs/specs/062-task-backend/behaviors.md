# Behaviors: Task Entity Backend

## Create Task

### Create task for a company

- **Given** a company exists
- **When** a POST request is sent to `/api/tasks` with action, dueDate, and companyId
- **Then** the task is created with status OPEN
- **Then** the response contains the task with companyId, companyName, and status 201

### Create task for a contact

- **Given** a contact exists
- **When** a POST request is sent to `/api/tasks` with action, dueDate, and contactId
- **Then** the task is created with status OPEN
- **Then** the response contains the task with contactId, contactName, and status 201

### Create task with explicit status

- **Given** a company exists
- **When** a POST request is sent with action, dueDate, companyId, and status IN_PROGRESS
- **Then** the task is created with status IN_PROGRESS

### Create task with tags

- **Given** a company exists and two tags exist
- **When** a POST request is sent with action, dueDate, companyId, and tagIds containing both tag IDs
- **Then** the task is created with both tags assigned
- **Then** the response contains the tagIds

### Create task without action fails

- **Given** a company exists
- **When** a POST request is sent without action (or blank action)
- **Then** the response is 400 Bad Request

### Create task without dueDate fails

- **Given** a company exists
- **When** a POST request is sent without dueDate
- **Then** the response is 400 Bad Request

### Create task without owner fails

- **Given** neither companyId nor contactId is provided
- **When** a POST request is sent to `/api/tasks`
- **Then** the response is 400 Bad Request

### Create task with both owners fails

- **Given** both companyId and contactId are provided
- **When** a POST request is sent to `/api/tasks`
- **Then** the response is 400 Bad Request

### Create task with non-existent company fails

- **Given** the provided companyId does not exist
- **When** a POST request is sent to `/api/tasks`
- **Then** the response is 404 Not Found

### Create task with non-existent contact fails

- **Given** the provided contactId does not exist
- **When** a POST request is sent to `/api/tasks`
- **Then** the response is 404 Not Found

### Create task with invalid tag ID fails

- **Given** a company exists but one of the provided tagIds does not exist
- **When** a POST request is sent to `/api/tasks`
- **Then** the response is 400 Bad Request

## Read Task

### Get task by ID

- **Given** a task exists
- **When** a GET request is sent to `/api/tasks/{id}`
- **Then** the response contains the full task DTO with status 200

### Get non-existent task

- **Given** the task ID does not exist
- **When** a GET request is sent to `/api/tasks/{id}`
- **Then** the response is 404 Not Found

## Update Task

### Update task action and due date

- **Given** a task exists
- **When** a PUT request is sent to `/api/tasks/{id}` with new action and dueDate
- **Then** the task is updated
- **Then** the response contains the updated values

### Update task status

- **Given** a task exists with status OPEN
- **When** a PUT request is sent with status DONE
- **Then** the task status changes to DONE

### Update task tags

- **Given** a task exists with one tag
- **When** a PUT request is sent with tagIds containing a different tag
- **Then** the old tag is removed and the new tag is assigned

### Update task with null tagIds preserves tags

- **Given** a task exists with two tags
- **When** a PUT request is sent with tagIds = null
- **Then** the existing tags are preserved unchanged

### Update task with empty tagIds removes all tags

- **Given** a task exists with two tags
- **When** a PUT request is sent with tagIds = []
- **Then** all tags are removed from the task

### Update does not change owner

- **Given** a task belongs to a company
- **When** a PUT request is sent to update the task
- **Then** the task still belongs to the same company (companyId is immutable on update)

### Update non-existent task

- **Given** the task ID does not exist
- **When** a PUT request is sent to `/api/tasks/{id}`
- **Then** the response is 404 Not Found

### Update task with blank action fails

- **Given** a task exists
- **When** a PUT request is sent with blank action
- **Then** the response is 400 Bad Request

## Delete Task

### Delete task

- **Given** a task exists
- **When** a DELETE request is sent to `/api/tasks/{id}`
- **Then** the task is deleted
- **Then** the response is 204 No Content

### Delete non-existent task

- **Given** the task ID does not exist
- **When** a DELETE request is sent to `/api/tasks/{id}`
- **Then** the response is 404 Not Found

## List Tasks

### List all tasks sorted by due date

- **Given** three tasks exist with different due dates
- **When** a GET request is sent to `/api/tasks`
- **Then** the tasks are returned sorted by dueDate ascending (earliest first)
- **Then** the response is paginated

### List tasks filtered by status

- **Given** tasks exist with status OPEN, IN_PROGRESS, and DONE
- **When** a GET request is sent to `/api/tasks?status=OPEN`
- **Then** only tasks with status OPEN are returned

### List tasks filtered by tags

- **Given** tasks exist with different tags
- **When** a GET request is sent to `/api/tasks?tagIds=<tagId1>,<tagId2>`
- **Then** only tasks with at least one of the specified tags are returned

### List tasks with combined filters

- **Given** tasks exist with various statuses and tags
- **When** a GET request is sent with both status and tagIds filters
- **Then** only tasks matching both filters are returned

### List tasks with pagination

- **Given** 25 tasks exist
- **When** a GET request is sent to `/api/tasks?page=0&size=10`
- **Then** the first 10 tasks are returned with pagination metadata

### List tasks empty result

- **Given** no tasks exist
- **When** a GET request is sent to `/api/tasks`
- **Then** an empty page is returned

## Cascade Behavior

### Contact hard-delete cascades tasks

- **Given** a contact has three tasks
- **When** the contact is deleted (hard delete)
- **Then** all three tasks are cascade-deleted

### Company soft-delete preserves tasks

- **Given** a company has two tasks
- **When** the company is soft-deleted (archived)
- **Then** both tasks are preserved

### Tag deletion removes tag from tasks

- **Given** a task has two tags
- **When** one of the tags is deleted
- **Then** the task retains the other tag
- **Then** the deleted tag is removed from the task's tag list

## Task on Soft-Deleted Company

### Create task for soft-deleted company

- **Given** a company is soft-deleted (archived)
- **When** a POST request is sent to create a task for that company
- **Then** the task is created successfully (consistent with comment behavior)

### List includes tasks of soft-deleted companies

- **Given** a task belongs to a soft-deleted company
- **When** a GET request is sent to `/api/tasks`
- **Then** the task is included in the results
