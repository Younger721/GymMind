-- 增量迁移：为会员训练计划动作增加阶段和显示顺序。
-- 仅新增字段并回填历史记录，不删除或重建任何数据。
ALTER TABLE workout_plan_item
    ADD COLUMN IF NOT EXISTS phase VARCHAR(16) NOT NULL DEFAULT 'MAIN',
    ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;

-- 旧客户端创建的动作均归入正式组，并沿用训练日、记录 ID 的原有顺序。
WITH ranked_items AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY plan_id ORDER BY day_of_week ASC, id ASC) AS item_order
    FROM workout_plan_item
)
UPDATE workout_plan_item item
JOIN ranked_items ranked ON ranked.id = item.id
SET item.phase = 'MAIN', item.sort_order = ranked.item_order
WHERE item.sort_order = 0;
