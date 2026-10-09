-- Existing installations: run before starting the updated application.
BEGIN;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS amount numeric(19,2);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS expected_close_date date;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS stage varchar(255);
UPDATE opportunities SET amount = 0 WHERE amount IS NULL;
UPDATE opportunities SET stage = '需求確認' WHERE stage IS NULL;
ALTER TABLE opportunities ALTER COLUMN amount SET NOT NULL;
ALTER TABLE opportunities ALTER COLUMN stage SET NOT NULL;
ALTER TABLE opportunities ALTER COLUMN lead_id DROP NOT NULL;
ALTER TABLE opportunities ALTER COLUMN contact_id DROP NOT NULL;
DO $$
DECLARE item record;
BEGIN
    FOR item IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'opportunities'::regclass AND contype = 'u'
        AND pg_get_constraintdef(oid) = 'UNIQUE (lead_id)'
    LOOP
        EXECUTE format('ALTER TABLE opportunities DROP CONSTRAINT %I', item.conname);
    END LOOP;
END $$;
COMMIT;
