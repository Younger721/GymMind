package com.gymmind.booking.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "course_booking",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_tenant_member_course",
                columnNames = {"tenant_id", "member_id", "course_id"}))
public class Booking extends TenantScopedEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status = BookingStatus.CONFIRMED;

    protected Booking() {
    }

    private Booking(Long tenantId, Long memberId, Long courseId) {
        super(tenantId);
        this.memberId = memberId;
        this.courseId = courseId;
    }

    public static Booking create(Long tenantId, Long memberId, Long courseId) {
        return new Booking(tenantId, memberId, courseId);
    }

    public void cancel() {
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel booking in status " + status);
        }
        status = BookingStatus.CANCELLED;
    }

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending booking can be confirmed");
        }
        status = BookingStatus.CONFIRMED;
    }

    public void complete() {
        status = BookingStatus.COMPLETED;
    }

    public void markNoShow() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed booking can be marked no-show");
        }
        status = BookingStatus.NO_SHOW;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
