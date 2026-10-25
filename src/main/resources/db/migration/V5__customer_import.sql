-- No raw CSV or customer payload is retained. Results are committed with customer inserts.
SELECT pg_advisory_xact_lock(823604914);
CREATE TABLE IF NOT EXISTS customer_import_requests (
    actor_id varchar(255) NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    request_hash varchar(64) NOT NULL,
    response_json text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (actor_id, idempotency_key)
);
-- Non-unique indexes speed duplicate checks without changing existing customer CRUD semantics.
CREATE INDEX IF NOT EXISTS ix_customers_import_name_company ON customers (btrim(name), coalesce(btrim(company), ''));
CREATE INDEX IF NOT EXISTS ix_customers_import_email ON customers (lower(btrim(email)));
