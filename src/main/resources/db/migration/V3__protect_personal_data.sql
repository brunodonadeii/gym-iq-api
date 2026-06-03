ALTER TABLE users
    DROP CONSTRAINT IF EXISTS uk_user_email;

ALTER TABLE users
    ALTER COLUMN email TYPE TEXT,
    ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_email_hash
    ON users (email_hash);

CREATE INDEX IF NOT EXISTS idx_user_email_hash
    ON users (email_hash);

ALTER TABLE student
    DROP CONSTRAINT IF EXISTS uk_student_cpf;

ALTER TABLE student
    ALTER COLUMN cpf TYPE TEXT,
    ALTER COLUMN birth_date TYPE TEXT USING birth_date::text,
    ALTER COLUMN phone TYPE TEXT,
    ALTER COLUMN zip_code TYPE TEXT,
    ALTER COLUMN address TYPE TEXT,
    ADD COLUMN IF NOT EXISTS cpf_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_cpf_hash
    ON student (cpf_hash);

CREATE INDEX IF NOT EXISTS idx_student_cpf_hash
    ON student (cpf_hash);

ALTER TABLE instructor
    ALTER COLUMN phone TYPE TEXT;

ALTER TABLE audit_log
    ALTER COLUMN actor_email TYPE TEXT;
