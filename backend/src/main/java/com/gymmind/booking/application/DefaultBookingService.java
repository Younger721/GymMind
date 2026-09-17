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
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DefaultBookingService(
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
    public BookingView book(CurrentActor actor, BookCourseCommand command) {
        requireWrite(actor, command == null ? null : command.tenantId());

        var existing = bookings.findByTenantIdAndMemberIdAndCourseId(
                actor.tenantId(), command.memberId(), command.courseId());
        if (existing.isPresent()) {
            return BookingView.from(existing.get());
        }

        entitlements.reserve(actor.tenantId(), command.memberId());
        try {
            reserveCourseCapacity(actor.tenantId(), command.courseId());
        } catch (RuntimeException ex) {
            entitlements.release(actor.tenantId(), command.memberId());
            throw ex;
        }

        Booking saved = bookings.save(Booking.create(actor.tenantId(), command.memberId(), command.courseId()));
        audit.record(new AuditEvent("BOOKING_CREATED", "BOOKING", saved.getId(),
                AuditResult.SUCCESS, "booking-service", Map.of()));
        return BookingView.from(saved);
    }

    @Override
    @Transactional
    public BookingView cancel(CurrentActor actor, Long bookingId) {
        requireWrite(actor, null);
        Booking booking = bookings.findByTenantIdAndId(actor.tenantId(), bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Course course = courses.findByTenantIdAndId(actor.tenantId(), booking.getCourseId())
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
        entitlements.release(actor.tenantId(), booking.getMemberId());
        Booking saved = bookings.save(booking);
        audit.record(new AuditEvent("BOOKING_CANCELLED", "BOOKING", saved.getId(),
                AuditResult.SUCCESS, "booking-service", Map.of()));
        return BookingView.from(saved);
    }

    @Override
    @Transactional
    public BookingView confirm(CurrentActor actor, Long bookingId) {
        requireWrite(actor, null);
        Booking booking = bookings.findByTenantIdAndId(actor.tenantId(), bookingId)
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
        requireWrite(actor, null);
        return bookings.findAllByTenantId(actor.tenantId()).stream().map(BookingView::from).toList();
    }

    private void ensureCancellationAllowed(Course course) {
        Instant now = clock.instant();
        if (!now.isBefore(course.getStartsAt())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private static void requireWrite(CurrentActor actor, Long commandTenantId) {
        if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission("booking:write")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (commandTenantId != null && !actor.tenantId().equals(commandTenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
