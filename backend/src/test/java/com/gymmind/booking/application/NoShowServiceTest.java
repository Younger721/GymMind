package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoShowServiceTest {
    @Test
    void marksOnlyConfirmedBookingsWhoseCourseHasEnded() {
        BookingRepository bookings = mock(BookingRepository.class);
        CourseRepository courses = mock(CourseRepository.class);
        Booking ended = Booking.create(11L, 41L, 4L);
        Booking future = Booking.create(11L, 42L, 5L);
        when(bookings.findAllByTenantIdAndStatus(11L, BookingStatus.CONFIRMED)).thenReturn(List.of(ended, future));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(courseAt(11L, 4L, -3600)));
        when(courses.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(courseAt(11L, 5L, 3600)));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        new DefaultNoShowService(bookings, courses, mock(com.gymmind.booking.application.port.MembershipEntitlementPort.class), mock(AuditRecorder.class))
                .markTenantNoShows(11L, Instant.now());

        assertThat(ended.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(future.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookings).save(ended);
        verify(bookings, never()).save(future);
    }

    @Test
    void missingTenantContextFailsClosed() {
        NoShowService service = new DefaultNoShowService(mock(BookingRepository.class),
                mock(CourseRepository.class), mock(com.gymmind.booking.application.port.MembershipEntitlementPort.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.markTenantNoShows(null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Course courseAt(long tenantId, long courseId, long endOffsetSeconds) {
        Instant end = Instant.now().plusSeconds(endOffsetSeconds);
        return Course.create(tenantId, 3L, "Yoga-" + courseId, "GROUP", end.minusSeconds(3600), end, 10, "Studio");
    }
}
