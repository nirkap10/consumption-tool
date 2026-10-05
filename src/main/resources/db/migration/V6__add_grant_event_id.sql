-- Same as V5, for grants: the caller's own id for each grant. A retry sends the
-- same id, and the unique constraint makes sure it is only stored once. Rows
-- from before this migration have no id; Postgres allows many NULLs in a unique column.
ALTER TABLE grants ADD COLUMN event_id VARCHAR(255);
ALTER TABLE grants ADD CONSTRAINT uq_grants_event_id UNIQUE (event_id);
