# Behaviors: Comment Count Live Update

## Company Comments

### Count updates after adding a comment

- **Given** the company detail page is open and the heading shows "Comments (3)"
- **When** the user adds a comment and the API call succeeds
- **Then** the heading shows "Comments (4)" without a page reload

### Count stays unchanged on API failure

- **Given** the company detail page is open and the heading shows "Comments (3)"
- **When** the user adds a comment and the API call fails
- **Then** the heading still shows "Comments (3)"

### Count displays correctly when totalCount is undefined

- **Given** the company detail page is open and no `totalCount` was provided
- **When** the user adds a comment and the API call succeeds
- **Then** the heading shows "Comments" with no count in parentheses (unchanged)

### Count resets when navigating to a different company

- **Given** the user is on company A's detail page showing "Comments (5)" (after adding 2 comments to an original count of 3)
- **When** the user navigates to company B's detail page which has 1 comment
- **Then** the heading shows "Comments (1)"

## Contact Comments

### Count updates after adding a comment

- **Given** the contact detail page is open and the heading shows "Comments (2)"
- **When** the user adds a comment and the API call succeeds
- **Then** the heading shows "Comments (3)" without a page reload

### Count stays unchanged on API failure

- **Given** the contact detail page is open and the heading shows "Comments (2)"
- **When** the user adds a comment and the API call fails
- **Then** the heading still shows "Comments (2)"

### Count displays correctly when totalCount is undefined

- **Given** the contact detail page is open and no `totalCount` was provided
- **When** the user adds a comment and the API call succeeds
- **Then** the heading shows "Comments" with no count in parentheses (unchanged)

### Count resets when navigating to a different contact

- **Given** the user is on contact A's detail page showing "Comments (4)" (after adding 1 comment to an original count of 3)
- **When** the user navigates to contact B's detail page which has 0 comments
- **Then** the heading shows "Comments (0)"