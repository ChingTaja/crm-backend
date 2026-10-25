-- Run only after a human confirms ownership; requires psql quote_id and opportunity_id variables.
-- Example: psql ... -v quote_id=... -v opportunity_id=... -f this-file.sql
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE quote_binding_input (quote_id varchar(255), opportunity_id varchar(255)) ON COMMIT DROP;
INSERT INTO quote_binding_input VALUES (:'quote_id', :'opportunity_id');
SELECT q.id FROM quotes q JOIN quote_binding_input i ON i.quote_id=q.id FOR UPDATE OF q;
SELECT o.id FROM opportunities o JOIN quote_binding_input i ON i.opportunity_id=o.id FOR UPDATE OF o;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM quotes q JOIN quote_binding_input i ON q.id=i.quote_id)
     OR NOT EXISTS (SELECT 1 FROM opportunities o JOIN quote_binding_input i ON o.id=i.opportunity_id)
     OR NOT EXISTS (SELECT 1 FROM quote_versions v JOIN quote_binding_input i ON v.quote_id=i.quote_id) THEN
    RAISE EXCEPTION 'Quote, versions or opportunity does not exist';
  END IF;
  IF EXISTS (
    SELECT 1 FROM quote_versions v JOIN quote_binding_input i ON v.quote_id=i.quote_id
    JOIN opportunities o ON o.id=i.opportunity_id
    WHERE v.customer_id IS DISTINCT FROM o.customer_id
       OR (v.opportunity_id IS NOT NULL AND v.opportunity_id <> i.opportunity_id)
  ) THEN
    RAISE EXCEPTION 'Customer or existing opportunity conflicts; manual review required';
  END IF;
END $$;
UPDATE quote_versions v SET opportunity_id=i.opportunity_id
FROM quote_binding_input i WHERE v.quote_id=i.quote_id AND v.opportunity_id IS NULL;
-- Preserve quote/version IDs, revision, audit, order links and all order snapshots.
SELECT q.id, q.number, v.id AS version_id, v.version, v.opportunity_id, q.order_id
FROM quotes q JOIN quote_versions v ON v.quote_id=q.id
JOIN quote_binding_input i ON i.quote_id=q.id ORDER BY v.version;
COMMIT;
