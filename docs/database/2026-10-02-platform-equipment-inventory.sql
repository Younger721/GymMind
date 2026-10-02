-- 平台仓库库存和库存调整记录
CREATE TABLE IF NOT EXISTS platform_equipment_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    catalog_id BIGINT NOT NULL,
    warehouse_quantity INT NOT NULL DEFAULT 0,
    in_transit_quantity INT NOT NULL DEFAULT 0,
    outbound_quantity INT NOT NULL DEFAULT 0,
    last_adjusted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_equipment_catalog (catalog_id)
);

CREATE TABLE IF NOT EXISTS platform_equipment_adjustment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    catalog_id BIGINT NOT NULL,
    delta INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operator_id BIGINT NOT NULL,
    adjusted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_platform_equipment_adjustment_catalog (catalog_id),
    KEY idx_platform_equipment_adjustment_operator (operator_id)
);

-- 现有门店库存视为历史出库，门店已配送但尚未收货的数量计入配送中；
-- 每类器械额外预留 10 件平台库存，脚本可重复执行。
INSERT INTO platform_equipment_inventory
    (version, created_at, updated_at, catalog_id, warehouse_quantity,
     in_transit_quantity, outbound_quantity, last_adjusted_at)
SELECT
    0,
    NOW(6),
    NOW(6),
    c.id,
    10,
    COALESCE(SUM(e.delivered_quantity - e.received_quantity), 0),
    COALESCE(SUM(e.delivered_quantity), 0),
    NOW(6)
FROM equipment_catalog c
LEFT JOIN tenant_equipment e ON e.catalog_id = c.id
GROUP BY c.id
ON DUPLICATE KEY UPDATE catalog_id = VALUES(catalog_id);

INSERT INTO platform_equipment_adjustment
    (version, created_at, updated_at, catalog_id, delta, reason, operator_id, adjusted_at)
SELECT
    0, NOW(6), NOW(6), p.catalog_id, 10, '初始化平台仓库库存', 1, NOW(6)
FROM platform_equipment_inventory p
WHERE NOT EXISTS (
    SELECT 1 FROM platform_equipment_adjustment a
    WHERE a.catalog_id = p.catalog_id AND a.reason = '初始化平台仓库库存'
);
