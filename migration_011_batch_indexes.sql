-- Migration: replace generic single-column batch indexes with ones matched
-- to how BatchRepository actually queries the table.
--
-- What was there before (one index per column):
--   idx_batches_medicine (medicine_id)
--   idx_batches_branch   (branch_id)
--   idx_batches_expiry   (expiry_date)
--
-- Every hot BatchRepository query filters on medicine_id and/or branch_id
-- AND sorts by expiry_date, and two of them SUM(quantity) over the match:
--   sumQuantityByMedicineId                    -> medicine_id
--   sumQuantityByMedicineIdAndBranchId         -> medicine_id + branch_id
--   findByMedicineIdOrderByExpiryDateAsc        -> medicine_id, order by expiry
--   findByMedicineIdAndBranchIdOrderByExpiryDateAsc -> medicine_id + branch_id, order by expiry
--   findByBranchIdOrderByExpiryDateAsc          -> branch_id, order by expiry
--   findExpiringBySoonCutoff                    -> expiry_date <= cutoff AND quantity > 0
--
-- A single-column index can serve the filter but not the ORDER BY, so Postgres
-- still does a sort step after the index scan. It also can't answer the SUM()
-- queries without visiting every matching row on disk. Composite indexes with
-- expiry_date as the trailing key column return rows pre-sorted; adding
-- INCLUDE (quantity) lets the SUM queries be answered straight from the index
-- (index-only scan) instead of round-tripping to the table.
--
-- idx_batches_expiry_instock is a partial index: findExpiringBySoonCutoff only
-- ever cares about batches that still have stock, and in a real pharmacy most
-- batches end up at quantity = 0 once sold through. A plain index on
-- expiry_date would carry every exhausted batch too; filtering it at index-build
-- time keeps the index smaller and every scan of it relevant.
--
-- Run against your EXISTING pms_db database -- additive/replacing, no data change.
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_011_batch_indexes.sql

DROP INDEX IF EXISTS idx_batches_medicine;
DROP INDEX IF EXISTS idx_batches_branch;
DROP INDEX IF EXISTS idx_batches_expiry;

CREATE INDEX idx_batches_medicine_expiry
    ON batches(medicine_id, expiry_date) INCLUDE (quantity);

CREATE INDEX idx_batches_branch_expiry
    ON batches(branch_id, expiry_date) INCLUDE (quantity);

CREATE INDEX idx_batches_medicine_branch_expiry
    ON batches(medicine_id, branch_id, expiry_date) INCLUDE (quantity);

CREATE INDEX idx_batches_expiry_instock
    ON batches(expiry_date) WHERE quantity > 0;
