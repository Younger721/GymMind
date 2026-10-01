package com.gymmind.booking.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "gym_check_in")
public class GymCheckIn extends TenantScopedEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;
    @Column(name = "operator_user_id")
    private Long operatorUserId;
    @Column(name = "course_id")
    private Long courseId;

    protected GymCheckIn() {}
    private GymCheckIn(Long tenantId, Long memberId, Instant checkInAt, Long operatorUserId, Long courseId) {
        super(tenantId); this.memberId = memberId; this.checkInAt = checkInAt;
        this.operatorUserId = operatorUserId; this.courseId = courseId;
    }
    public static GymCheckIn create(Long tenantId, Long memberId, Instant at, Long operatorUserId, Long courseId) {
        return new GymCheckIn(tenantId, memberId, at, operatorUserId, courseId);
    }
    public Long getMemberId() { return memberId; }
    public Instant getCheckInAt() { return checkInAt; }
}
