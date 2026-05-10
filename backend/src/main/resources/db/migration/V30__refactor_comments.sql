-- Spec 094: Refactor comments to use the standalone CommentEntity from spring-services.
-- The owner relation moves out of the comment row and into three new join tables.
-- All historical comments are reassigned to the SYSTEM-USER.

-- 1. Insert SYSTEM-USER (idempotent guard so existing dev DBs don't trip).
INSERT INTO users (id, sub, name, email, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'system', 'System', NULL, now(), now())
ON CONFLICT (sub) DO NOTHING;

-- 2. Create join tables. PRIMARY KEY (comment_id) ensures one comment can only
--    attach to one owner of a given type; the cross-table invariant is enforced
--    in the service layer.
CREATE TABLE company_comments (
    comment_id UUID PRIMARY KEY REFERENCES comments(id) ON DELETE CASCADE,
    company_id UUID NOT NULL    REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX idx_company_comments_company_id ON company_comments(company_id);

CREATE TABLE contact_comments (
    comment_id UUID PRIMARY KEY REFERENCES comments(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL    REFERENCES contacts(id) ON DELETE CASCADE
);
CREATE INDEX idx_contact_comments_contact_id ON contact_comments(contact_id);

CREATE TABLE task_comments (
    comment_id UUID PRIMARY KEY REFERENCES comments(id) ON DELETE CASCADE,
    task_id    UUID NOT NULL    REFERENCES tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);

-- 3. Backfill join tables from the existing FK columns.
INSERT INTO company_comments (comment_id, company_id)
SELECT id, company_id FROM comments WHERE company_id IS NOT NULL;

INSERT INTO contact_comments (comment_id, contact_id)
SELECT id, contact_id FROM comments WHERE contact_id IS NOT NULL;

INSERT INTO task_comments (comment_id, task_id)
SELECT id, task_id FROM comments WHERE task_id IS NOT NULL;

-- 4. Schema change on comments: drop CHECK + FK columns + author, add author_id.
ALTER TABLE comments DROP CONSTRAINT chk_comment_owner;

DROP INDEX IF EXISTS idx_comments_company_id;
DROP INDEX IF EXISTS idx_comments_contact_id;
DROP INDEX IF EXISTS idx_comments_task_id;

ALTER TABLE comments DROP COLUMN company_id;
ALTER TABLE comments DROP COLUMN contact_id;
ALTER TABLE comments DROP COLUMN task_id;

ALTER TABLE comments ADD COLUMN author_id VARCHAR(255);
UPDATE comments SET author_id = '00000000-0000-0000-0000-000000000000';
ALTER TABLE comments ALTER COLUMN author_id SET NOT NULL;
ALTER TABLE comments DROP COLUMN author;
