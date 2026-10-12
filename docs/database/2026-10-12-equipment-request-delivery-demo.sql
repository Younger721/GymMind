-- 器械申请 / 配送示例（仅手动在 MySQL 执行，应用启动不会跑任何脚本）
-- 按「申请说明」去重：已有同名说明则跳过，不 DELETE、不 UPDATE 已有业务数据。
-- 库存增减请在本系统内「登记配送 / 确认入库」操作（与线上逻辑一致），本脚本只写单据。
-- 前置：2026-10-02-equipment-management.sql、2026-10-02-equipment-sample-data.sql

SET @now = NOW(6);

SET @tenant_id = (
    SELECT id FROM sys_tenant WHERE status = 'ACTIVE' ORDER BY id LIMIT 1
);

SET @gym_admin = (
    SELECT ua.id
    FROM sys_user ua
    INNER JOIN sys_user_role ur ON ur.user_id = ua.id
    INNER JOIN sys_role r ON r.id = ur.role_id AND r.code = 'GYM_ADMIN'
    WHERE ua.tenant_id = @tenant_id
    ORDER BY ua.id
    LIMIT 1
);

SET @platform_admin = (
    SELECT ua.id
    FROM sys_user ua
    INNER JOIN sys_user_role ur ON ur.user_id = ua.id
    INNER JOIN sys_role r ON r.id = ur.role_id AND r.code = 'PLATFORM_ADMIN'
    WHERE ua.tenant_id IS NULL
    ORDER BY ua.id
    LIMIT 1
);

SET @cat_treadmill = (SELECT id FROM equipment_catalog WHERE name = '跑步机' LIMIT 1);
SET @cat_bike = (SELECT id FROM equipment_catalog WHERE name = '动感单车' LIMIT 1);
SET @cat_elliptical = (SELECT id FROM equipment_catalog WHERE name = '椭圆机' LIMIT 1);
SET @cat_dumbbell = (SELECT id FROM equipment_catalog WHERE name = '哑铃' LIMIT 1);

-- 1. 待审批
INSERT INTO equipment_request
    (tenant_id, version, created_at, updated_at, requester_id, note, status)
SELECT @tenant_id, 0, @now, @now, @gym_admin, '示例：补充跑步机', 'PENDING'
FROM DUAL
WHERE @tenant_id IS NOT NULL AND @gym_admin IS NOT NULL AND @cat_treadmill IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM equipment_request WHERE tenant_id = @tenant_id AND note = '示例：补充跑步机'
  );

SET @req_pending = (
    SELECT id FROM equipment_request
    WHERE tenant_id = @tenant_id AND note = '示例：补充跑步机' LIMIT 1
);

INSERT INTO equipment_request_item
    (version, request_id, catalog_id, catalog_name, requested_quantity, approved_quantity, delivered_quantity, received_quantity)
SELECT 0, @req_pending, @cat_treadmill, '跑步机', 2, 0, 0, 0
FROM DUAL
WHERE @req_pending IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_request_item WHERE request_id = @req_pending);

-- 2. 已驳回
INSERT INTO equipment_request
    (tenant_id, version, created_at, updated_at, requester_id, note, status,
     reviewer_id, reviewed_at, review_comment)
SELECT @tenant_id, 0, @now, @now, @gym_admin, '示例：临时加购（已驳回）', 'REJECTED',
       @platform_admin, @now, '单次申请数量过大，请分批提交'
FROM DUAL
WHERE @tenant_id IS NOT NULL AND @gym_admin IS NOT NULL AND @platform_admin IS NOT NULL
  AND @cat_dumbbell IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM equipment_request WHERE tenant_id = @tenant_id AND note = '示例：临时加购（已驳回）'
  );

SET @req_rejected = (
    SELECT id FROM equipment_request
    WHERE tenant_id = @tenant_id AND note = '示例：临时加购（已驳回）' LIMIT 1
);

INSERT INTO equipment_request_item
    (version, request_id, catalog_id, catalog_name, requested_quantity, approved_quantity, delivered_quantity, received_quantity)
SELECT 0, @req_rejected, @cat_dumbbell, '哑铃', 50, 0, 0, 0
FROM DUAL
WHERE @req_rejected IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_request_item WHERE request_id = @req_rejected);

-- 3. 已批准待配送
INSERT INTO equipment_request
    (tenant_id, version, created_at, updated_at, requester_id, note, status,
     reviewer_id, reviewed_at, review_comment)
