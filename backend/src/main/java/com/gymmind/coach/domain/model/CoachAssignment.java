package com.gymmind.coach.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "coach_member_assignment")
public class CoachAssignment extends TenantScopedEntity {
    @Column(name = "coach_id", nullable = false) private Long coachId;
    @Column(name = "member_id", nullable = false) private Long memberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private CoachAssignmentStatus status = CoachAssignmentStatus.ACTIVE;
    @Column(name = "requested_by", nullable = false) private Long requestedBy;
    @Column(name = "requested_at", nullable = false) private java.time.Instant requestedAt = java.time.Instant.now();
    @Column(name = "resolved_at") private java.time.Instant resolvedAt;
    protected CoachAssignment() {}
    private CoachAssignment(Long tenantId, Long coachId, Long memberId) { super(tenantId); this.coachId = coachId; this.memberId = memberId; this.requestedBy = memberId; }
    public static CoachAssignment create(Long tenantId, Long coachId, Long memberId) { return new CoachAssignment(tenantId, coachId, memberId); }
    public static CoachAssignment createRequest(Long tenantId, Long coachId, Long memberId, Long requestedBy) {
        CoachAssignment value = new CoachAssignment(tenantId, coachId, memberId); value.requestedBy = requestedBy; value.status = CoachAssignmentStatus.PENDING; return value;
    }
    public void requestedBy(Long userId) { this.requestedBy = userId; }
    public void request() { requireStatus(CoachAssignmentStatus.REJECTED, CoachAssignmentStatus.ENDED); status = CoachAssignmentStatus.PENDING; resolvedAt = null; }
    public void accept() { requireStatus(CoachAssignmentStatus.PENDING); status = CoachAssignmentStatus.ACTIVE; resolvedAt = java.time.Instant.now(); }
    public void reject() { requireStatus(CoachAssignmentStatus.PENDING); status = CoachAssignmentStatus.REJECTED; resolvedAt = java.time.Instant.now(); }
    public void cancel() { requireStatus(CoachAssignmentStatus.PENDING); status = CoachAssignmentStatus.ENDED; resolvedAt = java.time.Instant.now(); }
    public void end() { requireStatus(CoachAssignmentStatus.ACTIVE); status = CoachAssignmentStatus.ENDED; resolvedAt = java.time.Instant.now(); }
    public void deactivate() { end(); }
    public Long getCoachId() { return coachId; }
    public Long getMemberId() { return memberId; }
    public boolean isActive() { return status == CoachAssignmentStatus.ACTIVE; }
    public CoachAssignmentStatus getStatus() { return status; }
    public Long getRequestedBy() { return requestedBy; }
    private void requireStatus(CoachAssignmentStatus... allowed) { for (CoachAssignmentStatus value : allowed) if (status == value) return; throw new IllegalStateException("Invalid assignment status: " + status); }
}
