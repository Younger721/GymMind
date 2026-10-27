-- 本地开发库精简：保留 2 个健身房，每店 1 管理员 + 1 教练 + 1 会员
-- 保留 tenant: 1 (demo), 6 (rag-eval-a)
-- 保留 user: 1 (platform admin), 3,4,5 (tenant 1), 15,16,17 (tenant 6)

SET @keep_tenant := '1,6';
SET @keep_user := '1,3,4,5,15,16,17';

SET FOREIGN_KEY_CHECKS = 0;

-- 会话与消息（先子表）
DELETE FROM ai_chat_message
WHERE session_id IN (
    SELECT id FROM (
        SELECT s.id FROM ai_chat_session s
        WHERE s.tenant_id NOT IN (1, 6)
           OR s.user_id NOT IN (1, 3, 4, 5, 15, 16, 17)
    ) x
);
DELETE FROM ai_chat_session
WHERE tenant_id NOT IN (1, 6)
   OR user_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM ai_memory
WHERE tenant_id NOT IN (1, 6)
   OR user_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM ai_usage_record WHERE tenant_id NOT IN (1, 6) OR user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM async_job WHERE tenant_id NOT IN (1, 6);

DELETE FROM workout_set WHERE record_id IN (
    SELECT id FROM (SELECT id FROM workout_record WHERE tenant_id NOT IN (1, 6)) t
);
DELETE FROM workout_record WHERE tenant_id NOT IN (1, 6);

DELETE FROM workout_plan_item WHERE plan_id IN (
    SELECT id FROM (SELECT id FROM workout_plan WHERE tenant_id NOT IN (1, 6)) t
);
DELETE FROM workout_plan WHERE tenant_id NOT IN (1, 6);

DELETE FROM workout_plan_template_item WHERE template_id IN (
    SELECT id FROM (SELECT id FROM workout_plan_template WHERE tenant_id NOT IN (1, 6)) t
);
DELETE FROM workout_plan_template WHERE tenant_id NOT IN (1, 6);

DELETE FROM workout_plan_days WHERE plan_id IN (
    SELECT id FROM (SELECT id FROM workout_plans WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17)) t
);
DELETE FROM workout_plans WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM knowledge_document_audience_role WHERE document_id IN (
    SELECT id FROM (SELECT id FROM knowledge_document WHERE tenant_id IS NOT NULL AND tenant_id NOT IN (1, 6)) t
);
DELETE FROM knowledge_chunk WHERE tenant_id NOT IN (1, 6);
DELETE FROM knowledge_document WHERE tenant_id IS NOT NULL AND tenant_id NOT IN (1, 6);

DELETE FROM equipment_request_item WHERE request_id IN (
    SELECT id FROM (SELECT id FROM equipment_request WHERE tenant_id NOT IN (1, 6)) t
);
DELETE FROM equipment_request WHERE tenant_id NOT IN (1, 6);
DELETE FROM equipment_delivery WHERE tenant_id NOT IN (1, 6);
DELETE FROM tenant_equipment WHERE tenant_id NOT IN (1, 6);

DELETE FROM course_booking WHERE tenant_id NOT IN (1, 6);
DELETE FROM course_schedule WHERE tenant_id NOT IN (1, 6);

DELETE FROM coach_member_assignment WHERE tenant_id NOT IN (1, 6)
   OR coach_id NOT IN (1, 3, 4, 5, 15, 16, 17)
   OR member_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM coach_profile WHERE tenant_id NOT IN (1, 6) OR user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM member_profile WHERE tenant_id NOT IN (1, 6) OR user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM member_tenant WHERE tenant_id NOT IN (1, 6);
DELETE FROM member_membership WHERE tenant_id NOT IN (1, 6);
DELETE FROM member_workout_plan WHERE tenant_id NOT IN (1, 6);
DELETE FROM member_workout_schedule WHERE tenant_id NOT IN (1, 6);

DELETE FROM membership_order WHERE tenant_id NOT IN (1, 6);
DELETE FROM membership_package WHERE tenant_id NOT IN (1, 6);
DELETE FROM payment_record WHERE tenant_id NOT IN (1, 6);

DELETE FROM nutrition_record WHERE tenant_id NOT IN (1, 6);
DELETE FROM nutrition_plan WHERE tenant_id NOT IN (1, 6);
DELETE FROM nutrition_food WHERE tenant_id NOT IN (1, 6);

DELETE FROM exercise_video WHERE tenant_id NOT IN (1, 6);
DELETE FROM exercise WHERE tenant_id NOT IN (1, 6);
DELETE FROM gym_check_in WHERE tenant_id NOT IN (1, 6);

DELETE FROM tenant_agent WHERE tenant_id NOT IN (1, 6);
DELETE FROM tenant_ai_harness_config WHERE tenant_id NOT IN (1, 6);
DELETE FROM sys_operation_audit WHERE tenant_id IS NOT NULL AND tenant_id NOT IN (1, 6);
DELETE FROM sys_user_invitation WHERE tenant_id NOT IN (1, 6);

DELETE FROM post_likes WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM comments WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM posts WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM knowledge_images WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM challenge_participants WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM challenges WHERE creator_id NOT IN (1, 3, 4, 5, 15, 16, 17);

DELETE FROM sys_tenant_role_permission WHERE tenant_id NOT IN (1, 6);
DELETE FROM sys_tenant_quota WHERE tenant_id NOT IN (1, 6);
DELETE FROM sys_tenant_settings WHERE tenant_id NOT IN (1, 6);

DELETE FROM sys_user_role WHERE user_id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM sys_user WHERE id NOT IN (1, 3, 4, 5, 15, 16, 17);
DELETE FROM sys_tenant WHERE id NOT IN (1, 6);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'tenants' AS kind, id, code, name FROM sys_tenant ORDER BY id;
SELECT 'users' AS kind, u.id, u.tenant_id, u.normalized_username, r.code AS role
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role r ON r.id = ur.role_id
ORDER BY COALESCE(u.tenant_id, 0), u.id;
