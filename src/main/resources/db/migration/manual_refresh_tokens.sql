BEGIN;
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id varchar(255) PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE,
    user_id varchar(255) NOT NULL,
    family_id varchar(255) NOT NULL,
    token_version bigint NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    revoked boolean NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS ix_refresh_family ON refresh_tokens(family_id);
COMMIT;
