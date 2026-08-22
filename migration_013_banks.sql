-- Migration: Bank Management + richer POS payment details
-- Run against your EXISTING pms_db database.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_013_banks.sql
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (see application.yml),
-- Hibernate will also create these automatically on next backend startup.
-- This file exists for environments that run migrations manually instead.

CREATE TABLE banks (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    account_number  VARCHAR(60),
    created_at      TIMESTAMP DEFAULT NOW()
);

ALTER TABLE sales ADD COLUMN transaction_number VARCHAR(100);
ALTER TABLE sales ADD COLUMN bank_id INTEGER REFERENCES banks(id);

CREATE INDEX idx_sales_bank ON sales(bank_id);
