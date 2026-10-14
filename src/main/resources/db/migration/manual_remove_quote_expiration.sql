-- Phase 1: preserve legacy dates as archival data, but never require/default them.
DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(823604911);
    IF EXISTS (SELECT 1 FROM quote_versions WHERE status = 'Expired') THEN
        RAISE EXCEPTION 'Stored Expired quote versions require audit-based status review before deployment; no automatic Sent conversion is allowed';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='quote_versions' AND column_name='valid_until') THEN
        ALTER TABLE quote_versions ALTER COLUMN valid_until DROP NOT NULL;
        ALTER TABLE quote_versions ALTER COLUMN valid_until DROP DEFAULT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sales_orders' AND column_name='valid_until') THEN
        ALTER TABLE sales_orders ALTER COLUMN valid_until DROP NOT NULL;
        ALTER TABLE sales_orders ALTER COLUMN valid_until DROP DEFAULT;
    END IF;
END $$;
