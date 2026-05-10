-- Fix mismatch between V30 and CommentEntity from spring-services:
-- CommentEntity stores the UUID of the UserEntity as a String, but maps it
-- to a column named `author` (not `author_id`). The value stored in V30 is
-- already correct (the System-User's UUID), only the column name needs to
-- be aligned with the entity mapping. The misleading column name should
-- ultimately be fixed in spring-services itself.
ALTER TABLE comments RENAME COLUMN author_id TO author;