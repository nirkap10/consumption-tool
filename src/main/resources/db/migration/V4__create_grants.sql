CREATE TABLE grants (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    amount          BIGINT       NOT NULL,
    reason          VARCHAR(255),
    granted_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
