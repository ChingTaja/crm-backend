-- For deployments without Hibernate schema update. Run once before deploying the new backend.
-- Existing rows receive true; reruns preserve accounts already disabled.
BEGIN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;
UPDATE users SET enabled = true WHERE enabled IS NULL;
ALTER TABLE users ALTER COLUMN enabled SET DEFAULT true;
ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;
COMMIT;
