package com.gymmind.booking.application;
import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.booking.domain.model.*;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckInServiceTest {
    @Test
    void checkInRequiresConfirmedBooking() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        Course c = activeCourse();
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(c));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.empty());

        CheckInService service = new DefaultCheckInService(courses, bookings, memberships, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.checkIn(admin(11L), 41L, 4L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verifyNoInteractions(memberships);
    }

    @Test
    void checkInConsumesActiveMembershipEntitlement() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        Booking booking = Booking.create(11L, 41L, 4L);
        MemberMembership membership = MemberMembership.create(11L, 41L, 8L, 3,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(activeCourse()));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.of(booking));
        when(memberships.findAllByTenantIdAndMemberIdAndStatus(11L, 41L,
                com.gymmind.membership.domain.model.MemberMembershipStatus.ACTIVE)).thenReturn(java.util.List.of(membership));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any(MemberMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInService service = new DefaultCheckInService(courses, bookings, memberships, mock(AuditRecorder.class));
        service.checkIn(admin(11L), 41L, 4L);

        verify(memberships).save(membership);
        org.assertj.core.api.Assertions.assertThat(membership.getRemainingCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void checkInRequiresAvailableMembershipEntitlement() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(activeCourse()));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.of(Booking.create(11L, 41L, 4L)));
        when(memberships.findAllByTenantIdAndMemberIdAndStatus(11L, 41L,
                com.gymmind.membership.domain.model.MemberMembershipStatus.ACTIVE)).thenReturn(java.util.List.of());

        CheckInService service = new DefaultCheckInService(courses, bookings, memberships, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.checkIn(admin(11L), 41L, 4L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    private static Course activeCourse() {
        return Course.create(11L, 3L, "Yoga", "GROUP", Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600), 10, "Studio");
    }

    private static CurrentActor admin(long t) {
        return new CurrentActor(1L, t, Set.of(RoleCode.GYM_ADMIN), Set.of("checkin:write"), 0L, "token");
    }
}
