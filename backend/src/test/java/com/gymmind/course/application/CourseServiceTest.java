package com.gymmind.course.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

class CourseServiceTest {

    @Test
    void platformAdminMustSelectTenantBeforeListingCourses() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.list(platformActor(), null, null, null, null, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TENANT_CONTEXT_REQUIRED);
        verifyNoInteractions(courses);
    }

    @Test
    void platformAdminReadsCourseDetailsOnlyInSelectedTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Course course = futureCourse(11L, 8L, "Run");
        when(courses.findByTenantIdAndId(11L, 42L)).thenReturn(Optional.of(course));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        TenantContextHolder.set(11L);
        try {
            assertThat(service.find(platformActor(), 42L).tenantId()).isEqualTo(11L);
            verify(courses).findByTenantIdAndId(11L, 42L);
            verify(courses, never()).findById(42L);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void platformAdminListsOnlySelectedTenantsCourses() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        when(courses.findAllByTenantId(11L)).thenReturn(java.util.List.of(futureCourse(11L, 3L, "Yoga")));
        TenantContextHolder.set(11L);
        try {
            CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

            assertThat(service.list(platformActor(), null, null, null, null, false))
                    .extracting(CourseView::tenantId).containsExactly(11L);
            verify(courses).findAllByTenantId(11L);
            verify(courses, never()).findAll();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void coachListsAllCoursesInOwnTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Coach ownCoach = mock(Coach.class);
        when(ownCoach.getId()).thenReturn(3L);
        when(coaches.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(ownCoach));
        when(courses.findAllByTenantId(11L)).thenReturn(java.util.List.of(
                futureCourse(11L, 3L, "My Yoga"), futureCourse(11L, 8L, "Other Coach Run")));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThat(service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.COACH), Set.of(), 0L, "token"),
                null, null, null, null, false))
                .hasSize(2);
    }

    @Test
    void coachCanReadAnotherCoachsCourseDetailsInOwnTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Coach ownCoach = mock(Coach.class);
        when(ownCoach.getId()).thenReturn(3L);
        Course otherCourse = futureCourse(11L, 8L, "Other Coach Run");
        when(coaches.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(ownCoach));
        when(courses.findByTenantIdAndId(11L, 42L)).thenReturn(Optional.of(otherCourse));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThat(service.find(new CurrentActor(7L, 11L, Set.of(RoleCode.COACH), Set.of(), 0L, "token"), 42L).title())
                .isEqualTo("Other Coach Run");
    }

    @Test
    void tenantRolesListAllCoursesInTheirTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        when(courses.findAllByTenantId(11L)).thenReturn(java.util.List.of(
                futureCourse(11L, 3L, "Yoga"), futureCourse(11L, 8L, "Run")));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThat(service.list(admin(11L))).hasSize(2);
        assertThat(service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "token"))).hasSize(2);
    }

    @Test
    void coachCreatesCourseBoundToOwnProfile() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Coach coach = mock(Coach.class);
        when(coach.getId()).thenReturn(3L);
        when(coaches.findByTenantIdAndUserId(11L, 7L)).thenReturn(Optional.of(coach));
        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        service.create(new CurrentActor(7L, 11L, Set.of(RoleCode.COACH), Set.of(), 0L, "token"),
                new CreateCourseCommand(null, 999L, "Yoga", "GROUP", Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(7200), 10, "Studio"));

        verify(coaches).findByTenantIdAndUserId(11L, 7L);
        verify(courses).save(org.mockito.ArgumentMatchers.argThat(course -> course.getCoachId().equals(3L)));
    }

    @Test
    void createRejectsCoachFromAnotherTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.empty());
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateCourseCommand(null, 3L, "Yoga", "GROUP",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void updateRejectsCapacityBelowReservedAndStartedChanges() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Course course = futureCourse(11L, 3L, "Yoga");
        course.reserve();
        course.reserve();
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.update(admin(11L), 4L, "Yoga", "GROUP", course.getStartsAt(),
                course.getEndsAt(), 1, "Studio"))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);

        Course started = Course.create(11L, 3L, "Run", "GROUP", Instant.now().minusSeconds(10),
                Instant.now().plusSeconds(3600), 10, "Track");
        when(courses.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(started));
        assertThatThrownBy(() -> service.update(admin(11L), 5L, "Changed", "GROUP", started.getStartsAt(),
                started.getEndsAt(), 10, "Track"))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void platformAdminCannotUpdateOrCancelTenantCourses() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseService service = new DefaultCourseService(courses, mock(CoachRepository.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.update(platformActor(), 42L, "Updated Run", "GROUP", Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200), 10, "Studio")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.cancel(platformActor(), 42L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(courses);
    }

    @Test
    void platformAdminCreationIsRejectedEvenWithExplicitTargetTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        TenantRepository tenants = mock(TenantRepository.class);
        Tenant tenant = mock(Tenant.class);
        Coach coach = mock(Coach.class);
        when(tenant.isActive()).thenReturn(true);
        when(tenants.findById(22L)).thenReturn(Optional.of(tenant));
        when(coaches.findByTenantIdAndId(22L, 8L)).thenReturn(Optional.of(coach));
        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class), null, null, tenants);

        assertThatThrownBy(() -> service.create(platformActor(), new CreateCourseCommand(22L, 8L, "Run", "GROUP",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio", "Intervals")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(courses, coaches, tenants);
    }

    @Test
    void gymAdminCreationIsRejected() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateCourseCommand(null, 8L, "Run", "GROUP",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(courses, coaches);
    }

    @Test
    void platformAdminCreationDoesNotRequireTargetTenantOnceRejected() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        TenantRepository tenants = mock(TenantRepository.class);
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class), null, null, tenants);

        assertThatThrownBy(() -> service.create(platformActor(), new CreateCourseCommand(null, 8L, "Run", "GROUP",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(courses, coaches, tenants);
    }

    private static Course futureCourse(long tenantId, long coachId, String title) {
        return Course.create(tenantId, coachId, title, "GROUP", Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200), 10, "Studio");
    }

    private static CurrentActor platformActor() {
        return new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "token");
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token");
    }
}
