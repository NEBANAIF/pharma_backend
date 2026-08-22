-- Migration: add Prescription Management (Phase 2)
-- Run against your EXISTING pms_db database -- additive only.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_003_prescriptions.sql

CREATE TABLE doctors (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    clinic_name     VARCHAR(150),
    phone           VARCHAR(30),
    email           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT NOW()
);

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

-- Link a sale back to the prescription it fulfilled, if any (nullable -- most sales have no prescription)
ALTER TABLE sales ADD COLUMN prescription_id INTEGER REFERENCES prescriptions(id);

CREATE INDEX idx_prescriptions_customer ON prescriptions(customer_id);
CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);
