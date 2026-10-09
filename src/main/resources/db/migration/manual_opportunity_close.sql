BEGIN;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS close_description varchar(2000);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_at timestamp with time zone;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_by_id varchar(255);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_by_name varchar(255);
COMMIT;
