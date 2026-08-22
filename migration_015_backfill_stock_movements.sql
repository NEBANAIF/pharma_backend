-- Backfill: give every pre-existing batch a stock_movements entry.
-- Run this ONCE, after migration_014_stock_movements.sql, against your
-- EXISTING pms_db database (or let Hibernate ddl-auto=update create the
-- table first by starting the backend, then run this).
--
-- Via pgAdmin: open Query Tool on pms_db, paste this, run.
-- Via psql:    psql -U postgres -d pms_db -f migration_015_backfill_stock_movements.sql
--
-- Why this is needed: PurchaseService.receive() and BatchService.create()
-- only started writing to stock_movements once that feature was added.
-- Any batch that existed before then (received via a Purchase Order, added
-- manually, or inserted directly into the database) has no movement row,
-- so it's invisible in Stock History even though real stock exists.
--
-- Part 1: batches that WERE received through a Purchase Order still have
-- their original received quantity on purchase_items.quantity (unlike
-- batches.quantity, which has since been reduced by any sales/discards/
-- transfers against that batch) -- so these are backfilled as accurate
-- PURCHASE entries.
INSERT INTO stock_movements (medicine_id, branch_id, batch_id, source, quantity_before, quantity_after, change, reference, notes, performed_by, created_at)
SELECT
    b.medicine_id,
    b.branch_id,
    b.id,
    'PURCHASE',
    0,
    pi.quantity,
    pi.quantity,
    'Purchase #' || p.id,
    'Backfilled from an existing purchase received before Stock History tracking was added.',
    NULL,
    COALESCE(b.created_at, NOW())
FROM batches b
JOIN purchase_items pi ON pi.batch_id = b.id
JOIN purchases p ON p.id = pi.purchase_id
WHERE NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.batch_id = b.id);

-- Part 2: everything else (manually added batches, or rows inserted
-- directly into the database) has no record of its original received
-- quantity, so it's backfilled as an opening-balance ADJUSTMENT using the
-- batch's CURRENT quantity -- accurate as a starting point for the ledger
-- going forward, not as a claim about what was originally received.
INSERT INTO stock_movements (medicine_id, branch_id, batch_id, source, quantity_before, quantity_after, change, reference, notes, performed_by, created_at)
SELECT
    b.medicine_id,
    b.branch_id,
    b.id,
    'ADJUSTMENT',
    0,
    b.quantity,
    b.quantity,
    'Opening balance (batch ' || b.batch_number || ')',
    'Backfilled: this batch existed before Stock History tracking was added, and its original received quantity was not recorded.',
    NULL,
    COALESCE(b.created_at, NOW())
FROM batches b
WHERE NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.batch_id = b.id);
