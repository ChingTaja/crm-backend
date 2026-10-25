-- Currency belongs to quote versions and orders; never rewrite transaction snapshots.
DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(823604912);
    INSERT INTO currencies(code, name, symbol, fraction_digits, active)
    VALUES ('TWD', '新台幣', 'NT$', 2, true), ('USD', '美元', 'US$', 2, true)
    ON CONFLICT (code) DO NOTHING;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'currency_code_format') THEN
        ALTER TABLE currencies ADD CONSTRAINT currency_code_format CHECK (code ~ '^[A-Z]{3}$');
        ALTER TABLE currencies ADD CONSTRAINT currency_supported_precision CHECK (fraction_digits = 2);
    END IF;
    -- Legacy column is retained as nullable, unused data. New schemas do not create it.
    ALTER TABLE products DROP CONSTRAINT IF EXISTS fk_products_currency;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'products' AND column_name = 'currency') THEN
        ALTER TABLE products ALTER COLUMN currency DROP NOT NULL;
        ALTER TABLE products ALTER COLUMN currency DROP DEFAULT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quote_versions_currency') THEN
        ALTER TABLE quote_versions ADD CONSTRAINT fk_quote_versions_currency FOREIGN KEY (currency) REFERENCES currencies(code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_orders_currency') THEN
        ALTER TABLE sales_orders ADD CONSTRAINT fk_sales_orders_currency FOREIGN KEY (currency) REFERENCES currencies(code);
    END IF;
END $$;
