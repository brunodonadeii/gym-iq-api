CREATE UNIQUE INDEX IF NOT EXISTS uk_enrollment_student_open_status
    ON enrollment (student_id)
    WHERE status IN ('ACTIVE', 'SUSPENDED');
