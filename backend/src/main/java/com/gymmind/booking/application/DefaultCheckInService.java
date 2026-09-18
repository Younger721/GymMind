package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
public class DefaultCheckInService implements CheckInService {

    private final CourseRepository courses;
    private final BookingRepository bookings;
    private final MembershipEntitlementPort entitlements;
    private final AuditRecorder audit;
    private final Clock clock;

    public DefaultCheckInService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock) {
        this.courses = courses;
        this.bookings = bookings;
        this.entitlements = entitlements;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void checkIn(CurrentActor actor, Long memberId, Long courseId) {
        requirePermission(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        var course = courses.findByTenantIdAndId(tenantId, courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Instant now = clock.instant();
        if (now.isBefore(course.getStartsAt()) || now.isAfter(course.getEndsAt())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Booking booking = bookings.findByTenantIdAndMemberIdAndCourseId(tenantId, memberId, courseId)
                .filter(value -> value.getStatus() == BookingStatus.CONFIRMED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        entitlements.consume(tenantId, memberId);
        booking.complete();
        bookings.save(booking);
        audit.record(new AuditEvent("CHECKIN_RECORDED", "CHECKIN", courseId,
                AuditResult.SUCCESS, "checkin-service", Map.of("resourceName", "member-membership")));
    }

    private static void requirePermission(CurrentActor actor) {
        ActorAccess.requireGymAdmin(actor, "checkin:write");
    }
}
