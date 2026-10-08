-- ERD table: users
-- No password column. keycloak_user_id links to the Keycloak account.
CREATE TABLE users (
    id                 UUID         PRIMARY KEY,
    keycloak_user_id   VARCHAR(64)  NOT NULL UNIQUE,
    email              VARCHAR(255) NOT NULL UNIQUE,
    full_name          VARCHAR(150) NOT NULL,
    student_code       VARCHAR(50)  UNIQUE,
    role               VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'INSTRUCTOR', 'STUDENT')),
    status             VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    preferred_language VARCHAR(2)   NOT NULL DEFAULT 'en' CHECK (preferred_language IN ('km', 'en')),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_email_lower ON users (lower(email));
CREATE INDEX idx_users_full_name_lower ON users (lower(full_name));
