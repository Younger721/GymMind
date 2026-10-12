-- 为所有启用中的健身房初始化常用器械目录（门店库存由申请/配送/入库流程写入，不在此脚本灌数）。
-- 脚本可重复执行：已存在的目录不会重复插入。

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
