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
