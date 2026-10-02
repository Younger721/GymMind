-- 器械目录、配送、申请和健身房库存
CREATE TABLE IF NOT EXISTS equipment_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NULL,
    unit VARCHAR(16) NOT NULL DEFAULT '台',
    active BIT NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_catalog_name (name)
);

CREATE TABLE IF NOT EXISTS tenant_equipment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    catalog_id BIGINT NOT NULL,
    delivered_quantity INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    usable_quantity INT NOT NULL DEFAULT 0,
    abnormal_quantity INT NOT NULL DEFAULT 0,
    last_received_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_equipment_catalog (tenant_id, catalog_id),
    KEY idx_tenant_equipment_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS equipment_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    requester_id BIGINT NOT NULL,
    note VARCHAR(1000) NULL,
    status VARCHAR(24) NOT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    review_comment VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_equipment_request_tenant_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS equipment_request_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    request_id BIGINT NOT NULL,
    catalog_id BIGINT NOT NULL,
    catalog_name VARCHAR(128) NOT NULL,
    requested_quantity INT NOT NULL,
    approved_quantity INT NOT NULL DEFAULT 0,
    delivered_quantity INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_equipment_request_item_request (request_id),
    CONSTRAINT fk_equipment_request_item_request FOREIGN KEY (request_id) REFERENCES equipment_request (id)
);

CREATE TABLE IF NOT EXISTS equipment_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    request_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    delivered_at DATETIME(6) NOT NULL,
    note VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_equipment_delivery_tenant (tenant_id),
    KEY idx_equipment_delivery_request (request_id)
);
