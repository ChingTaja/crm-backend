-- Upgrade legacy columns and install database safeguards in one Flyway transaction.

-- manual_password_hash.sql

-- Existing installations: run once before starting the new version.
-- Preserve existing BCrypt hashes; Hibernate ddl-auto cannot rename columns.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password_hash') THEN
        ALTER TABLE users RENAME COLUMN password TO password_hash;
    END IF;
END $$;

-- manual_user_enabled.sql

-- For deployments without Hibernate schema update. Run once before deploying the new backend.
-- Existing rows receive true; reruns preserve accounts already disabled.

ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;
UPDATE users SET enabled = true WHERE enabled IS NULL;
ALTER TABLE users ALTER COLUMN enabled SET DEFAULT true;
ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;

-- manual_refresh_tokens.sql

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

-- manual_opportunity_fields.sql

-- Existing installations: run before starting the updated application.

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

-- manual_opportunity_close.sql

ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS close_description varchar(2000);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_at timestamp with time zone;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_by_id varchar(255);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS closed_by_name varchar(255);

-- manual_remove_contacting.sql

-- Run on existing databases when deploying the removal of CONTACTING.
UPDATE leads SET status = CASE qualification_decision
    WHEN 'approved' THEN '已合格'
    WHEN 'rejected' THEN '不合格'
    ELSE '待聯繫' END
WHERE status = '聯繫中';

-- manual_remove_quote_expiration.sql

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

-- manual_entity_reference_guards.sql

-- Non-destructive and repeatable. Invalid existing IDs abort the transaction;
-- no business data is deleted or silently unlinked.
DO $$
DECLARE relation record;
BEGIN
    PERFORM pg_advisory_xact_lock(823604910);
    FOR relation IN SELECT * FROM (VALUES
        ('fk_contacts_customer', 'contacts', 'customer_id', 'customers', 'NO ACTION'),
        ('fk_opportunities_customer', 'opportunities', 'customer_id', 'customers', 'NO ACTION'),
        ('fk_opportunities_contact', 'opportunities', 'contact_id', 'contacts', 'NO ACTION'),
        ('fk_opportunities_lead', 'opportunities', 'lead_id', 'leads', 'NO ACTION'),
        ('fk_lead_qualification_customer', 'leads', 'qualification_customer_id', 'customers', 'NO ACTION'),
        ('fk_lead_qualification_contact', 'leads', 'qualification_contact_id', 'contacts', 'NO ACTION'),
        ('fk_lead_qualification_opportunity', 'leads', 'qualification_opportunity_id', 'opportunities', 'NO ACTION'),
        ('fk_refresh_tokens_user', 'refresh_tokens', 'user_id', 'users', 'CASCADE')
    ) AS refs(constraint_name, source_table, source_column, target_table, deletion)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname=relation.constraint_name AND conrelid=relation.source_table::regclass) THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(id) ON DELETE %s NOT VALID',
                relation.source_table, relation.constraint_name, relation.source_column, relation.target_table, relation.deletion);
        END IF;
        EXECUTE format('ALTER TABLE %I VALIDATE CONSTRAINT %I', relation.source_table, relation.constraint_name);
    END LOOP;
END $$;

CREATE SEQUENCE IF NOT EXISTS quote_number_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku_ci ON products (lower(btrim(sku)));
