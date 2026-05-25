-- spring-services 0.16 adds a mandatory `name` field on AuditLogEntity, mapped
-- to the `entity_name` column with nullable = false. Existing rows have no
-- value, so add the column nullable first, backfill the sentinel, then apply
-- the NOT NULL constraint — the standard safe add-nullable / backfill /
-- constrain pattern (a direct ADD COLUMN ... NOT NULL would fail on existing
-- rows). Hibernate runs with ddl-auto=validate, so the final schema must match
-- the entity's nullable = false declaration.
ALTER TABLE audit_log
    ADD COLUMN entity_name VARCHAR(255);

UPDATE audit_log
    SET entity_name = 'UNKNOWN'
    WHERE entity_name IS NULL;

ALTER TABLE audit_log
    ALTER COLUMN entity_name SET NOT NULL;
