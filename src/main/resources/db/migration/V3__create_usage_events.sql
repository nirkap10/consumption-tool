CREATE TABLE usage_events (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    service_name    VARCHAR(255) NOT NULL,
    tokens_used     BIGINT       NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The monthly report filters by user or by organization, then by month.
CREATE INDEX idx_usage_events_user_occurred ON usage_events (user_id, occurred_at);
CREATE INDEX idx_usage_events_org_occurred  ON usage_events (organization_id, occurred_at);
