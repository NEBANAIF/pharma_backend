-- Fixes a role-name casing bug: schema.sql seeds roles as uppercase
-- ('ADMIN', 'PHARMACIST', 'CASHIER', 'STOREKEEPER', 'MANAGER'), but a role
-- row can end up with different casing (e.g. 'admin') if it was inserted by
-- hand instead of through the seed script. The frontend's role checks
-- (hasAnyRole in roles.js) and the backend's @PreAuthorize("hasRole('ADMIN')")
-- checks both compare against the exact uppercase string, so a stray-case
-- role silently fails every permission check tied to it -- no error, the
-- gated menu items/actions just don't show up.
--
-- This uppercases any role name that isn't already canonical. Safe to run
-- repeatedly; a no-op once everything matches.
--
-- Via psql: psql -U postgres -d pms_db -f migration_012_fix_role_casing.sql

UPDATE roles SET name = UPPER(TRIM(name)) WHERE name <> UPPER(TRIM(name));
