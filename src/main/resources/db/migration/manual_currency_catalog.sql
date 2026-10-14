-- Existing product currency must be assigned from confirmed business data separately.
-- Never infer currency from price or overwrite a stored transaction currency.
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_currency') THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_currency FOREIGN KEY (currency) REFERENCES currencies(code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quote_versions_currency') THEN
        ALTER TABLE quote_versions ADD CONSTRAINT fk_quote_versions_currency FOREIGN KEY (currency) REFERENCES currencies(code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_orders_currency') THEN
        ALTER TABLE sales_orders ADD CONSTRAINT fk_sales_orders_currency FOREIGN KEY (currency) REFERENCES currencies(code);
    END IF;
END $$;
