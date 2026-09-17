package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-11T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void bookingIsTenantScopedAndCapacityChecked() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Course course = Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 1, "Studio");
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.empty());
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.getClass(); // keep booking reference
            return booking;
        });
        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        service.book(admin(11L), new BookCourseCommand(null, 41L, 4L));

        verify(entitlements).reserve(11L, 41L);
        assertThatThrownBy(() -> service.book(admin(11L), new BookCourseCommand(null, 42L, 4L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void duplicateBookingIsIdempotent() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Booking existing = Booking.create(11L, 41L, 4L);
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.of(existing));

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        BookingView view = service.book(admin(11L), new BookCourseCommand(null, 41L, 4L));

        assertThat(view.memberId()).isEqualTo(41L);
        verify(entitlements, never()).reserve(11L, 41L);
        verify(courses, never()).save(any());
    }

    @Test
    void cancelReleasesCapacityAndEntitlement() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Booking booking = Booking.create(11L, 41L, 4L);
        Course course = Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 10, "Studio");
        course.reserve();
        when(bookings.findByTenantIdAndId(11L, 99L)).thenReturn(Optional.of(booking));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        BookingView view = service.cancel(admin(11L), 99L);

        assertThat(view.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(entitlements).release(11L, 41L);
        assertThat(course.getReservedCount()).isZero();
    }

    @Test
    void cancelAfterCourseStartsIsRejected() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Booking booking = Booking.create(11L, 41L, 4L);
        Course course = Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.minusSeconds(60), FIXED_NOW.plusSeconds(3600), 10, "Studio");
        when(bookings.findByTenantIdAndId(11L, 99L)).thenReturn(Optional.of(booking));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);

        assertThatThrownBy(() -> service.cancel(admin(11L), 99L))
                .isInstanceOf(BusinessException.class);
        verify(entitlements, never()).release(11L, 41L);
    }

    @Test
    void crossTenantBookingLookupReturnsNotFound() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        when(bookings.findByTenantIdAndId(11L, 99L)).thenReturn(Optional.empty());

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);

        assertThatThrownBy(() -> service.cancel(admin(11L), 99L))
                .isInstanceOf(BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("booking:write"), 0L, "token");
    }
}
