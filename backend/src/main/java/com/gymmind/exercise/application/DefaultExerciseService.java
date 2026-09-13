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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        require(actor, "exercise:write");
        if (command == null || command.tenantId() != null && !actor.tenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (exercises.existsByTenantIdAndName(actor.tenantId(), command.name().trim())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Exercise saved = exercises.save(Exercise.create(actor.tenantId(), command.name(), command.category(),
                command.targetMuscle(), command.difficulty(), command.equipment(), command.description(),
                command.steps(), command.commonMistakes(), command.safetyNotes(), command.tags()));
        audit.record(new AuditEvent("EXERCISE_CREATED", "EXERCISE", saved.getId(), AuditResult.SUCCESS,
                "exercise-service", Map.of("resourceName", saved.getName())));
        return ExerciseView.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseView find(CurrentActor actor, Long id) {
        require(actor, "exercise:read");
        Exercise exercise = exercises.findByTenantIdAndId(actor.tenantId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return ExerciseView.from(exercise);
    }

    @Override
    @Transactional
    public void update(CurrentActor actor, Long id, ExerciseUpdateCommand command) {
        require(actor, "exercise:write");
        Exercise exercise = exercises.findByTenantIdAndId(actor.tenantId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        exercise.update(command.name(), command.category(), command.targetMuscle(), command.difficulty(),
                command.equipment(), command.description(), command.steps(), command.commonMistakes(),
                command.safetyNotes(), command.tags());
        exercises.save(exercise);
        audit.record(new AuditEvent("EXERCISE_UPDATED", "EXERCISE", id, AuditResult.SUCCESS,
                "exercise-service", Map.of("resourceName", exercise.getName())));
    }

    private static void require(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
