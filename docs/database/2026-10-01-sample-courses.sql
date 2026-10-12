-- Idempotent demo data for the course center.
-- Run after the application has created/updated the course tables.
START TRANSACTION;

INSERT INTO coach_profile
    (version, created_at, updated_at, tenant_id, coach_number, full_name, phone, status, user_id)
SELECT 0, NOW(6), NOW(6), t.id, CONCAT('DEMO-', t.code, '-01'), CONCAT('李教练-', t.code), '13800000001', 'ACTIVE', NULL
FROM sys_tenant t
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM coach_profile c
      WHERE c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-01')
  );

INSERT INTO coach_profile
    (version, created_at, updated_at, tenant_id, coach_number, full_name, phone, status, user_id)
SELECT 0, NOW(6), NOW(6), t.id, CONCAT('DEMO-', t.code, '-02'), CONCAT('王教练-', t.code), '13800000002', 'ACTIVE', NULL
FROM sys_tenant t
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM coach_profile c
      WHERE c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-02')
  );

INSERT INTO course_schedule
    (version, created_at, updated_at, tenant_id, capacity, coach_id, ends_at, location,
     reserved_count, starts_at, status, title, course_type, description)
SELECT 0, NOW(6), NOW(6), t.id, 20, c.id,
       DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, '瑜伽教室', 0,
       DATE_ADD(NOW(), INTERVAL 2 DAY), 'SCHEDULED', '早晨瑜伽', '瑜伽',
       '适合所有水平的呼吸、柔韧与舒缓流瑜伽课程。'
FROM sys_tenant t
JOIN coach_profile c ON c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-01')
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM course_schedule s WHERE s.tenant_id = t.id AND s.title = '早晨瑜伽');

INSERT INTO course_schedule
    (version, created_at, updated_at, tenant_id, capacity, coach_id, ends_at, location,
     reserved_count, starts_at, status, title, course_type, description)
SELECT 0, NOW(6), NOW(6), t.id, 16, c.id,
       DATE_ADD(NOW(), INTERVAL 4 DAY) + INTERVAL 1 HOUR, '力量训练区', 0,
       DATE_ADD(NOW(), INTERVAL 4 DAY), 'SCHEDULED', '力量训练基础', '力量训练',
       '从基础动作开始，在教练指导下安全提升力量。'
FROM sys_tenant t
JOIN coach_profile c ON c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-02')
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM course_schedule s WHERE s.tenant_id = t.id AND s.title = '力量训练基础');

INSERT INTO course_schedule
    (version, created_at, updated_at, tenant_id, capacity, coach_id, ends_at, location,
     reserved_count, starts_at, status, title, course_type, description)
SELECT 0, NOW(6), NOW(6), t.id, 12, c.id,
       DATE_ADD(NOW(), INTERVAL 30 MINUTE), '综合训练区', 0,
       DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'SCHEDULED', '高强度间歇训练', '高强度间歇',
       '通过短时高强度间歇训练提升心肺能力和全身运动表现。'
FROM sys_tenant t
JOIN coach_profile c ON c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-01')
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM course_schedule s WHERE s.tenant_id = t.id AND s.title = '高强度间歇训练');

INSERT INTO course_schedule
    (version, created_at, updated_at, tenant_id, capacity, coach_id, ends_at, location,
     reserved_count, starts_at, status, title, course_type, description)
SELECT 0, NOW(6), NOW(6), t.id, 18, c.id,
       DATE_SUB(NOW(), INTERVAL 1 DAY), '普拉提教室', 0,
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED', '普拉提核心训练', '普拉提',
       '已结束的普拉提核心训练示例课程。'
FROM sys_tenant t
JOIN coach_profile c ON c.tenant_id = t.id AND c.coach_number = CONCAT('DEMO-', t.code, '-02')
WHERE t.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM course_schedule s WHERE s.tenant_id = t.id AND s.title = '普拉提核心训练');

-- Link the demo gym's sample coach to the built-in coach account.
UPDATE coach_profile c
JOIN sys_user u ON u.normalized_username = 'coach'
SET c.user_id = u.id,
    c.full_name = '李教练'
WHERE c.tenant_id = 1 AND c.coach_number = 'DEMO-demo-01';

-- Keep every demo-gym course manageable by that linked coach account.
UPDATE course_schedule s
JOIN coach_profile c ON c.tenant_id = 1 AND c.coach_number = 'DEMO-demo-01'
SET s.coach_id = c.id
WHERE s.tenant_id = 1
  AND s.title IN ('早晨瑜伽', '力量训练基础', '高强度间歇训练', '普拉提核心训练');

COMMIT;
