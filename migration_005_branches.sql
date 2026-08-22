-- Migration: add Multi-Branch Support (Phase 3)
-- Run against your EXISTING pms_db database.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_005_branches.sql
--
-- This creates a "Main Branch" and assigns all your EXISTING stock, sales,
-- and purchases to it, so nothing you've already built breaks.

CREATE TABLE branches (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     TEXT,
    phone       VARCHAR(30),
    is_main     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT NOW()
);

INSERT INTO branches (name, is_main) VALUES ('Main Branch', TRUE);

ALTER TABLE users ADD COLUMN branch_id INTEGER REFERENCES branches(id);
ALTER TABLE batches ADD COLUMN branch_id INTEGER REFERENCES branches(id);
ALTER TABLE sales ADD COLUMN branch_id INTEGER REFERENCES branches(id);
ALTER TABLE purchases ADD COLUMN branch_id INTEGER REFERENCES branches(id);

-- Backfill everything that already exists to the Main Branch
UPDATE users SET branch_id = (SELECT id FROM branches WHERE is_main = TRUE LIMIT 1) WHERE branch_id IS NULL;
UPDATE batches SET branch_id = (SELECT id FROM branches WHERE is_main = TRUE LIMIT 1) WHERE branch_id IS NULL;
UPDATE sales SET branch_id = (SELECT id FROM branches WHERE is_main = TRUE LIMIT 1) WHERE branch_id IS NULL;
UPDATE purchases SET branch_id = (SELECT id FROM branches WHERE is_main = TRUE LIMIT 1) WHERE branch_id IS NULL;

-- Stock, sales, and purchases must always belong to a branch going forward.
-- Users may optionally be branch-less (e.g. an Admin who oversees all branches).
ALTER TABLE batches ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE sales ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN branch_id SET NOT NULL;

CREATE TABLE stock_transfers (
    id              SERIAL PRIMARY KEY,
    medicine_id     INTEGER REFERENCES medicines(id) NOT NULL,
    from_branch_id  INTEGER REFERENCES branches(id) NOT NULL,
    to_branch_id    INTEGER REFERENCES branches(id) NOT NULL,
    quantity        INTEGER NOT NULL,
    status          VARCHAR(30) DEFAULT 'PENDING', -- PENDING, COMPLETED, CANCELLED
    notes           VARCHAR(255),
    created_by      INTEGER REFERENCES users(id),
    completed_by    INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW(),
    completed_at    TIMESTAMP
);

CREATE INDEX idx_batches_branch ON batches(branch_id);
CREATE INDEX idx_sales_branch ON sales(branch_id);
CREATE INDEX idx_purchases_branch ON purchases(branch_id);
CREATE INDEX idx_stock_transfers_from ON stock_transfers(from_branch_id);
CREATE INDEX idx_stock_transfers_to ON stock_transfers(to_branch_id);
