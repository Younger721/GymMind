-- 增量迁移：为动作库增加启用状态。
-- 仅新增字段并回填，不删除表、数据或历史计划。
ALTER TABLE exercise ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE exercise SET active = TRUE WHERE active IS NULL;

ALTER TABLE workout_plan_item ADD COLUMN IF NOT EXISTS exercise_name_snapshot VARCHAR(200) NULL;
UPDATE workout_plan_item i
JOIN exercise e ON e.id = i.exercise_id AND e.tenant_id = (SELECT tenant_id FROM workout_plan WHERE id = i.plan_id)
SET i.exercise_name_snapshot = e.name
WHERE i.exercise_name_snapshot IS NULL OR TRIM(i.exercise_name_snapshot) = '';

-- 历史模板项保留 exercise_id 与 exercise_name；缺失名称时回填动作快照。
UPDATE workout_plan_template_item i
JOIN workout_plan_template t ON t.id = i.template_id
JOIN exercise e ON e.id = i.exercise_id AND e.tenant_id = t.tenant_id
SET i.exercise_name = e.name
WHERE (i.exercise_name IS NULL OR TRIM(i.exercise_name) = '') AND i.exercise_id IS NOT NULL;
