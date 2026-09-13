package com.gymmind.course.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.model.CourseStatus;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseServiceTest {
    @Test
    void createRejectsCoachFromAnotherTenant() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.empty());
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.create(admin(11L), new CreateCourseCommand(null, 3L, "Yoga", "GROUP", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void updateRejectsCapacityBelowReservedAndStartedChanges() {
        CourseRepository courses = mock(CourseRepository.class);
        CoachRepository coaches = mock(CoachRepository.class);
        Course course = Course.create(11L, 3L, "Yoga", "GROUP", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, "Studio");
        course.reserve(); course.reserve();
        when(courses.findByTenantIdAndId(11L, 4L)).thenReturn(Optional.of(course));
        CourseService service = new DefaultCourseService(courses, coaches, mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.update(admin(11L), 4L, "Yoga", "GROUP", course.getStartsAt(), course.getEndsAt(), 1, "Studio"))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        Course started = Course.create(11L, 3L, "Run", "GROUP", Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600), 10, "Track");
        when(courses.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(started));
        assertThatThrownBy(() -> service.update(admin(11L), 5L, "Changed", "GROUP", started.getStartsAt(), started.getEndsAt(), 10, "Track"))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }
    private static CurrentActor admin(long tenantId) { return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("course:write"), 0L, "token"); }
}
