-- Spec 095: spring-services 0.14 replaces the free-text user columns on
-- audit_log and comments with real FK associations to users(id).
--
-- audit_log.user_name VARCHAR(255) NULL  ->  audit_log.user_id UUID NOT NULL FK
--   All existing rows are blindly remapped to SystemUser.ID
--   (00000000-0000-0000-0000-000000000000). Historical attribution is
--   intentionally lost — the prior column held JWT `name` claims that are
--   neither unique nor reliably matchable to a user row.
--
-- comments.author VARCHAR(255) NOT NULL  ->  comments.author_id UUID NOT NULL FK
--   V30 already populated this column with UUID strings (all pointing at
--   SystemUser.ID), so the cast via USING is lossless.

-- audit_log: free-text user_name -> UUID FK user_id
ALTER TABLE audit_log ADD COLUMN user_id UUID;

UPDATE audit_log SET user_id = '00000000-0000-0000-0000-000000000000';

ALTER TABLE audit_log ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_log_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE audit_log DROP COLUMN user_name;

-- comments: VARCHAR author -> UUID FK author_id
ALTER TABLE comments RENAME COLUMN author TO author_id;

ALTER TABLE comments ALTER COLUMN author_id TYPE UUID USING author_id::uuid;

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_author
    FOREIGN KEY (author_id) REFERENCES users(id);
