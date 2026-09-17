package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckInServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void checkInRequiresConfirmedBooking() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(activeCourse()));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.empty());

        CheckInService service = new DefaultCheckInService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);

        assertThatThrownBy(() -> service.checkIn(admin(11L), 41L, 4L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verifyNoInteractions(entitlements);
    }

    @Test
    void checkInConsumesEntitlementAndCompletesBooking() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Booking booking = Booking.create(11L, 41L, 4L);
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(activeCourse()));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInService service = new DefaultCheckInService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        service.checkIn(admin(11L), 41L, 4L);

        verify(entitlements).consume(11L, 41L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    private static Course activeCourse() {
        return Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.minusSeconds(60), FIXED_NOW.plusSeconds(3600), 10, "Studio");
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("checkin:write"), 0L, "token");
    }
}
