package com.gymmind.coach.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "coach_member_assignment", uniqueConstraints = @UniqueConstraint(
        name = "uk_assignment_tenant_coach_member", columnNames = {"tenant_id", "coach_id", "member_id"}))
public class CoachAssignment extends TenantScopedEntity {
    @Column(name = "coach_id", nullable = false) private Long coachId;
    @Column(name = "member_id", nullable = false) private Long memberId;
    @Column(nullable = false) private boolean active = true;
    protected CoachAssignment() {}
    private CoachAssignment(Long tenantId, Long coachId, Long memberId) { super(tenantId); this.coachId = coachId; this.memberId = memberId; }
    public static CoachAssignment create(Long tenantId, Long coachId, Long memberId) { return new CoachAssignment(tenantId, coachId, memberId); }
    public void deactivate() { active = false; }
    public Long getCoachId() { return coachId; }
    public Long getMemberId() { return memberId; }
    public boolean isActive() { return active; }
}
