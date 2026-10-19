-- A reservation is a usage row written before the use. While reserved_until is
-- set, the row waits for its usage call; the expiry job deletes it once the
-- time has passed. NULL means normal usage, as on every row before this.
ALTER TABLE usage_events ADD COLUMN reserved_until TIMESTAMPTZ;
