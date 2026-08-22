-- Migration: add insurance formulary ("Approved medicines" from the original spec)
-- Run against your EXISTING pms_db database -- additive only.
--
-- IMPORTANT: confirm migration_006_insurance.sql has actually been run and the
-- app boots cleanly before applying this one.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_007_insurance_formulary.sql

CREATE TABLE insurance_approved_medicines (
    id                      SERIAL PRIMARY KEY,
    insurance_provider_id   INTEGER REFERENCES insurance_providers(id) NOT NULL,
    medicine_id             INTEGER REFERENCES medicines(id) NOT NULL,
    coverage_pct_override   NUMERIC(5,2), -- overrides the provider's default_coverage_pct for this specific medicine
    created_at              TIMESTAMP DEFAULT NOW(),
    UNIQUE (insurance_provider_id, medicine_id)
);

CREATE INDEX idx_approved_medicines_provider ON insurance_approved_medicines(insurance_provider_id);
CREATE INDEX idx_approved_medicines_medicine ON insurance_approved_medicines(medicine_id);
