CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE student DROP CONSTRAINT IF EXISTS fk_student_user;
ALTER TABLE instructor DROP CONSTRAINT IF EXISTS fk_instructor_user;
ALTER TABLE password_reset_token DROP CONSTRAINT IF EXISTS fk_password_reset_token_user;

ALTER TABLE users
    DROP COLUMN IF EXISTS public_id;

ALTER TABLE users
    ALTER COLUMN id_user DROP IDENTITY IF EXISTS;

ALTER TABLE users
    ALTER COLUMN id_user DROP DEFAULT;

ALTER TABLE users
    ALTER COLUMN id_user TYPE UUID USING gen_random_uuid(),
    ALTER COLUMN id_user SET DEFAULT gen_random_uuid();

ALTER TABLE student
    ALTER COLUMN user_id TYPE UUID USING NULL;

ALTER TABLE instructor
    ALTER COLUMN user_id TYPE UUID USING NULL;

ALTER TABLE password_reset_token
    ALTER COLUMN user_id TYPE UUID USING NULL;

ALTER TABLE audit_log
    ALTER COLUMN actor_user_id TYPE UUID USING NULL,
    ALTER COLUMN resource_id TYPE VARCHAR(100) USING resource_id::VARCHAR;

ALTER TABLE student
    ADD CONSTRAINT fk_student_user
        FOREIGN KEY (user_id) REFERENCES users (id_user);

ALTER TABLE instructor
    ADD CONSTRAINT fk_instructor_user
        FOREIGN KEY (user_id) REFERENCES users (id_user);

ALTER TABLE password_reset_token
    ADD CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_id) REFERENCES users (id_user);
