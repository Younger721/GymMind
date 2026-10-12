-- 为所有启用中的健身房初始化常用器械目录与期初门店库存。
-- 后续申请/配送/入库由业务接口联动增减；本脚本只补「尚未存在」的行，不覆盖已有数据。
-- 脚本可重复执行。

INSERT INTO equipment_catalog
    (version, created_at, updated_at, name, category, description, unit, active)
SELECT
    0,
    NOW(6),
    NOW(6),
    seed.name,
    seed.category,
    seed.description,
    seed.unit,
    b'1'
FROM (
    SELECT '跑步机' AS name, '有氧器械' AS category, '用于跑步和快走训练的电动有氧设备' AS description, '台' AS unit
    UNION ALL SELECT '动感单车', '有氧器械', '用于室内骑行和团体单车课程', '台'
    UNION ALL SELECT '椭圆机', '有氧器械', '低冲击全身有氧训练设备', '台'
    UNION ALL SELECT '划船机', '有氧器械', '用于模拟划船动作的全身训练设备', '台'
    UNION ALL SELECT '哑铃', '力量器械', '适用于上肢和全身力量训练', '套'
    UNION ALL SELECT '杠铃架', '力量器械', '用于深蹲、卧推等杠铃训练', '台'
) seed
LEFT JOIN equipment_catalog existing ON existing.name = seed.name
WHERE existing.id IS NULL;

INSERT INTO tenant_equipment
    (tenant_id, version, created_at, updated_at, catalog_id,
     delivered_quantity, received_quantity, usable_quantity, abnormal_quantity, last_received_at)
SELECT
    tenant.id,
    0,
    NOW(6),
    NOW(6),
    catalog.id,
    seed.quantity,
    seed.quantity,
    seed.quantity,
    0,
    NOW(6)
FROM sys_tenant tenant
JOIN (
    SELECT '跑步机' AS name, 8 AS quantity
    UNION ALL SELECT '动感单车', 12
    UNION ALL SELECT '椭圆机', 6
    UNION ALL SELECT '划船机', 4
    UNION ALL SELECT '哑铃', 20
    UNION ALL SELECT '杠铃架', 4
) seed ON 1 = 1
JOIN equipment_catalog catalog ON catalog.name = seed.name
WHERE tenant.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM tenant_equipment existing
      WHERE existing.tenant_id = tenant.id
        AND existing.catalog_id = catalog.id
  );
