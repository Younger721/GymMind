package com.gymmind.workout.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.model.WorkoutPlan;
import com.gymmind.workout.domain.model.WorkoutPlanStatus;
import com.gymmind.workout.domain.repository.WorkoutPlanItemRepository;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DefaultWorkoutPlanService implements WorkoutPlanService {

    private final WorkoutPlanRepository plans;
    private final WorkoutPlanItemRepository items;
    private final ExerciseRepository exercises;
    private final MemberRepository members;
    private final CoachAssignmentService assignments;
    private final AuditRecorder audit;
    private final WorkoutPlanValidator validator = new WorkoutPlanValidator();

    public DefaultWorkoutPlanService(
            WorkoutPlanRepository plans,
            WorkoutPlanItemRepository items,
            ExerciseRepository exercises,
            MemberRepository members,
            CoachAssignmentService assignments,
            AuditRecorder audit) {
        this.plans = plans;
        this.items = items;
        this.exercises = exercises;
        this.members = members;
        this.assignments = assignments;
        this.audit = audit;
    }

    @Override
    @Transactional
    public WorkoutPlanView create(CurrentActor actor, CreateWorkoutPlanCommand command) {
        require(actor);
        validator.validate(command);
        if (command.tenantId() != null && !actor.tenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        members.findByTenantIdAndId(actor.tenantId(), command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, command.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        for (var item : command.items()) {
            exercises.findByTenantIdAndId(actor.tenantId(), item.exerciseId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        WorkoutPlan plan = plans.save(WorkoutPlan.create(actor.tenantId(), command.memberId(), command.coachId(),
                command.name(), command.goal(), command.startDate(), command.endDate(), command.description()));
        for (var item : command.items()) {
            items.save(com.gymmind.workout.domain.model.WorkoutPlanItem.create(plan, item.exerciseId(),
                    item.dayOfWeek(), item.sets(), item.reps(), item.restSeconds(), item.weight(), item.notes()));
        }
        plan.publish();
        audit.record(new AuditEvent("WORKOUT_PLAN_CREATED", "WORKOUT_PLAN", plan.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", plan.getName())));
        return WorkoutPlanView.from(plan);
    }

    @Override
    @Transactional
    public WorkoutPlanView saveValidatedDraft(CurrentActor actor, CreateWorkoutPlanCommand command) {
        require(actor);
        validator.validate(command);
        if (command.tenantId() != null && !actor.tenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        members.findByTenantIdAndId(actor.tenantId(), command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, command.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        for (var item : command.items()) {
            exercises.findByTenantIdAndId(actor.tenantId(), item.exerciseId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        WorkoutPlan plan = plans.save(WorkoutPlan.create(actor.tenantId(), command.memberId(), command.coachId(),
                command.name(), command.goal(), command.startDate(), command.endDate(), command.description()));
        for (var item : command.items()) {
            items.save(com.gymmind.workout.domain.model.WorkoutPlanItem.create(plan, item.exerciseId(),
                    item.dayOfWeek(), item.sets(), item.reps(), item.restSeconds(), item.weight(), item.notes()));
        }
        audit.record(new AuditEvent("WORKOUT_PLAN_DRAFT_SAVED", "WORKOUT_PLAN", plan.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", plan.getName())));
        return WorkoutPlanView.from(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutPlanView find(CurrentActor actor, Long id) {
        require(actor);
        WorkoutPlan plan = plans.findByTenantIdAndId(actor.tenantId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, plan.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return WorkoutPlanView.from(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutPlanView> list(CurrentActor actor) {
        require(actor);
        return plans.findAllByTenantId(actor.tenantId()).stream()
                .filter(plan -> assignments.canAccess(actor, plan.getMemberId()))
                .map(WorkoutPlanView::from)
                .toList();
    }

    @Override
    @Transactional
    public WorkoutPlanView publish(CurrentActor actor, Long id) {
        require(actor);
        WorkoutPlan plan = plans.findByTenantIdAndId(actor.tenantId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, plan.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (plan.getStatus() == WorkoutPlanStatus.PUBLISHED) {
            return WorkoutPlanView.from(plan);
        }
        plan.publish();
        WorkoutPlan saved = plans.save(plan);
        audit.record(new AuditEvent("WORKOUT_PLAN_PUBLISHED", "WORKOUT_PLAN", saved.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", saved.getName())));
        return WorkoutPlanView.from(saved);
    }

    private static void require(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null
                || (!actor.roles().contains(RoleCode.GYM_ADMIN) && !actor.roles().contains(RoleCode.COACH))
                || !actor.hasPermission("workout:write")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
