-- Training management plans no longer require the exercise catalog.
-- Run once before deploying the application version that writes exercise_name.
ALTER TABLE workout_plan_template_item ADD COLUMN IF NOT EXISTS exercise_name VARCHAR(200) NULL;

UPDATE workout_plan_template_item i
LEFT JOIN exercise e ON e.id = i.exercise_id AND e.tenant_id = (
    SELECT tenant_id FROM workout_plan_template WHERE id = i.template_id
)
SET i.exercise_name = COALESCE(NULLIF(TRIM(i.exercise_name), ''), e.name, CONCAT('动作 #', i.exercise_id))
WHERE i.exercise_name IS NULL OR TRIM(i.exercise_name) = '';

ALTER TABLE workout_plan_template_item MODIFY COLUMN exercise_name VARCHAR(200) NOT NULL;
-- exercise_id is retained for historical compatibility and can be dropped after
-- all legacy readers have been retired:
-- ALTER TABLE workout_plan_template_item DROP COLUMN exercise_id;
