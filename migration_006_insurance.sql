-- Migration: add Insurance Integration
-- Run against your EXISTING pms_db database.
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_006_insurance.sql
--
-- IMPORTANT: confirm migration_005_branches.sql has actually been run and the
-- app boots cleanly (ddl-auto=validate) BEFORE applying this one. Layering a
-- third unverified migration on top of an unverified one compounds the risk.

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

-- Coverage fields on customers. Nullable -- most customers won't have insurance.
ALTER TABLE customers ADD COLUMN insurance_provider_id INTEGER REFERENCES insurance_providers(id);
ALTER TABLE customers ADD COLUMN insurance_policy_number VARCHAR(100);
ALTER TABLE customers ADD COLUMN insurance_coverage_pct NUMERIC(5,2); -- overrides provider default when set

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

CREATE INDEX idx_insurance_claims_sale ON insurance_claims(sale_id);
CREATE INDEX idx_insurance_claims_provider ON insurance_claims(insurance_provider_id);
CREATE INDEX idx_insurance_claims_status ON insurance_claims(status);
CREATE INDEX idx_customers_insurance_provider ON customers(insurance_provider_id);
