CREATE INDEX IF NOT EXISTS idx_payment_status_due_date
    ON payment (status, due_date);

CREATE INDEX IF NOT EXISTS idx_payment_due_date
    ON payment (due_date);

CREATE INDEX IF NOT EXISTS idx_payment_enrollment_id
    ON payment (enrollment_id);
