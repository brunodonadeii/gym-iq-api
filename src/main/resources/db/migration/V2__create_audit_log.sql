CREATE TABLE IF NOT EXISTS audit_log (
    id_audit_log BIGSERIAL PRIMARY KEY,
    actor_user_id INTEGER,
    actor_email VARCHAR(150),
    actor_role VARCHAR(20),
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(40),
    resource_id INTEGER,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_actor
    ON audit_log (actor_user_id);

CREATE INDEX IF NOT EXISTS idx_audit_resource
    ON audit_log (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_audit_action
    ON audit_log (action);

CREATE INDEX IF NOT EXISTS idx_audit_created
    ON audit_log (created_at);
