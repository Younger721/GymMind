-- Additive migration for tenant-scoped role menu overrides.
-- Safe for existing data: this table is independent of the global role links.
CREATE TABLE IF NOT EXISTS sys_tenant_role_permission (
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, role_id, permission_id),
    CONSTRAINT fk_tenant_role_permission_tenant
        FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id),
    CONSTRAINT fk_tenant_role_permission_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_tenant_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
);

CREATE INDEX idx_tenant_role_permission_role
    ON sys_tenant_role_permission (tenant_id, role_id);
