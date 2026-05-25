-- Run this first, before removing duration_days from the database.
BEGIN;

ALTER TABLE plan
ADD COLUMN IF NOT EXISTS duration_months INTEGER;

UPDATE plan
SET duration_months = CEIL(duration_days / 30.0)::INTEGER
WHERE duration_months IS NULL
  AND duration_days IS NOT NULL;

UPDATE plan
SET duration_months = 1
WHERE duration_months IS NULL
   OR duration_months < 1;

ALTER TABLE plan
ALTER COLUMN duration_months SET NOT NULL;

COMMIT;

-- After the backend is deployed and validated using duration_months,
-- you can run this cleanup separately:
--
-- BEGIN;
-- ALTER TABLE plan DROP COLUMN IF EXISTS duration_days;
-- COMMIT;
