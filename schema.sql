-- Pharmacy Management System - Consolidated Schema
-- Reconciled against migration_002 through migration_006 on 2026-08-09.
--
-- USE THIS FILE ONLY FOR A FRESH, EMPTY DATABASE:
--   createdb pms_db
--   psql pms_db -f schema.sql
--
-- If you already have a running database that was built from the old
-- (Phase 1 only) schema.sql, do NOT re-run this file against it -- run the
-- migration_00N_*.sql files in order (002, 003, 004, 005, 006) instead, and
-- confirm the app boots after each one before applying the next.

-- ============================================================
-- Core / Phase 1
-- ============================================================

CREATE TABLE roles (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL   -- ADMIN, PHARMACIST, CASHIER, STOREKEEPER, MANAGER
);

-- Multi-branch (Phase 3) -- created early since users/batches/sales/purchases reference it.
CREATE TABLE branches (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     TEXT,
    phone       VARCHAR(30),
    is_main     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,        -- bcrypt hash
    full_name   VARCHAR(100),
    email       VARCHAR(100),
    role_id     INTEGER REFERENCES roles(id),
    branch_id   INTEGER REFERENCES branches(id), -- nullable: an Admin may oversee all branches
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE suppliers (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    phone       VARCHAR(30),
    email       VARCHAR(100),
    address     TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Insurance providers (Insurance Integration) -- created before customers since customers references it.
CREATE TABLE insurance_providers (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    contact_phone       VARCHAR(30),
    contact_email       VARCHAR(150),
    default_coverage_pct NUMERIC(5,2) DEFAULT 0,  -- e.g. 80.00 = 80%
    is_active           BOOLEAN DEFAULT TRUE,
    notes               VARCHAR(255),
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE TABLE customers (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(30),
    email       VARCHAR(100),
    address     TEXT,
    loyalty_points INTEGER DEFAULT 0,
    credit_balance NUMERIC(12,2) DEFAULT 0,
    insurance_provider_id INTEGER REFERENCES insurance_providers(id),
    insurance_policy_number VARCHAR(100),
    insurance_coverage_pct NUMERIC(5,2), -- overrides provider default when set
    date_of_birth DATE,
    gender      VARCHAR(20),
    allergies   TEXT,
    medical_notes TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Doctors (Prescription Management, Phase 2)
CREATE TABLE doctors (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    clinic_name     VARCHAR(150),
    phone           VARCHAR(30),
    email           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE medicines (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    generic_name    VARCHAR(150),
    brand           VARCHAR(100),
    category_id     INTEGER REFERENCES categories(id),
    barcode         VARCHAR(64) UNIQUE,
    unit            VARCHAR(30),              -- Box, Bottle, Tablet, etc.
    purchase_price  NUMERIC(12,2) NOT NULL DEFAULT 0,
    selling_price   NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_percent     NUMERIC(5,2) DEFAULT 0,
    reorder_level   INTEGER DEFAULT 10,
    image_url       VARCHAR(255),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Batches let us track expiry/manufacture date and quantity per batch (FEFO-ready)
CREATE TABLE batches (
    id              SERIAL PRIMARY KEY,
    medicine_id     INTEGER REFERENCES medicines(id) NOT NULL,
    batch_number    VARCHAR(64) NOT NULL,
    manufacture_date DATE,
    expiry_date     DATE NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 0,
    supplier_id     INTEGER REFERENCES suppliers(id),
    branch_id       INTEGER REFERENCES branches(id) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Prescriptions (Phase 2) -- created before sales since sales.prescription_id references it.
CREATE TABLE prescriptions (
    id              SERIAL PRIMARY KEY,
    customer_id     INTEGER REFERENCES customers(id) NOT NULL,
    doctor_id       INTEGER REFERENCES doctors(id),
    status          VARCHAR(30) DEFAULT 'PENDING', -- PENDING, VERIFIED, DISPENSED, CANCELLED
    notes           TEXT,
    created_by      INTEGER REFERENCES users(id),
    verified_by     INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE prescription_items (
    id                   SERIAL PRIMARY KEY,
    prescription_id      INTEGER REFERENCES prescriptions(id) NOT NULL,
    medicine_id          INTEGER REFERENCES medicines(id) NOT NULL,
    quantity             INTEGER NOT NULL,
    dosage_instructions  VARCHAR(255)
);

CREATE TABLE purchases (
    id              SERIAL PRIMARY KEY,
    supplier_id     INTEGER REFERENCES suppliers(id),
    invoice_number  VARCHAR(64),
    purchase_date   DATE DEFAULT CURRENT_DATE,
    total_amount    NUMERIC(12,2) DEFAULT 0,
    status          VARCHAR(30) DEFAULT 'PENDING',  -- PENDING, RECEIVED, CANCELLED
    created_by      INTEGER REFERENCES users(id),
    branch_id       INTEGER REFERENCES branches(id) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE purchase_items (
    id              SERIAL PRIMARY KEY,
    purchase_id     INTEGER REFERENCES purchases(id) NOT NULL,
    medicine_id     INTEGER REFERENCES medicines(id) NOT NULL,
    batch_id        INTEGER REFERENCES batches(id),
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    subtotal        NUMERIC(12,2) NOT NULL
);

CREATE TABLE sales (
    id              SERIAL PRIMARY KEY,
    customer_id     INTEGER REFERENCES customers(id),
    prescription_id INTEGER REFERENCES prescriptions(id),
    sale_date       TIMESTAMP DEFAULT NOW(),
    total_amount    NUMERIC(12,2) DEFAULT 0,
    discount        NUMERIC(12,2) DEFAULT 0,
    tax_amount      NUMERIC(12,2) DEFAULT 0,
    payment_method  VARCHAR(30),              -- CASH, CARD, CREDIT, MOBILE
    payment_status  VARCHAR(30) DEFAULT 'PAID', -- PAID, PARTIAL, CREDIT
    amount_paid     NUMERIC(12,2) DEFAULT 0,
    amount_due      NUMERIC(12,2) DEFAULT 0,
    points_earned   INTEGER DEFAULT 0,
    points_redeemed INTEGER DEFAULT 0,
    cashier_id      INTEGER REFERENCES users(id),
    branch_id       INTEGER REFERENCES branches(id) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sale_items (
    id              SERIAL PRIMARY KEY,
    sale_id         INTEGER REFERENCES sales(id) NOT NULL,
    medicine_id     INTEGER REFERENCES medicines(id) NOT NULL,
    batch_id        INTEGER REFERENCES batches(id),
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    subtotal        NUMERIC(12,2) NOT NULL
);

-- ============================================================
-- Phase 2
-- ============================================================

CREATE TABLE sale_returns (
    id              SERIAL PRIMARY KEY,
    sale_item_id    INTEGER REFERENCES sale_items(id) NOT NULL,
    quantity        INTEGER NOT NULL,
    reason          VARCHAR(255),
    refund_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_by      INTEGER REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

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

-- ============================================================
-- Phase 3
-- ============================================================

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

-- Insurance Integration
CREATE TABLE insurance_claims (
    id                  SERIAL PRIMARY KEY,
    sale_id             INTEGER REFERENCES sales(id) NOT NULL,
    insurance_provider_id INTEGER REFERENCES insurance_providers(id) NOT NULL,
    policy_number       VARCHAR(100),           -- snapshot at time of claim
    claimed_amount      NUMERIC(12,2) NOT NULL,
    approved_amount     NUMERIC(12,2),
    status              VARCHAR(30) DEFAULT 'PENDING', -- PENDING, SUBMITTED, APPROVED, REJECTED, PAID
    notes               VARCHAR(255),
    created_by          INTEGER REFERENCES users(id),
    resolved_by         INTEGER REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT NOW(),
    submitted_at        TIMESTAMP,
    resolved_at         TIMESTAMP
);

-- ============================================================
-- Settings (Settings module: pharmacy info, currency, tax, receipt
-- template, units of measure, email/SMS config). Singleton table -- only
-- one row, id = 1.
-- ============================================================

CREATE TABLE settings (
    id                              INTEGER PRIMARY KEY DEFAULT 1,
    pharmacy_name                   VARCHAR(150) NOT NULL DEFAULT 'My Pharmacy',
    address                         VARCHAR(255),
    phone                           VARCHAR(30),
    email                           VARCHAR(150),
    currency_code                   VARCHAR(10) NOT NULL DEFAULT 'USD',
    currency_symbol                 VARCHAR(10) NOT NULL DEFAULT '$',
    default_tax_percent             NUMERIC(5,2) DEFAULT 0,
    receipt_header                  TEXT,
    receipt_footer                  TEXT,
    units_of_measure                VARCHAR(255) DEFAULT 'Tablet,Bottle,Box,Strip,Vial,Ampoule',
    email_notifications_enabled     BOOLEAN DEFAULT FALSE,
    smtp_host                       VARCHAR(150),
    smtp_port                       INTEGER,
    smtp_username                   VARCHAR(150),
    smtp_password                   VARCHAR(255),
    sms_notifications_enabled       BOOLEAN DEFAULT FALSE,
    sms_provider                    VARCHAR(50),
    sms_api_key                     VARCHAR(255),
    sms_sender_id                   VARCHAR(50),
    updated_at                      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT settings_singleton CHECK (id = 1)
);

-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_medicines_barcode ON medicines(barcode);

-- Batch/stock indexes, tuned to BatchRepository's actual query shapes rather than
-- one index per column. Each covers a filter + the ORDER BY expiry_date that goes
-- with it, and includes quantity so the SUM(...) queries can be answered from the
-- index alone (index-only scan) without touching the table rows. See
-- migration_011_batch_indexes.sql for the reasoning behind each one.
CREATE INDEX idx_batches_medicine_expiry ON batches(medicine_id, expiry_date) INCLUDE (quantity);
CREATE INDEX idx_batches_branch_expiry ON batches(branch_id, expiry_date) INCLUDE (quantity);
CREATE INDEX idx_batches_medicine_branch_expiry ON batches(medicine_id, branch_id, expiry_date) INCLUDE (quantity);
CREATE INDEX idx_batches_expiry_instock ON batches(expiry_date) WHERE quantity > 0;
CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_sales_branch ON sales(branch_id);
CREATE INDEX idx_purchases_branch ON purchases(branch_id);
CREATE INDEX idx_sale_returns_sale_item ON sale_returns(sale_item_id);
CREATE INDEX idx_prescriptions_customer ON prescriptions(customer_id);
CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_stock_transfers_from ON stock_transfers(from_branch_id);
CREATE INDEX idx_stock_transfers_to ON stock_transfers(to_branch_id);
CREATE INDEX idx_insurance_claims_sale ON insurance_claims(sale_id);
CREATE INDEX idx_insurance_claims_provider ON insurance_claims(insurance_provider_id);
CREATE INDEX idx_insurance_claims_status ON insurance_claims(status);
CREATE INDEX idx_customers_insurance_provider ON customers(insurance_provider_id);

-- ============================================================
-- Seed data
-- ============================================================

INSERT INTO roles (name) VALUES
  ('ADMIN'), ('PHARMACIST'), ('CASHIER'), ('STOREKEEPER'), ('MANAGER');

INSERT INTO branches (name, is_main) VALUES ('Main Branch', TRUE);

INSERT INTO settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;