SELECT @tenant_id, 0, @now, @now, @gym_admin, '示例：团课区增配动感单车', 'APPROVED',
       @platform_admin, @now, '批准 3 台'
FROM DUAL
WHERE @tenant_id IS NOT NULL AND @gym_admin IS NOT NULL AND @platform_admin IS NOT NULL
  AND @cat_bike IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM equipment_request WHERE tenant_id = @tenant_id AND note = '示例：团课区增配动感单车'
  );

SET @req_approved = (
    SELECT id FROM equipment_request
    WHERE tenant_id = @tenant_id AND note = '示例：团课区增配动感单车' LIMIT 1
);

INSERT INTO equipment_request_item
    (version, request_id, catalog_id, catalog_name, requested_quantity, approved_quantity, delivered_quantity, received_quantity)
SELECT 0, @req_approved, @cat_bike, '动感单车', 3, 3, 0, 0
FROM DUAL
WHERE @req_approved IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_request_item WHERE request_id = @req_approved);

-- 4. 待确认入库（明细已含配送数量；配送单关联 request_id）
INSERT INTO equipment_request
    (tenant_id, version, created_at, updated_at, requester_id, note, status,
     reviewer_id, reviewed_at, review_comment)
SELECT @tenant_id, 0, @now, @now, @gym_admin, '示例：椭圆机更新', 'WAITING_RECEIVE',
       @platform_admin, @now, '批准 2 台'
FROM DUAL
WHERE @tenant_id IS NOT NULL AND @gym_admin IS NOT NULL AND @platform_admin IS NOT NULL
  AND @cat_elliptical IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM equipment_request WHERE tenant_id = @tenant_id AND note = '示例：椭圆机更新'
  );

SET @req_waiting = (
    SELECT id FROM equipment_request
    WHERE tenant_id = @tenant_id AND note = '示例：椭圆机更新' LIMIT 1
);

INSERT INTO equipment_request_item
    (version, request_id, catalog_id, catalog_name, requested_quantity, approved_quantity, delivered_quantity, received_quantity)
SELECT 0, @req_waiting, @cat_elliptical, '椭圆机', 2, 2, 2, 0
FROM DUAL
WHERE @req_waiting IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_request_item WHERE request_id = @req_waiting);

INSERT INTO equipment_delivery
    (tenant_id, version, created_at, updated_at, request_id, operator_id, total_quantity, delivered_at, note)
SELECT @tenant_id, 0, @now, @now, @req_waiting, @platform_admin, 2, @now, '示例配送：物流单 DEMO-001'
FROM DUAL
WHERE @req_waiting IS NOT NULL AND @platform_admin IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_delivery WHERE request_id = @req_waiting);

-- 5. 已完成入库
INSERT INTO equipment_request
    (tenant_id, version, created_at, updated_at, requester_id, note, status,
     reviewer_id, reviewed_at, review_comment)
SELECT @tenant_id, 0, @now, @now, @gym_admin, '示例：哑铃套装补货', 'RECEIVED',
       @platform_admin, @now, '批准 4 套'
FROM DUAL
WHERE @tenant_id IS NOT NULL AND @gym_admin IS NOT NULL AND @platform_admin IS NOT NULL
  AND @cat_dumbbell IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM equipment_request WHERE tenant_id = @tenant_id AND note = '示例：哑铃套装补货'
  );

SET @req_done = (
    SELECT id FROM equipment_request
    WHERE tenant_id = @tenant_id AND note = '示例：哑铃套装补货' LIMIT 1
);

INSERT INTO equipment_request_item
    (version, request_id, catalog_id, catalog_name, requested_quantity, approved_quantity, delivered_quantity, received_quantity)
SELECT 0, @req_done, @cat_dumbbell, '哑铃', 4, 4, 4, 4
FROM DUAL
WHERE @req_done IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_request_item WHERE request_id = @req_done);

INSERT INTO equipment_delivery
    (tenant_id, version, created_at, updated_at, request_id, operator_id, total_quantity, delivered_at, note)
SELECT @tenant_id, 0, @now, @now, @req_done, @platform_admin, 4, @now, '示例配送：已签收'
FROM DUAL
WHERE @req_done IS NOT NULL AND @platform_admin IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM equipment_delivery WHERE request_id = @req_done);
