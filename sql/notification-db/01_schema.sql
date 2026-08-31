-- notification-service schema
-- Matches proto/notification.proto: SendNotificationRequest, Preferences

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notification_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,               -- references auth-service's users.id
    channel         VARCHAR(10) NOT NULL,        -- EMAIL | SMS | PUSH
    template        VARCHAR(100) NOT NULL,       -- e.g. 'transaction_completed'
    payload         JSONB NOT NULL DEFAULT '{}', -- template variables used
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED', -- QUEUED | SENT | FAILED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE preferences (
    user_id             UUID PRIMARY KEY,        -- references auth-service's users.id
    enabled_channels    VARCHAR(10)[] NOT NULL DEFAULT ARRAY['EMAIL'], -- e.g. {EMAIL,SMS}
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_log_user_id ON notification_log(user_id);
