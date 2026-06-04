ALTER TABLE enrollment
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP;

UPDATE enrollment
SET canceled_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE status = 'CANCELED'
  AND canceled_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_enrollment_status_canceled_at
    ON enrollment (status, canceled_at);

CREATE INDEX IF NOT EXISTS idx_enrollment_start_date
    ON enrollment (start_date);
