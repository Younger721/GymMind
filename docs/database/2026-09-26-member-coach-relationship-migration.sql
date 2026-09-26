-- GymMind relationship migration (MySQL 8+)
-- Run once during deployment after taking a backup. Statements are idempotent
-- where MySQL supports it; the preflight queries must be reviewed first.

-- 1. Create the membership table used by the application model.
CREATE TABLE IF NOT EXISTS member_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_number VARCHAR(64) NULL,
    phone VARCHAR(32) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_member_tenant_member_gym UNIQUE (member_id, tenant_id),
    INDEX idx_member_tenant_lookup (tenant_id, member_id)
);

-- 2. Preserve every legacy membership. The legacy tenant_id is retained on
-- member_profile for rollback compatibility until the next cleanup release.
INSERT INTO member_tenant (tenant_id, member_id, member_number, phone, status)
SELECT m.tenant_id, m.id, m.member_number, m.phone, COALESCE(m.status, 'ACTIVE')
FROM member_profile m
WHERE m.tenant_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM member_tenant mt
      WHERE mt.tenant_id = m.tenant_id AND mt.member_id = m.id
  );

-- 3. Ensure all relations are tenant-scoped and status-aware. Existing rows
-- become ACTIVE only when both the coach and member are in that tenant.
ALTER TABLE coach_member_assignment
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS requested_by BIGINT NULL,
    ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP(6) NULL,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP(6) NULL;

UPDATE coach_member_assignment a
JOIN coach c ON c.id = a.coach_id AND c.tenant_id = a.tenant_id
JOIN member_tenant mt ON mt.member_id = a.member_id AND mt.tenant_id = a.tenant_id
SET a.status = 'ACTIVE',
    a.requested_by = COALESCE(a.requested_by, a.member_id),
    a.requested_at = COALESCE(a.requested_at, a.created_at)
WHERE a.status IS NULL OR a.status NOT IN ('ACTIVE', 'PENDING', 'REJECTED', 'ENDED');

-- Rows that cannot be proven to belong to one tenant must not grant access.
UPDATE coach_member_assignment a
LEFT JOIN coach c ON c.id = a.coach_id AND c.tenant_id = a.tenant_id
LEFT JOIN member_tenant mt ON mt.member_id = a.member_id AND mt.tenant_id = a.tenant_id
SET a.status = 'ENDED',
    a.resolved_at = COALESCE(a.resolved_at, CURRENT_TIMESTAMP(6))
WHERE c.id IS NULL OR mt.member_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_coach_assignment_tenant_status
    ON coach_member_assignment (tenant_id, status, coach_id, member_id);

-- Deployment checks. Expected counts should be recorded in the release log.
SELECT COUNT(*) AS legacy_members_without_membership
FROM member_profile m
LEFT JOIN member_tenant mt ON mt.member_id = m.id AND mt.tenant_id = m.tenant_id
WHERE m.tenant_id IS NOT NULL AND mt.id IS NULL;

SELECT COUNT(*) AS cross_tenant_active_relations
FROM coach_member_assignment a
LEFT JOIN coach c ON c.id = a.coach_id AND c.tenant_id = a.tenant_id
LEFT JOIN member_tenant mt ON mt.member_id = a.member_id AND mt.tenant_id = a.tenant_id
WHERE a.status = 'ACTIVE' AND (c.id IS NULL OR mt.id IS NULL);
