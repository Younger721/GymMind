package com.gymmind.course.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DefaultCourseService implements CourseService {

    private final CourseRepository courses;
    private final CoachRepository coaches;
    private final AuditRecorder audit;

    public DefaultCourseService(CourseRepository courses, CoachRepository coaches, AuditRecorder audit) {
        this.courses = courses;
        this.coaches = coaches;
        this.audit = audit;
    }

    @Override
    @Transactional
    public CourseView create(CurrentActor actor, CreateCourseCommand command) {
        require(actor, "course:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        coaches.findByTenantIdAndId(tenantId, command.coachId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Course saved = courses.save(Course.create(tenantId, command.coachId(), command.title(),
                command.type(), command.startsAt(), command.endsAt(), command.capacity(), command.location()));
        audit.record(new AuditEvent("COURSE_CREATED", "COURSE", saved.getId(), AuditResult.SUCCESS,
                "course-service", Map.of()));
        return view(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView find(CurrentActor actor, Long id) {
        require(actor, "course:read");
        return view(findCourse(actor, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> list(CurrentActor actor) {
        require(actor, "course:read");
        return courses.findAllByTenantId(ActorAccess.tenantId(actor)).stream().map(DefaultCourseService::view).toList();
    }

    @Override
    @Transactional
    public void update(CurrentActor actor, Long id, String title, String type, Instant startsAt, Instant endsAt,
                       int capacity, String location) {
        require(actor, "course:write");
        Course course = findCourse(actor, id);
        try {
            course.update(title, type, startsAt, endsAt, capacity, location);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        courses.save(course);
        audit.record(new AuditEvent("COURSE_UPDATED", "COURSE", id, AuditResult.SUCCESS, "course-service", Map.of()));
    }

    @Override
    @Transactional
    public void cancel(CurrentActor actor, Long id) {
        require(actor, "course:write");
        Course course = findCourse(actor, id);
        course.cancel();
        courses.save(course);
        audit.record(new AuditEvent("COURSE_CANCELLED", "COURSE", id, AuditResult.SUCCESS, "course-service", Map.of()));
    }

    private Course findCourse(CurrentActor actor, Long id) {
        return courses.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static void require(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
    }

    private static CourseView view(Course course) {
        return new CourseView(course.getId(), course.getTenantId(), course.getCoachId(), course.getTitle(),
                course.getType(), course.getStartsAt(), course.getEndsAt(), course.getCapacity(),
                course.getReservedCount(), course.getLocation(), course.getStatus());
    }
}
