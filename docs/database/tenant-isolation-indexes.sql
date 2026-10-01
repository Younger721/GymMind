-- GymMind shared-database/shared-table tenant isolation reference.
-- This file is intentionally documentation-first: apply equivalent statements
-- through the project's migration tool after checking the target database.

-- Every tenant-scoped root table must have tenant_id and a tenant-leading index.
-- Existing tables already declare tenant_id through TenantScopedEntity or an
-- explicit column; these statements document the expected physical indexes.

CREATE INDEX idx_member_profile_tenant ON member_profile (tenant_id, id);
CREATE INDEX idx_coach_profile_tenant ON coach_profile (tenant_id, id);
CREATE INDEX idx_coach_assignment_tenant ON coach_member_assignment (tenant_id, coach_id, member_id);
CREATE INDEX idx_course_schedule_tenant ON course_schedule (tenant_id, starts_at);
CREATE INDEX idx_course_booking_tenant ON course_booking (tenant_id, member_id, status);
CREATE INDEX idx_workout_plan_tenant ON workout_plan (tenant_id, member_id, status);
CREATE INDEX idx_workout_record_tenant ON workout_record (tenant_id, member_id, workout_date);
CREATE INDEX idx_exercise_tenant ON exercise (tenant_id, name);
CREATE INDEX idx_exercise_video_tenant ON exercise_video (tenant_id, exercise_id);
CREATE INDEX idx_tenant_agent_tenant ON tenant_agent (tenant_id, status);
CREATE INDEX idx_knowledge_document_tenant ON knowledge_document (tenant_id, status, id);
CREATE INDEX idx_knowledge_chunk_tenant_document ON knowledge_chunk (tenant_id, document_id, generation);
CREATE INDEX idx_ai_usage_tenant_created ON ai_usage_record (tenant_id, created_at);
CREATE INDEX idx_ai_memory_tenant_user ON ai_memory (tenant_id, user_id);
CREATE INDEX idx_operation_audit_tenant_created ON sys_operation_audit (tenant_id, created_at);

-- WorkoutPlanItem and WorkoutSet are child tables. They inherit the tenant
-- boundary through plan_id/record_id and must only be loaded by a service after
-- the parent has been resolved with findByTenantIdAndId. Do not expose a raw
-- findById/findAll endpoint for either child table.

-- For production migrations, use IF NOT EXISTS or the syntax supported by the
-- selected MySQL/PostgreSQL profile, and review existing index names first.
