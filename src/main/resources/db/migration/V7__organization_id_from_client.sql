-- The client now sends the organization id it already uses, in any format, so
-- the id becomes text. The three columns that point to it change with it.
-- Existing UUIDs are kept as their text form.

-- A foreign key needs both sides to have the same type, so drop them first.
ALTER TABLE users        DROP CONSTRAINT users_organization_id_fkey;
ALTER TABLE usage_events DROP CONSTRAINT usage_events_organization_id_fkey;
ALTER TABLE grants       DROP CONSTRAINT grants_organization_id_fkey;

ALTER TABLE organizations ALTER COLUMN id              TYPE VARCHAR(255) USING id::text;
ALTER TABLE users         ALTER COLUMN organization_id TYPE VARCHAR(255) USING organization_id::text;
ALTER TABLE usage_events  ALTER COLUMN organization_id TYPE VARCHAR(255) USING organization_id::text;
ALTER TABLE grants        ALTER COLUMN organization_id TYPE VARCHAR(255) USING organization_id::text;

ALTER TABLE users        ADD CONSTRAINT users_organization_id_fkey
    FOREIGN KEY (organization_id) REFERENCES organizations (id);
ALTER TABLE usage_events ADD CONSTRAINT usage_events_organization_id_fkey
    FOREIGN KEY (organization_id) REFERENCES organizations (id);
ALTER TABLE grants       ADD CONSTRAINT grants_organization_id_fkey
    FOREIGN KEY (organization_id) REFERENCES organizations (id);
