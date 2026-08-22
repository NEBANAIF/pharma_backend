-- Migration: add Financial Management (Phase 2) -- expenses + daily cash register closing
-- Run against your EXISTING pms_db database -- additive only.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_004_financials.sql

CREATE TABLE expenses (
    id              SERIAL PRIMARY KEY,
    category        VARCHAR(50) NOT NULL, -- RENT, UTILITIES, SALARIES, SUPPLIES, MAINTENANCE, OTHER
    description     VARCHAR(255),
    amount          NUMERIC(12,2) NOT NULL,
    expense_date    DATE DEFAULT CURRENT_DATE,
    created_by      INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cash_register_closings (
    id                SERIAL PRIMARY KEY,
    closing_date      DATE UNIQUE NOT NULL,
    opening_balance   NUMERIC(12,2) NOT NULL DEFAULT 0,
    cash_sales        NUMERIC(12,2) NOT NULL DEFAULT 0,
    cash_expenses     NUMERIC(12,2) NOT NULL DEFAULT 0,
    expected_cash     NUMERIC(12,2) NOT NULL DEFAULT 0,
    actual_cash       NUMERIC(12,2) NOT NULL DEFAULT 0,
    difference        NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes             TEXT,
    closed_by         INTEGER REFERENCES users(id),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_expenses_date ON expenses(expense_date);
