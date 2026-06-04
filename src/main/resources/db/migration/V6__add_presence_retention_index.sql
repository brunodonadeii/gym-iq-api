CREATE INDEX IF NOT EXISTS idx_presence_student_check_in_at
    ON presence (student_id, check_in_at);
