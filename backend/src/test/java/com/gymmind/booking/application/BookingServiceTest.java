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
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
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

    @Test
    void platformAdminRequiresTenantContextForBookingList() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingService service = new DefaultBookingService(mock(CourseRepository.class), bookings,
                mock(MembershipEntitlementPort.class), mock(AuditRecorder.class), CLOCK);
        var platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "token");

        assertThatThrownBy(() -> service.list(platform))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TENANT_CONTEXT_REQUIRED);
        verify(bookings, never()).findAll();
    }

    @Test
    void platformAdminListsBookingsForSelectedTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        Booking first = Booking.create(11L, 41L, 4L);
        when(bookings.findAllByTenantId(11L)).thenReturn(java.util.List.of(first));
        BookingService service = new DefaultBookingService(courses, bookings, mock(MembershipEntitlementPort.class), mock(AuditRecorder.class), CLOCK);
        com.gymmind.shared.security.TenantContextHolder.set(11L);
        try {
            var result = service.list(new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "token"));
            assertThat(result).hasSize(1);
            verify(bookings).findAllByTenantId(11L);
        } finally {
            com.gymmind.shared.security.TenantContextHolder.clear();
        }
    }

    @Test
    void coachListsOnlyBookingsForCoursesTheyCreated() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        Coach coach = mock(Coach.class);
        when(coach.getId()).thenReturn(3L);
        when(coaches.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(coach));
        when(bookings.findAllByTenantId(11L)).thenReturn(java.util.List.of(
                Booking.create(11L, 41L, 4L), Booking.create(11L, 42L, 8L)));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(
                Course.create(11L, 3L, "My Yoga", "GROUP", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 10, "Studio")));
        when(courses.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                Course.create(11L, 9L, "Other Run", "GROUP", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 10, "Studio")));
        BookingService service = new DefaultBookingService(courses, bookings, mock(MembershipEntitlementPort.class),
                mock(AuditRecorder.class), CLOCK, members, coaches);

        assertThat(service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.COACH), Set.of(), 0L, "token")))
                .extracting(BookingView::courseId).containsExactly(4L);
    }

    @Test
    void gymAdminListsAllBookingsInTheirTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findAllByTenantId(11L)).thenReturn(java.util.List.of(
                Booking.create(11L, 41L, 4L), Booking.create(11L, 42L, 8L)));
        BookingService service = new DefaultBookingService(courses, bookings, mock(MembershipEntitlementPort.class),
                mock(AuditRecorder.class), CLOCK);

        assertThat(service.list(admin(11L))).hasSize(2);
    }

    @Test
    void memberListsOnlyOwnBookings() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(member));
        when(bookings.findAllByTenantId(11L)).thenReturn(java.util.List.of(
                Booking.create(11L, 41L, 4L), Booking.create(11L, 42L, 8L)));
        BookingService service = new DefaultBookingService(courses, bookings, mock(MembershipEntitlementPort.class),
                mock(AuditRecorder.class), CLOCK, members);

        assertThat(service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "token")))
                .extracting(BookingView::memberId).containsExactly(41L);
    }

    @Test
    void memberBooksForSelfAndBookingIsImmediatelyConfirmed() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        MemberRepository members = mock(MemberRepository.class);
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(member));
        Course course = Course.create(11L, 3L, "Yoga", "GROUP", FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200), 10, "Studio");
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, member.getId(), 4L)).thenReturn(Optional.empty());
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courses.reserveCapacity(11L, 4L)).thenReturn(true);
        BookingService service = new DefaultBookingService(courses, bookings, entitlements,
                mock(AuditRecorder.class), CLOCK, members);

        BookingView view = service.book(new CurrentActor(7L, 11L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "token"),
                new BookCourseCommand(null, null, 4L));

        assertThat(view.memberId()).isEqualTo(member.getId());
        assertThat(view.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(entitlements).reserve(11L, member.getId());
    }

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
        when(courses.reserveCapacity(11L, 4L)).thenReturn(true, false);

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        service.book(admin(11L), new BookCourseCommand(null, 41L, 4L));

        verify(entitlements).reserve(11L, 41L);
        assertThatThrownBy(() -> service.book(admin(11L), new BookCourseCommand(null, 42L, 4L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void bookingFailsWhenAtomicCapacityReservationFindsNoAvailableSeat() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Course course = Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 1, "Studio");
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.empty());
        when(courses.reserveCapacity(11L, 4L)).thenReturn(false);

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);

        assertThatThrownBy(() -> service.book(admin(11L), new BookCourseCommand(null, 41L, 4L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(entitlements).reserve(11L, 41L);
        verify(entitlements).release(11L, 41L);
        verify(bookings, never()).save(any(Booking.class));
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
    void cancelledBookingCanBeBookedAgain() {
        CourseRepository courses = mock(CourseRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        MembershipEntitlementPort entitlements = mock(MembershipEntitlementPort.class);
        Booking existing = Booking.create(11L, 41L, 4L);
        existing.cancel();
        Course course = Course.create(11L, 3L, "Yoga", "GROUP",
                FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200), 10, "Studio");
        when(bookings.findByTenantIdAndMemberIdAndCourseId(11L, 41L, 4L)).thenReturn(Optional.of(existing));
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courses.reserveCapacity(11L, 4L)).thenReturn(true);

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);

        BookingView view = service.book(admin(11L), new BookCourseCommand(null, 41L, 4L));

        assertThat(view.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(courses).reserveCapacity(11L, 4L);
        verify(entitlements).reserve(11L, 41L);
        verify(bookings).save(existing);
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
        when(courses.releaseCapacity(11L, 4L)).thenReturn(true);

        BookingService service = new DefaultBookingService(
                courses, bookings, entitlements, mock(AuditRecorder.class), CLOCK);
        BookingView view = service.cancel(admin(11L), 99L);

        assertThat(view.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(entitlements).release(11L, 41L);
        verify(courses).releaseCapacity(11L, 4L);
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
