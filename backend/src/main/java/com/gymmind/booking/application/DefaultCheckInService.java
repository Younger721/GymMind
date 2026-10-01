package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.booking.domain.model.GymCheckIn;
import com.gymmind.booking.domain.repository.GymCheckInRepository;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final MemberRepository members;
    private final GymCheckInRepository gymCheckIns;

    @Autowired
    public DefaultCheckInService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock,
            MemberRepository members,
            GymCheckInRepository gymCheckIns) {
        this.courses = courses;
        this.bookings = bookings;
        this.entitlements = entitlements;
        this.audit = audit;
        this.clock = clock;
        this.members = members;
        this.gymCheckIns = gymCheckIns;
    }

    public DefaultCheckInService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock) {
        this(courses, bookings, entitlements, audit, clock, null, new GymCheckInRepository() {
            @Override
            public GymCheckIn save(GymCheckIn checkIn) { return checkIn; }

            @Override
            public java.util.List<GymCheckIn> findByTenantIdAndMemberIdAndCheckInAtBetween(
                    Long tenantId, Long memberId, Instant from, Instant to) {
                return java.util.List.of();
            }

            @Override
            public long countByTenantIdAndMemberIdAndCheckInAtBetween(
                    Long tenantId, Long memberId, Instant from, Instant to) {
                return 0L;
            }
        });
    }

    @Override
    @Transactional
    public void checkInByToken(CurrentActor actor, String token, Long courseId) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (token == null || token.isBlank() || tenantId == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        var member = members.findByTenantIdAndCheckInToken(tenantId, token.trim())
                .filter(value -> value.getStatus() == com.gymmind.member.domain.model.MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        requirePermission(actor);
        if (courseId == null) {
            gymCheckIns.save(GymCheckIn.create(tenantId, member.getId(), clock.instant(), actor.userId(), null));
            return;
        }
        checkIn(actor, member.getId(), courseId);
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
        booking.complete(now);
        bookings.save(booking);
        gymCheckIns.save(GymCheckIn.create(tenantId, memberId, now, actor.userId(), courseId));
        audit.record(new AuditEvent("CHECKIN_RECORDED", "CHECKIN", courseId,
                AuditResult.SUCCESS, "checkin-service", Map.of("resourceName", "member-membership")));
    }

    private static void requirePermission(CurrentActor actor) {
        ActorAccess.requireGymAdmin(actor, "checkin:write");
    }
}
