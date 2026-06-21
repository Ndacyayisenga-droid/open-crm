# Behaviors: Tag Task Count

## Count Display

### Task count column visible

- **Given** the user opens the tag list
- **When** the table is rendered
- **Then** a "Tasks" / "Aufgaben" column is shown alongside Companies and Contacts columns

### Tag with active tasks shows count

- **Given** a tag is assigned to 3 tasks with status OPEN and 1 task with status IN_PROGRESS
- **When** the tag list is displayed
- **Then** the task count shows 4

### Tag with only completed tasks shows zero

- **Given** a tag is assigned to 2 tasks, both with status DONE
- **When** the tag list is displayed
- **Then** the task count shows 0

### Tag with mixed status tasks counts only active

- **Given** a tag is assigned to 5 tasks: 2 OPEN, 1 IN_PROGRESS, 2 DONE
- **When** the tag list is displayed
- **Then** the task count shows 3

### Tag with no tasks shows zero

- **Given** a tag has no tasks assigned
- **When** the tag list is displayed
- **Then** the task count shows 0

## Navigation Link

### Count links to filtered task list

- **Given** a tag "Important" (id: abc-123) has 3 active tasks
- **When** the user clicks the task count
- **Then** the browser navigates to `/tasks?tagIds=abc-123`

### Zero count has no link

- **Given** a tag has 0 active tasks
- **When** the tag list is displayed
- **Then** the task count shows 0 without a navigation link

## Column Order

### Task column after contact column

- **Given** the tag list is displayed
- **When** the table columns are rendered
- **Then** the column order is: Name, Description, Color, Companies, Contacts, Tasks, Actions

## Existing Counts Unchanged

### Company count still works

- **Given** a tag is assigned to 5 active companies
- **When** the tag list is displayed
- **Then** the company count still shows 5 with a link to `/companies?tagIds=...`

### Contact count still works

- **Given** a tag is assigned to 3 contacts
- **When** the tag list is displayed
- **Then** the contact count still shows 3 with a link to `/contacts?tagIds=...`

## Task Lifecycle

### Count updates when task status changes to DONE

- **Given** a tag has 3 active tasks
- **When** one task's status is changed to DONE
- **Then** the tag list shows a task count of 2 on next load

### Count updates when task is deleted

- **Given** a tag has 2 active tasks
- **When** one task is deleted
- **Then** the tag list shows a task count of 1 on next load

### Count updates when tag is assigned to a task

- **Given** a tag has 0 tasks
- **When** the tag is assigned to a new task with status OPEN
- **Then** the tag list shows a task count of 1 on next load
