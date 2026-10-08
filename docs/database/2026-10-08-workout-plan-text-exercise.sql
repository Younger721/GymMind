-- 增量迁移：训练计划动作改为支持自由文本名称。
-- 仅允许列约束变更和历史快照回填，不删除动作库或已有计划数据。
ALTER TABLE workout_plan_item MODIFY COLUMN exercise_id BIGINT NULL;
ALTER TABLE workout_plan_item ADD COLUMN IF NOT EXISTS exercise_name_snapshot VARCHAR(200) NULL;
UPDATE workout_plan_item i
LEFT JOIN exercise e ON e.id = i.exercise_id
SET i.exercise_name_snapshot = COALESCE(NULLIF(TRIM(i.exercise_name_snapshot), ''), e.name,
    CASE WHEN i.exercise_id IS NULL THEN '未命名动作' ELSE CONCAT('动作 #', i.exercise_id) END)
WHERE i.exercise_name_snapshot IS NULL OR TRIM(i.exercise_name_snapshot) = '';
ALTER TABLE workout_plan_item MODIFY COLUMN exercise_name_snapshot VARCHAR(200) NOT NULL;

-- 模板动作名称已在历史迁移中补齐；保留 exercise_id 以兼容旧客户端和历史查询。

-- 历史选择记录转换为会员可编辑草稿。额外关联列用于幂等回填及旧排期追溯。
ALTER TABLE workout_plan ADD COLUMN IF NOT EXISTS legacy_member_plan_id BIGINT NULL;
ALTER TABLE member_workout_plan ADD COLUMN IF NOT EXISTS workout_plan_id BIGINT NULL;
INSERT INTO workout_plan (tenant_id, member_id, coach_id, name, goal, start_date, end_date, status, description,
    legacy_member_plan_id, version, created_at, updated_at)
SELECT mp.tenant_id, mp.member_id, t.coach_id, mp.name, t.goal, CURRENT_DATE(),
    DATE_ADD(CURRENT_DATE(), INTERVAL 1 MONTH), 'DRAFT', t.description, mp.id, 0, NOW(), NOW()
FROM member_workout_plan mp JOIN workout_plan_template t ON t.id = mp.template_id AND t.tenant_id = mp.tenant_id
LEFT JOIN workout_plan p ON p.legacy_member_plan_id = mp.id
WHERE mp.workout_plan_id IS NULL AND p.id IS NULL;
UPDATE member_workout_plan mp JOIN workout_plan p ON p.legacy_member_plan_id = mp.id
SET mp.workout_plan_id = p.id WHERE mp.workout_plan_id IS NULL;
INSERT INTO workout_plan_item (plan_id, exercise_id, exercise_name_snapshot, day_of_week, phase, sort_order,
    sets, reps, rest_seconds, weight, notes, version)
SELECT p.id, ti.exercise_id, COALESCE(NULLIF(TRIM(ti.exercise_name), ''), e.name, CONCAT('动作 #', ti.exercise_id)),
    1, ti.phase, ti.sort_order, ti.sets, ti.reps, ti.rest_seconds, ti.weight, ti.notes, 0
FROM member_workout_plan mp JOIN workout_plan p ON p.id = mp.workout_plan_id
JOIN workout_plan_template_item ti ON ti.template_id = mp.template_id
LEFT JOIN exercise e ON e.id = ti.exercise_id AND e.tenant_id = mp.tenant_id
WHERE NOT EXISTS (SELECT 1 FROM workout_plan_item wi WHERE wi.plan_id = p.id);

-- 新排期使用 workout_plan_id；历史 member_plan_id 继续保留和兼容读取。
ALTER TABLE member_workout_schedule ADD COLUMN IF NOT EXISTS workout_plan_id BIGINT NULL;
UPDATE member_workout_schedule s JOIN member_workout_plan mp ON mp.id = s.member_plan_id
SET s.workout_plan_id = mp.workout_plan_id WHERE s.workout_plan_id IS NULL;
ALTER TABLE member_workout_schedule MODIFY COLUMN member_plan_id BIGINT NULL;
