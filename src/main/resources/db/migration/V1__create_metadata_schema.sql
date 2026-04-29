-- Refinex-DBFlow 元数据库 V1 schema。
-- 约束：不存 Token 明文；审计表不存完整结果集。

create
database if not exists dbf;

use
dbf;

CREATE TABLE dbf_users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(128) NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255),
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_users PRIMARY KEY (id),
    CONSTRAINT uk_dbf_users_username UNIQUE (username),
    CONSTRAINT ck_dbf_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE dbf_projects
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    project_key VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_projects PRIMARY KEY (id),
    CONSTRAINT uk_dbf_projects_project_key UNIQUE (project_key),
    CONSTRAINT ck_dbf_projects_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE dbf_environments
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    environment_key VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_environments PRIMARY KEY (id),
    CONSTRAINT uk_dbf_environments_project_env UNIQUE (project_id, environment_key),
    CONSTRAINT fk_dbf_environments_project FOREIGN KEY (project_id) REFERENCES dbf_projects (id),
    CONSTRAINT ck_dbf_environments_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE dbf_api_tokens
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    token_prefix VARCHAR(32)  NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    active_flag  TINYINT,
    expires_at   TIMESTAMP(6),
    last_used_at TIMESTAMP(6),
    revoked_at   TIMESTAMP(6),
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_api_tokens PRIMARY KEY (id),
    CONSTRAINT uk_dbf_api_tokens_hash UNIQUE (token_hash),
    CONSTRAINT uk_dbf_api_tokens_user_active UNIQUE (user_id, active_flag),
    CONSTRAINT fk_dbf_api_tokens_user FOREIGN KEY (user_id) REFERENCES dbf_users (id),
    CONSTRAINT ck_dbf_api_tokens_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_dbf_api_tokens_active_flag CHECK (
        (status = 'ACTIVE' AND active_flag = 1)
            OR (status <> 'ACTIVE' AND active_flag IS NULL)
        )
);

CREATE TABLE dbf_user_env_grants
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    environment_id BIGINT       NOT NULL,
    grant_type     VARCHAR(32)  NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_user_env_grants PRIMARY KEY (id),
    CONSTRAINT uk_dbf_user_env_grants_user_env UNIQUE (user_id, environment_id),
    CONSTRAINT fk_dbf_user_env_grants_user FOREIGN KEY (user_id) REFERENCES dbf_users (id),
    CONSTRAINT fk_dbf_user_env_grants_environment FOREIGN KEY (environment_id) REFERENCES dbf_environments (id),
    CONSTRAINT ck_dbf_user_env_grants_type CHECK (grant_type IN ('READ', 'WRITE', 'ADMIN')),
    CONSTRAINT ck_dbf_user_env_grants_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE TABLE dbf_confirmation_challenges
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    environment_id  BIGINT       NOT NULL,
    confirmation_id VARCHAR(64)  NOT NULL,
    sql_hash        VARCHAR(128) NOT NULL,
    sql_text        TEXT         NOT NULL,
    risk_level      VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    expires_at      TIMESTAMP(6) NOT NULL,
    confirmed_at    TIMESTAMP(6),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_confirmation_challenges PRIMARY KEY (id),
    CONSTRAINT uk_dbf_confirmation_challenges_confirmation_id UNIQUE (confirmation_id),
    CONSTRAINT fk_dbf_confirmation_challenges_user FOREIGN KEY (user_id) REFERENCES dbf_users (id),
    CONSTRAINT fk_dbf_confirmation_challenges_environment FOREIGN KEY (environment_id) REFERENCES dbf_environments (id),
    CONSTRAINT ck_dbf_confirmation_challenges_risk CHECK (risk_level IN ('HIGH', 'CRITICAL')),
    CONSTRAINT ck_dbf_confirmation_challenges_status CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE dbf_audit_events
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    request_id      VARCHAR(64)  NOT NULL,
    user_id         BIGINT,
    token_prefix    VARCHAR(32),
    project_key     VARCHAR(128) NOT NULL,
    environment_key VARCHAR(128) NOT NULL,
    client_name     VARCHAR(128),
    client_version  VARCHAR(64),
    source_ip       VARCHAR(64),
    operation_type  VARCHAR(64)  NOT NULL,
    risk_level      VARCHAR(32)  NOT NULL,
    status          VARCHAR(64)  NOT NULL,
    sql_hash        VARCHAR(128),
    sql_text        TEXT,
    result_summary  TEXT,
    affected_rows   BIGINT,
    error_code      VARCHAR(128),
    error_message   TEXT,
    confirmation_id VARCHAR(64),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dbf_audit_events PRIMARY KEY (id),
    CONSTRAINT fk_dbf_audit_events_user FOREIGN KEY (user_id) REFERENCES dbf_users (id),
    CONSTRAINT ck_dbf_audit_events_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL', 'FORBIDDEN')),
    CONSTRAINT ck_dbf_audit_events_status CHECK (
        status IN ('ALLOWED_EXECUTED', 'DENIED', 'FAILED', 'REQUIRES_CONFIRMATION', 'CONFIRMATION_EXPIRED')
        )
);

CREATE INDEX idx_dbf_api_tokens_user_status ON dbf_api_tokens (user_id, status);
CREATE INDEX idx_dbf_environments_project_status ON dbf_environments (project_id, status);
CREATE INDEX idx_dbf_confirmation_user_status ON dbf_confirmation_challenges (user_id, status, expires_at);
CREATE INDEX idx_dbf_confirmation_environment_status ON dbf_confirmation_challenges (environment_id, status, expires_at);
CREATE INDEX idx_dbf_audit_user_time ON dbf_audit_events (user_id, created_at);
CREATE INDEX idx_dbf_audit_target_time ON dbf_audit_events (project_key, environment_key, created_at);
CREATE INDEX idx_dbf_audit_status_time ON dbf_audit_events (status, created_at);
CREATE INDEX idx_dbf_audit_sql_hash ON dbf_audit_events (sql_hash);
CREATE INDEX idx_dbf_audit_request_id ON dbf_audit_events (request_id);
