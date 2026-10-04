package com.gymmind.exercise.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DefaultExerciseService implements ExerciseService {
    private final ExerciseRepository exercises;
    private final AuditRecorder audit;

    public DefaultExerciseService(ExerciseRepository exercises, AuditRecorder audit) {
        this.exercises = exercises;
        this.audit = audit;
    }

    @Override
    @Transactional
    public ExerciseView create(CurrentActor actor, CreateExerciseCommand command) {
        requireCreate(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (exercises.existsByTenantIdAndName(tenantId, command.name().trim())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Exercise saved = exercises.save(Exercise.create(tenantId, command.name(), command.category(),
                command.targetMuscle(), command.difficulty(), command.equipment(), command.description(),
                command.steps(), command.commonMistakes(), command.safetyNotes(), command.tags()));
        audit.record(new AuditEvent("EXERCISE_CREATED", "EXERCISE", saved.getId(), AuditResult.SUCCESS,
                "exercise-service", Map.of("resourceName", saved.getName())));
        return ExerciseView.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseView> list(CurrentActor actor) {
        requireRead(actor);
        List<Exercise> source = ActorAccess.canManageTenant(actor)
                ? exercises.findAllByTenantId(ActorAccess.tenantId(actor))
                : exercises.findAllByTenantIdAndActiveTrue(ActorAccess.tenantId(actor));
        return source.stream().map(ExerciseView::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseView find(CurrentActor actor, Long id) {
        requireRead(actor);
        Exercise exercise = exercises.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!ActorAccess.canManageTenant(actor) && !exercise.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return ExerciseView.from(exercise);
    }

    @Override
    @Transactional
    public void update(CurrentActor actor, Long id, ExerciseUpdateCommand command) {
        require(actor, "exercise:write");
        Exercise exercise = exercises.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        exercise.update(command.name(), command.category(), command.targetMuscle(), command.difficulty(),
                command.equipment(), command.description(), command.steps(), command.commonMistakes(),
                command.safetyNotes(), command.tags());
        exercises.save(exercise);
        audit.record(new AuditEvent("EXERCISE_UPDATED", "EXERCISE", id, AuditResult.SUCCESS,
                "exercise-service", Map.of("resourceName", exercise.getName())));
    }

    @Override
    @Transactional
    public void setActive(CurrentActor actor, Long id, boolean active) {
        require(actor, "exercise:write");
        Exercise exercise = exercises.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        exercise.setActive(active);
        exercises.save(exercise);
        audit.record(new AuditEvent("EXERCISE_STATUS_CHANGED", "EXERCISE", id, AuditResult.SUCCESS,
                "exercise-service", Map.of("resourceName", String.valueOf(exercise.getName()))));
    }

    private static void require(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
    }

    private static void requireCreate(CurrentActor actor) {
        if (actor == null || ActorAccess.tenantId(actor) == null || !actor.hasPermission("exercise:write")
                || (!ActorAccess.canManageTenant(actor) && !actor.roles().contains(RoleCode.COACH))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireRead(CurrentActor actor) {
        if (actor == null || ActorAccess.tenantId(actor) == null || !actor.hasPermission("exercise:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
