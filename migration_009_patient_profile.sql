-- Migration: add patient-profile fields to customers
--
-- The spec's "Patient Management" module (medical history, allergies, DOB)
-- was never built as a separate module -- this pharmacy already treats each
-- Customer as the patient (Prescriptions and Sales both hang off customer_id),
-- so rather than bolt on a redundant parallel Patient entity, this extends
-- Customer with the missing medical fields. Medication history is derived
-- from the existing prescriptions/sales tables at read time -- see the new
-- GET /api/customers/{id}/history endpoint.
--
-- Run against your EXISTING pms_db database -- additive only.
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_009_patient_profile.sql

ALTER TABLE customers
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN gender        VARCHAR(20),
    ADD COLUMN allergies     TEXT,
    ADD COLUMN medical_notes TEXT;