package com.gymmind.course.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.member.domain.repository.MemberRepository;
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
    private final BookingRepository bookings;
    private final MemberRepository members;

    public DefaultCourseService(CourseRepository courses, CoachRepository coaches, AuditRecorder audit) {
        this(courses, coaches, audit, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultCourseService(CourseRepository courses, CoachRepository coaches, AuditRecorder audit, BookingRepository bookings, MemberRepository members) {
        this.courses = courses;
        this.coaches = coaches;
        this.audit = audit;
        this.bookings = bookings;
        this.members = members;
    }

    @Override
    @Transactional
    public CourseView create(CurrentActor actor, CreateCourseCommand command) {
        requireAuthenticated(actor);
        Long tenantId = ActorAccess.requireTenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long coachId = resolveCoachForCreate(actor, tenantId, command.coachId());
        Course saved = courses.save(Course.create(tenantId, coachId, command.title(),
                command.type(), command.startsAt(), command.endsAt(), command.capacity(), command.location()));
        audit.record(new AuditEvent("COURSE_CREATED", "COURSE", saved.getId(), AuditResult.SUCCESS,
                "course-service", Map.of()));
        return view(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView find(CurrentActor actor, Long id) {
        requireAuthenticated(actor);
        return view(findCourseForRead(actor, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> list(CurrentActor actor) {
        return list(actor, null, null, null, null, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> list(CurrentActor actor, Instant from, Instant to, String type, Long coachId,
                                 boolean availableOnly) {
        return list(actor, "all", from, to, type, coachId, availableOnly);
    }

    @Override
    public List<CourseView> list(CurrentActor actor, String scope, Instant from, Instant to, String type, Long coachId,
                                 boolean availableOnly) {
        requireListActor(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        Long visibleCoachId = coachId;
        Instant now = Instant.now();
        final Long filterCoachId = visibleCoachId;
        List<Course> source = ActorAccess.isSuperAdmin(actor)
                ? courses.findAll()
                : courses.findAllByTenantId(tenantId);
        if ("mine".equalsIgnoreCase(scope) && bookings != null && tenantId != null) {
            var ids = bookings.findAllByTenantId(tenantId).stream()
                    .filter(b -> !b.getStatus().equals(BookingStatus.CANCELLED))
                    .filter(b -> actor.roles().contains(RoleCode.MEMBER) && b.getMemberId().equals(resolveMemberId(actor, tenantId)))
                    .map(b -> b.getCourseId()).collect(java.util.stream.Collectors.toSet());
            source = source.stream().filter(c -> ids.contains(c.getId())).toList();
        }
        if ("managed".equalsIgnoreCase(scope) && actor.roles().contains(RoleCode.COACH) && !ActorAccess.canManageTenant(actor)) {
            Long coachIdForActor = coaches.findByTenantIdAndUserId(tenantId, actor.userId()).map(c -> c.getId()).orElse(-1L);
            source = source.stream().filter(c -> coachIdForActor.equals(c.getCoachId())).toList();
        }
        return source.stream()
                .filter(course -> filterCoachId == null || filterCoachId.equals(course.getCoachId()))
                .filter(course -> from == null || !course.getEndsAt().isBefore(from))
                .filter(course -> to == null || !course.getStartsAt().isAfter(to))
                .filter(course -> type == null || type.isBlank() || type.equalsIgnoreCase(course.getType()))
                .filter(course -> !availableOnly || course.getStatus() == com.gymmind.course.domain.model.CourseStatus.SCHEDULED
                        && course.getReservedCount() < course.getCapacity() && course.getEndsAt().isAfter(now))
                .map(DefaultCourseService::view)
                .toList();
    }

    private Long resolveMemberId(CurrentActor actor, Long tenantId) {
        return members == null ? -1L : members.findByTenantIdAndUserId(tenantId, actor.userId()).map(m -> m.getId()).orElse(-1L);
    }

    @Override
    @Transactional
    public void update(CurrentActor actor, Long id, String title, String type, Instant startsAt, Instant endsAt,
                       int capacity, String location) {
        requireAuthenticated(actor);
        Course course = findCourse(actor, id);
        ensureCanManage(actor, course);
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
        requireAuthenticated(actor);
        Course course = findCourse(actor, id);
        ensureCanManage(actor, course);
        course.cancel();
        courses.save(course);
        audit.record(new AuditEvent("COURSE_CANCELLED", "COURSE", id, AuditResult.SUCCESS, "course-service", Map.of()));
    }

    private Course findCourse(CurrentActor actor, Long id) {
        Course course = courses.findByTenantIdAndId(ActorAccess.requireTenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (actor.roles().contains(RoleCode.COACH) && !ActorAccess.canManageTenant(actor)) {
            ensureCanManage(actor, course);
        }
        return course;
    }

    private Course findCourseForRead(CurrentActor actor, Long id) {
        if (ActorAccess.isSuperAdmin(actor)) {
            return courses.findAll().stream()
                    .filter(course -> id.equals(course.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        return findCourse(actor, id);
    }

    private Long resolveCoachForCreate(CurrentActor actor, Long tenantId, Long requestedCoachId) {
        if (actor.roles().contains(RoleCode.COACH) && !ActorAccess.canManageTenant(actor)) {
            return coaches.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(coach -> coach.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        if (!ActorAccess.canManageTenant(actor) || requestedCoachId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        coaches.findByTenantIdAndId(tenantId, requestedCoachId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return requestedCoachId;
    }

    private void ensureCanManage(CurrentActor actor, Course course) {
        if (ActorAccess.canManageTenant(actor)) return;
        if (!actor.roles().contains(RoleCode.COACH)
                || coaches.findByTenantIdAndUserId(course.getTenantId(), actor.userId())
                .map(coach -> !coach.getId().equals(course.getCoachId()))
                .orElse(true)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireAuthenticated(CurrentActor actor) {
        if (actor == null
                || actor.roles().stream().noneMatch(role -> role == RoleCode.MEMBER || role == RoleCode.COACH
                || role == RoleCode.GYM_ADMIN || role == RoleCode.PLATFORM_ADMIN)
                || (!ActorAccess.isSuperAdmin(actor) && ActorAccess.tenantId(actor) == null)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireListActor(CurrentActor actor) {
        if (actor == null || actor.roles().stream().noneMatch(role -> role == RoleCode.MEMBER || role == RoleCode.COACH
                || role == RoleCode.GYM_ADMIN || role == RoleCode.PLATFORM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!ActorAccess.isSuperAdmin(actor) && ActorAccess.tenantId(actor) == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static CourseView view(Course course) {
        return new CourseView(course.getId(), course.getTenantId(), course.getCoachId(), course.getTitle(),
                course.getType(), course.getStartsAt(), course.getEndsAt(), course.getCapacity(),
                course.getReservedCount(), course.getLocation(), course.getStatus());
    }
}
