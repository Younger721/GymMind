package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class DefaultCheckInService implements CheckInService {
    private final CourseRepository courses;
    private final BookingRepository bookings;
    private final MemberMembershipRepository memberships;
    private final AuditRecorder audit;

    public DefaultCheckInService(CourseRepository courses, BookingRepository bookings,
                                 MemberMembershipRepository memberships, AuditRecorder audit) {
        this.courses = courses;
        this.bookings = bookings;
        this.memberships = memberships;
        this.audit = audit;
    }

    @Override
    @Transactional
    public void checkIn(CurrentActor actor, Long memberId, Long courseId) {
        requirePermission(actor);
        var course = courses.findByTenantIdAndId(actor.tenantId(), courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Instant now = Instant.now();
        if (now.isBefore(course.getStartsAt()) || now.isAfter(course.getEndsAt())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Booking booking = bookings.findByTenantIdAndMemberIdAndCourseId(actor.tenantId(), memberId, courseId)
                .filter(value -> value.getStatus() == BookingStatus.CONFIRMED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        MemberMembership membership = memberships
                .findAllByTenantIdAndMemberIdAndStatus(actor.tenantId(), memberId, MemberMembershipStatus.ACTIVE)
                .stream()
                .filter(value -> usable(value, now))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT));

        membership.consume();
        memberships.save(membership);
        booking.complete();
        bookings.save(booking);
        audit.record(new AuditEvent("CHECKIN_RECORDED", "CHECKIN", courseId,
                AuditResult.SUCCESS, "checkin-service", Map.of("resourceName", "member-membership")));
    }

    private static boolean usable(MemberMembership membership, Instant now) {
        return membership.getRemainingCount() != null
                && membership.getRemainingCount() > 0
                && !now.isBefore(membership.getStartsAt())
                && (membership.getExpiresAt() == null || now.isBefore(membership.getExpiresAt()));
    }

    private static void requirePermission(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission("checkin:write")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
