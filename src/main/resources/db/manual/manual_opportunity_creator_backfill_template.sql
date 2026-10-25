-- MANUAL ONLY. This file is never run by an application initializer.
-- Supply independently verified IDs and the evidence for each mapping.
-- Do not derive this mapping from owner, quote.created_by, or the current operator.
BEGIN;
CREATE TEMP TABLE verified_opportunity_creators (
    opportunity_id varchar(255) PRIMARY KEY,
    creator_id varchar(255) NOT NULL,
    evidence text NOT NULL CHECK (length(trim(evidence)) > 0)
) ON COMMIT DROP;

-- INSERT INTO verified_opportunity_creators VALUES
-- ('verified-opportunity-id', 'verified-user-id', 'confirmed source / ticket / reviewer');

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM verified_opportunity_creators) THEN
        RAISE EXCEPTION 'Provide verified creator mappings before running the backfill';
    END IF;
    IF EXISTS (
        SELECT 1 FROM verified_opportunity_creators m
        LEFT JOIN opportunities o ON o.id = m.opportunity_id
        LEFT JOIN users u ON u.id = m.creator_id
        WHERE o.id IS NULL OR u.id IS NULL
           OR (o.created_by_id IS NOT NULL AND o.created_by_id IS DISTINCT FROM m.creator_id)
    ) THEN
        RAISE EXCEPTION 'Mapping includes missing records or conflicts with a confirmed creator';
    END IF;
END $$;
UPDATE opportunities o SET created_by_id = m.creator_id
FROM verified_opportunity_creators m
WHERE o.id = m.opportunity_id AND o.created_by_id IS NULL
RETURNING o.id, o.name, o.created_by_id, m.evidence;

-- Review all returned rows. Change to COMMIT only after confirming the mapping.
ROLLBACK;
