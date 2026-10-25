-- Historical rows intentionally stay NULL until verified against trustworthy records.
-- Never derive created_by_id from owner, quote author, or the current operator.
SELECT pg_advisory_xact_lock(823604913);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS created_by_id varchar(255);
CREATE OR REPLACE FUNCTION protect_opportunity_creator() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.created_by_id IS NOT NULL AND NEW.created_by_id IS DISTINCT FROM OLD.created_by_id THEN
        RAISE EXCEPTION 'opportunity creator is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'opportunity_creator_immutable' AND tgrelid = 'opportunities'::regclass) THEN
        CREATE TRIGGER opportunity_creator_immutable BEFORE UPDATE OF created_by_id ON opportunities
            FOR EACH ROW EXECUTE FUNCTION protect_opportunity_creator();
    END IF;
END $$;
