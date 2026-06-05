CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE   users (
    id_user UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email TEXT NOT NULL,
    email_hash VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    lgpd_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    lgpd_accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE student (
    id_student UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    cpf TEXT NOT NULL,
    cpf_hash VARCHAR(64) NOT NULL,
    birth_date TEXT NOT NULL,
    phone TEXT NOT NULL,
    zip_code TEXT,
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users (id_user)
);

CREATE TABLE instructor (
    id_instructor UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    cref VARCHAR(20) NOT NULL,
    phone TEXT NOT NULL,
    specialty VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_instructor_user FOREIGN KEY (user_id) REFERENCES users (id_user)
);

CREATE TABLE plan (
    id_plan SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    monthly_price NUMERIC(10, 2) NOT NULL,
    duration_months INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exercise (
    id_exercise SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    muscle_group VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE enrollment (
    id_enrollment UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    plan_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    canceled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student (id_student),
    CONSTRAINT fk_enrollment_plan FOREIGN KEY (plan_id) REFERENCES plan (id_plan)
);

CREATE TABLE payment (
    id_payment UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment (id_enrollment),
    CONSTRAINT uk_payment_enrollment_due_date UNIQUE (enrollment_id, due_date)
);

CREATE TABLE presence (
    id_presence UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    check_in_at TIMESTAMP NOT NULL,
    check_out_at TIMESTAMP,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_presence_student FOREIGN KEY (student_id) REFERENCES student (id_student)
);

CREATE TABLE workout_sheet (
    id_workout_sheet UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    goal VARCHAR(150),
    start_date DATE NOT NULL,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_workout_sheet_student FOREIGN KEY (student_id) REFERENCES student (id_student),
    CONSTRAINT fk_workout_sheet_instructor FOREIGN KEY (instructor_id) REFERENCES instructor (id_instructor)
);

CREATE TABLE workout_sheet_exercise (
    id_workout_sheet_exercise UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_sheet_id UUID NOT NULL,
    exercise_id INTEGER NOT NULL,
    sets INTEGER NOT NULL,
    repetitions VARCHAR(50) NOT NULL,
    rest_seconds INTEGER,
    training_section VARCHAR(40) NOT NULL DEFAULT 'A',
    execution_order INTEGER NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT fk_workout_sheet_exercise_sheet FOREIGN KEY (workout_sheet_id) REFERENCES workout_sheet (id_workout_sheet),
    CONSTRAINT fk_workout_sheet_exercise_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id_exercise),
    CONSTRAINT uk_workout_sheet_exercise_order UNIQUE (workout_sheet_id, training_section, execution_order)
);

CREATE TABLE retention_alert (
    id_retention_alert UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    inactive_days INTEGER NOT NULL,
    overdue_payments INTEGER NOT NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_retention_alert_student FOREIGN KEY (student_id) REFERENCES student (id_student)
);

CREATE TABLE password_reset_token (
    id_password_reset_token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id_user)
);

CREATE TABLE audit_log (
    id_audit_log BIGSERIAL PRIMARY KEY,
    actor_user_id UUID,
    actor_email TEXT,
    actor_role VARCHAR(20),
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(40),
    resource_id VARCHAR(100),
    description VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD CONSTRAINT uk_user_email_hash UNIQUE (email_hash);
ALTER TABLE student ADD CONSTRAINT uk_student_cpf_hash UNIQUE (cpf_hash);
ALTER TABLE instructor ADD CONSTRAINT uk_instructor_cref UNIQUE (cref);
ALTER TABLE exercise ADD CONSTRAINT uk_exercise_name UNIQUE (name);
ALTER TABLE password_reset_token ADD CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash);

CREATE INDEX idx_user_name ON users (name);
CREATE INDEX idx_user_active ON users (active);
CREATE INDEX idx_user_email_hash ON users (email_hash);

CREATE INDEX idx_student_user_id ON student (user_id);
CREATE INDEX idx_student_cpf_hash ON student (cpf_hash);

CREATE INDEX idx_enrollment_student_id ON enrollment (student_id);
CREATE INDEX idx_enrollment_plan_id ON enrollment (plan_id);
CREATE INDEX idx_enrollment_status_canceled_at ON enrollment (status, canceled_at);
CREATE INDEX idx_enrollment_start_date ON enrollment (start_date);

CREATE INDEX idx_payment_status_due_date ON payment (status, due_date);
CREATE INDEX idx_payment_due_date ON payment (due_date);
CREATE INDEX idx_payment_enrollment_id ON payment (enrollment_id);

CREATE INDEX idx_presence_student_check_in_at ON presence (student_id, check_in_at);

CREATE INDEX idx_workout_sheet_student_id ON workout_sheet (student_id);
CREATE INDEX idx_workout_sheet_instructor_id ON workout_sheet (instructor_id);

CREATE INDEX idx_workout_sheet_exercise_sheet_id ON workout_sheet_exercise (workout_sheet_id);
CREATE INDEX idx_workout_sheet_exercise_exercise_id ON workout_sheet_exercise (exercise_id);

CREATE INDEX idx_retention_alert_student_id ON retention_alert (student_id);

CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);
CREATE INDEX idx_password_reset_token_expires_at ON password_reset_token (expires_at);

CREATE INDEX idx_audit_actor ON audit_log (actor_user_id);
CREATE INDEX idx_audit_resource ON audit_log (resource_type, resource_id);
CREATE INDEX idx_audit_action ON audit_log (action);
CREATE INDEX idx_audit_created ON audit_log (created_at);
