CREATE SCHEMA IF NOT EXISTS user_schema;

CREATE TABLE IF NOT EXISTS user_schema.p_users (
    id UUID PRIMARY KEY,
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    hub_id UUID,
    company_id UUID,
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP NOT NULL,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID
);
INSERT INTO user_schema.p_users (
    id, keycloak_sub, email, name, role, status, created_at, updated_at
) VALUES (gen_random_uuid(),
          'b6f733f5-b813-4284-a05c-8250c454e802',
          'master@boxoffice.com',
          '최고관리자',
          'MASTER',
          'APPROVED',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
         )
    ON CONFLICT (keycloak_sub) DO NOTHING;

CREATE SCHEMA IF NOT EXISTS delivery_manager_schema;
CREATE SCHEMA IF NOT EXISTS company_schema;
CREATE SCHEMA IF NOT EXISTS ai_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;
