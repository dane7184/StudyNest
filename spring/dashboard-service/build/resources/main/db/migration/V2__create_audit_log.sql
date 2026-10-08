-- Local audit log of admin actions. Login events are read from Keycloak, not stored here.
CREATE TABLE audit_log (
    id                UUID        PRIMARY KEY,
    action            VARCHAR(50) NOT NULL,
    actor_keycloak_id VARCHAR(64) NOT NULL,
    target_type       VARCHAR(30),
    target_id         VARCHAR(64),
    details           TEXT,
    ip_address        VARCHAR(45),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_keycloak_id);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
