-- Adds the columns needed for the public teacher signup + admin approval flow.
--
-- 1. `status` — lifecycle of the account (PENDING / APPROVED / REJECTED).
--    Existing rows are backfilled to APPROVED so seeded admins and admin-created
--    teachers continue to log in.
-- 2. `email` — required for new public signups (uniquely indexed); nullable so existing
--    rows without an email remain valid.

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32);
UPDATE users SET status = 'APPROVED' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Unique index, not a UNIQUE constraint, so multiple existing NULLs are allowed
-- (Postgres treats NULLs in unique indexes as distinct by default).
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);
