-- Migration: Stock Movement audit trail (Stock History + Discard)
-- Run against your EXISTING pms_db database.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_014_stock_movements.sql
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (see application.yml),
-- Hibernate will also create this automatically on next backend startup.
-- This file exists for environments that run migrations manually instead.

CREATE TABLE stock_movements (
    id              SERIAL PRIMARY KEY,
    medicine_id     INTEGER NOT NULL REFERENCES medicines(id),
    branch_id       INTEGER REFERENCES branches(id),
    batch_id        INTEGER REFERENCES batches(id),
    source          VARCHAR(30) NOT NULL, -- SALE, RETURN, PURCHASE, TRANSFER_IN, TRANSFER_OUT, DISCARD, ADJUSTMENT
    quantity_before INTEGER NOT NULL,
    quantity_after  INTEGER NOT NULL,
    change          INTEGER NOT NULL,
    reference       VARCHAR(255),
    notes           TEXT,
    performed_by    INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_medicine ON stock_movements(medicine_id);
CREATE INDEX idx_stock_movements_source ON stock_movements(source);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at);
