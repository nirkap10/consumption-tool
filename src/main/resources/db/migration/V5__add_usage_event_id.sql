-- The caller's own id for each usage report. A retry sends the same id, and the
-- unique constraint makes sure it is only stored once. Rows from before this
-- migration have no id; Postgres allows many NULLs in a unique column.
ALTER TABLE usage_events ADD COLUMN event_id VARCHAR(255);
ALTER TABLE usage_events ADD CONSTRAINT uq_usage_events_event_id UNIQUE (event_id);
