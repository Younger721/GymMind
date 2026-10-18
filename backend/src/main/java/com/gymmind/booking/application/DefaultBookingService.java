package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DefaultBookingService implements BookingService {

    private static final int CAPACITY_RETRY_ATTEMPTS = 3;

    private final CourseRepository courses;
    private final BookingRepository bookings;
    private final MembershipEntitlementPort entitlements;
    private final AuditRecorder audit;
    private final Clock clock;
    private final MemberRepository members;
    private final CoachRepository coaches;

    @Autowired
    public DefaultBookingService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock,
            MemberRepository members,
            CoachRepository coaches) {
        this.courses = courses;
        this.bookings = bookings;
        this.entitlements = entitlements;
        this.audit = audit;
        this.clock = clock;
        this.members = members;
        this.coaches = coaches;
    }

    public DefaultBookingService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock) {
        this(courses, bookings, entitlements, audit, clock, null, null);
    }

    public DefaultBookingService(
            CourseRepository courses,
            BookingRepository bookings,
            MembershipEntitlementPort entitlements,
            AuditRecorder audit,
            Clock clock,
            MemberRepository members) {
        this(courses, bookings, entitlements, audit, clock, members, null);
    }

    @Override
    @Transactional
    public BookingView book(CurrentActor actor, BookCourseCommand command) {
        requireBook(actor, command == null ? null : command.tenantId());
        if (command == null || command.courseId() == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);

        Long tenantId = ActorAccess.requireTenantId(actor);
        Long memberId = resolveMemberId(actor, command.memberId(), tenantId);
        var existing = bookings.findByTenantIdAndMemberIdAndCourseId(
                tenantId, memberId, command.courseId());
        if (existing.isPresent()) {
            Booking booking = existing.get();
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                return BookingView.from(booking);
            }

            Course course = courses.findByTenantIdAndId(tenantId, command.courseId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            ensureBookable(course);
            entitlements.reserve(tenantId, memberId);
            try {
                reserveCourseCapacity(tenantId, command.courseId());
            } catch (RuntimeException ex) {
                entitlements.release(tenantId, memberId);
                throw ex;
            }

            booking.reactivate();
            Booking saved = bookings.save(booking);
            audit.record(new AuditEvent("BOOKING_REACTIVATED", "BOOKING", saved.getId(),
                    AuditResult.SUCCESS, "booking-service", Map.of()));
            return BookingView.from(saved);
        }

        Course course = courses.findByTenantIdAndId(tenantId, command.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureBookable(course);
        entitlements.reserve(tenantId, memberId);
        try {
            reserveCourseCapacity(tenantId, command.courseId());
        } catch (RuntimeException ex) {
            entitlements.release(tenantId, memberId);
            throw ex;
        }

        Booking saved = bookings.save(Booking.create(tenantId, memberId, command.courseId()));
        audit.record(new AuditEvent("BOOKING_CREATED", "BOOKING", saved.getId(),
                AuditResult.SUCCESS, "booking-service", Map.of()));
        return BookingView.from(saved);
    }

    @Override
    @Transactional
    public BookingView cancel(CurrentActor actor, Long bookingId) {
        requireAuthenticated(actor);
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        Long tenantId = ActorAccess.requireTenantId(actor);
        Booking booking = bookings.findByTenantIdAndId(tenantId, bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (actor.roles().contains(RoleCode.MEMBER)
                && !isCurrentMember(actor, tenantId, booking.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!ActorAccess.canManageTenant(actor) && !actor.roles().contains(RoleCode.MEMBER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Course course = courses.findByTenantIdAndId(tenantId, booking.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureCancellationAllowed(course);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingView.from(booking);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        booking.cancel();
        course.release();
        courses.save(course);
        entitlements.release(tenantId, booking.getMemberId());
        Booking saved = bookings.save(booking);
        audit.record(new AuditEvent("BOOKING_CANCELLED", "BOOKING", saved.getId(),
                AuditResult.SUCCESS, "booking-service", Map.of()));
        return BookingView.from(saved);
    }

    @Override
    @Transactional
    public BookingView confirm(CurrentActor actor, Long bookingId) {
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!ActorAccess.canManageTenant(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        Booking booking = bookings.findByTenantIdAndId(ActorAccess.tenantId(actor), bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return BookingView.from(booking);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        booking.confirm();
        Booking saved = bookings.save(booking);
        audit.record(new AuditEvent("BOOKING_CONFIRMED", "BOOKING", saved.getId(),
                AuditResult.SUCCESS, "booking-service", Map.of()));
        return BookingView.from(saved);
    }

    private void reserveCourseCapacity(Long tenantId, Long courseId) {
        for (int attempt = 0; attempt < CAPACITY_RETRY_ATTEMPTS; attempt++) {
            Course course = courses.findByTenantIdAndId(tenantId, courseId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            try {
                course.reserve();
                courses.save(course);
                return;
            } catch (OptimisticLockException | IllegalStateException ex) {
                if (attempt == CAPACITY_RETRY_ATTEMPTS - 1) {
                    throw new BusinessException(ErrorCode.CONFLICT);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingView> list(CurrentActor actor) {
        return list(actor, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingView> list(CurrentActor actor, Long courseId) {
        requireAuthenticated(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        List<Booking> result;
        if (ActorAccess.isSuperAdmin(actor)) {
            if (tenantId == null) {
                throw new BusinessException(ErrorCode.TENANT_CONTEXT_REQUIRED);
            }
            result = bookings.findAllByTenantId(tenantId);
            if (courseId != null) {
                result = result.stream().filter(booking -> booking.getCourseId().equals(courseId)).toList();
            }
            return result.stream().map(this::view).toList();
        }
        result = bookings.findAllByTenantId(tenantId);
        if (actor.roles().contains(RoleCode.MEMBER)) {
            Long memberId = resolveMemberId(actor, null, tenantId);
            result = result.stream().filter(booking -> booking.getMemberId().equals(memberId)).toList();
        } else if (actor.roles().contains(RoleCode.COACH) && !ActorAccess.canManageTenant(actor)) {
            Long coachId = coaches == null ? null : coaches.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(coach -> coach.getId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            result = result.stream().filter(booking -> courses.findByTenantIdAndId(tenantId, booking.getCourseId())
                    .map(course -> course.getCoachId().equals(coachId)).orElse(false)).toList();
        }
        if (courseId != null) result = result.stream().filter(booking -> booking.getCourseId().equals(courseId)).toList();
        return result.stream().map(this::view).toList();
    }

    private void ensureCancellationAllowed(Course course) {
        Instant now = clock.instant();
        if (!now.isBefore(course.getStartsAt())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void ensureBookable(Course course) {
        if (course.getStatus() != com.gymmind.course.domain.model.CourseStatus.SCHEDULED
                || !course.getEndsAt().isAfter(clock.instant())
                || !course.getStartsAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private Long resolveMemberId(CurrentActor actor, Long requestedMemberId, Long tenantId) {
        if (actor.roles().contains(RoleCode.MEMBER)) {
            if (members == null) throw new BusinessException(ErrorCode.FORBIDDEN);
            Long ownId = members.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(member -> member.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if (requestedMemberId != null && !ownId.equals(requestedMemberId)) throw new BusinessException(ErrorCode.FORBIDDEN);
            return ownId;
        }
        if (!ActorAccess.canManageTenant(actor) || requestedMemberId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (members != null && members.findByTenantIdAndId(tenantId, requestedMemberId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return requestedMemberId;
    }

    private boolean isCurrentMember(CurrentActor actor, Long tenantId, Long memberId) {
        return members != null && members.findByTenantIdAndUserId(tenantId, actor.userId())
                .map(member -> member.getId().equals(memberId)).orElse(false);
    }

    private static void requireBook(CurrentActor actor, Long commandTenantId) {
        requireAuthenticated(actor);
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!actor.roles().contains(RoleCode.MEMBER) && !ActorAccess.canManageTenant(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.requireTenantId(actor);
        if (commandTenantId != null && !tenantId.equals(commandTenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireAuthenticated(CurrentActor actor) {
        if (actor == null || (ActorAccess.tenantId(actor) == null && !ActorAccess.isSuperAdmin(actor))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private BookingView view(Booking booking) {
        if (members == null) return BookingView.from(booking);
        return members.findByTenantIdAndId(booking.getTenantId(), booking.getMemberId())
                .map(member -> new BookingView(booking.getId(), booking.getTenantId(), booking.getMemberId(),
                        member.getFullName(), member.getPhone(), booking.getCourseId(), booking.getStatus(),
                        booking.getCheckedInAt()))
                .orElseGet(() -> BookingView.from(booking));
    }
}
