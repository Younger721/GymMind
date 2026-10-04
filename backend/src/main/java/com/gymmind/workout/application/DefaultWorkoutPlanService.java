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
import com.gymmind.shared.security.ActorAccess;
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
        Long tenantId = ActorAccess.tenantId(actor);
        if (command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        members.findByTenantIdAndId(tenantId, command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, command.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        for (var item : command.items()) {
            exercises.findByTenantIdAndId(tenantId, item.exerciseId())
                    .filter(com.gymmind.exercise.domain.model.Exercise::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        WorkoutPlan plan = plans.save(WorkoutPlan.create(tenantId, command.memberId(), command.coachId(),
                command.name(), command.goal(), command.startDate(), command.endDate(), command.description()));
        for (var item : command.items()) {
            var exercise = exercises.findByTenantIdAndId(tenantId, item.exerciseId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            items.save(com.gymmind.workout.domain.model.WorkoutPlanItem.create(plan, item.exerciseId(),
                    exercise.getName(), item.dayOfWeek(), item.sets(), item.reps(), item.restSeconds(), item.weight(), item.notes()));
        }
        plan.publish();
        audit.record(new AuditEvent("WORKOUT_PLAN_CREATED", "WORKOUT_PLAN", plan.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", plan.getName())));
        return view(plan);
    }

    @Override
    @Transactional
    public WorkoutPlanView createForCurrentMember(CurrentActor actor, CreateWorkoutPlanCommand command) {
        if (actor == null || !actor.roles().contains(RoleCode.MEMBER)
                || ActorAccess.tenantId(actor) == null || !actor.hasPermission("workout:write")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.tenantId(actor);
        Long memberId = members.findByTenantIdAndUserId(tenantId, actor.userId())
                .map(com.gymmind.member.domain.model.Member::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (command == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        return saveValidatedDraft(actor, new CreateWorkoutPlanCommand(command.tenantId(), memberId, null,
                command.name(), command.goal(), command.startDate(), command.endDate(), command.description(), command.items()));
    }

    @Override
    @Transactional
    public WorkoutPlanView saveValidatedDraft(CurrentActor actor, CreateWorkoutPlanCommand command) {
        requireDraftSaver(actor, command);
        validator.validate(command);
        Long tenantId = ActorAccess.tenantId(actor);
        if (command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        members.findByTenantIdAndId(tenantId, command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!canAccessPlanMember(actor, command.memberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        for (var item : command.items()) {
            exercises.findByTenantIdAndId(tenantId, item.exerciseId())
                    .filter(com.gymmind.exercise.domain.model.Exercise::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        WorkoutPlan plan = plans.save(WorkoutPlan.create(tenantId, command.memberId(), command.coachId(),
                command.name(), command.goal(), command.startDate(), command.endDate(), command.description()));
        for (var item : command.items()) {
            var exercise = exercises.findByTenantIdAndId(tenantId, item.exerciseId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            items.save(com.gymmind.workout.domain.model.WorkoutPlanItem.create(plan, item.exerciseId(),
                    exercise.getName(), item.dayOfWeek(), item.sets(), item.reps(), item.restSeconds(), item.weight(), item.notes()));
        }
        audit.record(new AuditEvent("WORKOUT_PLAN_DRAFT_SAVED", "WORKOUT_PLAN", plan.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", plan.getName())));
        return view(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutPlanView find(CurrentActor actor, Long id) {
        requireReader(actor);
        WorkoutPlan plan = plans.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!canAccessPlanMember(actor, plan.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return view(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutPlanView> list(CurrentActor actor) {
        requireReader(actor);
        return plans.findAllByTenantId(ActorAccess.tenantId(actor)).stream()
                .filter(plan -> canAccessPlanMember(actor, plan.getMemberId()))
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional
    public WorkoutPlanView publish(CurrentActor actor, Long id) {
        require(actor);
        WorkoutPlan plan = plans.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!assignments.canAccess(actor, plan.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (plan.getStatus() == WorkoutPlanStatus.PUBLISHED) {
            return view(plan);
        }
        plan.publish();
        WorkoutPlan saved = plans.save(plan);
        audit.record(new AuditEvent("WORKOUT_PLAN_PUBLISHED", "WORKOUT_PLAN", saved.getId(), AuditResult.SUCCESS,
                "workout-plan-service", Map.of("resourceName", saved.getName())));
        return view(saved);
    }

    private WorkoutPlanView view(WorkoutPlan plan) {
        List<com.gymmind.workout.domain.model.WorkoutPlanItem> planItems = items.findAllByPlanId(plan.getId());
        if (planItems == null) planItems = List.of();
        return WorkoutPlanView.from(plan, planItems.stream().map(WorkoutPlanItemView::from).toList());
    }

    private static void require(CurrentActor actor) {
        if (ActorAccess.tenantId(actor) == null
                || (!ActorAccess.canManageTenant(actor) && !actor.roles().contains(RoleCode.COACH))
                || !actor.hasPermission("workout:write")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireDraftSaver(CurrentActor actor, CreateWorkoutPlanCommand command) {
        if (actor != null && actor.roles().contains(RoleCode.MEMBER)) {
            Long tenantId = ActorAccess.tenantId(actor);
            boolean ownMember = tenantId != null && command != null && command.memberId() != null
                    && (actor.hasPermission("ai:plan") || actor.hasPermission("workout:write"))
                    && members.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(member -> member.getId().equals(command.memberId()))
                    .orElse(false);
            if (!ownMember) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        require(actor);
    }

    private void requireReader(CurrentActor actor) {
        if (actor != null && actor.roles().contains(RoleCode.MEMBER)) {
            if (ActorAccess.tenantId(actor) == null || (!actor.hasPermission("ai:plan") && !actor.hasPermission("workout:write"))) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        require(actor);
    }

    private boolean canAccessPlanMember(CurrentActor actor, Long memberId) {
        if (actor != null && actor.roles().contains(RoleCode.MEMBER)) {
            Long tenantId = ActorAccess.tenantId(actor);
            return tenantId != null && (actor.hasPermission("ai:plan") || actor.hasPermission("workout:write"))
                    && members.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(member -> member.getId().equals(memberId))
                    .orElse(false);
        }
        return assignments.canAccess(actor, memberId);
    }
}
