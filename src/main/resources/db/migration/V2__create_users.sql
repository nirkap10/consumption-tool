CREATE TABLE users (
    id                UUID        PRIMARY KEY,
    organization_id   UUID        NOT NULL REFERENCES organizations (id),
    monthly_allowance BIGINT      NOT NULL,
    remaining_credit  BIGINT      NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
