-- Migration: add returns support (Phase 2)
-- Run this against your EXISTING pms_db database -- it only adds a new table,
-- it does not touch anything you already have.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_002_returns.sql

CREATE TABLE sale_returns (
    id              SERIAL PRIMARY KEY,
    sale_item_id    INTEGER REFERENCES sale_items(id) NOT NULL,
    quantity        INTEGER NOT NULL,
    reason          VARCHAR(255),
    refund_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_by      INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_sale_returns_sale_item ON sale_returns(sale_item_id);
