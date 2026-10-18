package com.gymmind.exercise.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExerciseServiceTest {
    @Test
    void coachCanCreateExerciseButCannotUpdateIt() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(exercises.existsByTenantIdAndName(11L, "Lat pulldown")).thenReturn(false);
        when(exercises.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));

        ExerciseView view = service.create(coach(11L), new CreateExerciseCommand(null, "Lat pulldown",
                "BACK", "LATS", "BEGINNER", "CABLE", "desc", "steps", "mistakes", "safety", "back"));

        assertThat(view.name()).isEqualTo("Lat pulldown");
        assertThatThrownBy(() -> service.update(coach(11L), 8L, new ExerciseUpdateCommand(
                "Changed", "BACK", "LATS", "BEGINNER", "CABLE", "desc", "steps", "mistakes", "safety", "back")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void memberOnlySeesActiveExercises() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(exercises.findAllByTenantIdAndActiveTrue(11L)).thenReturn(java.util.List.of());
        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));

        service.list(member(11L));

        verify(exercises).findAllByTenantIdAndActiveTrue(11L);
        verify(exercises, never()).findAllByTenantId(11L);
    }

    @Test
    void gymAdminCanChangeExerciseStatus() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        var exercise = mock(com.gymmind.exercise.domain.model.Exercise.class);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(exercise));
        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));

        service.setActive(admin(11L), 8L, false);

        verify(exercise).setActive(false);
        verify(exercises).save(exercise);
    }
    @Test
    void createsExerciseInsideActorTenant() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(exercises.existsByTenantIdAndName(11L, "Barbell squat")).thenReturn(false);
        when(exercises.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));
        ExerciseView view = service.create(admin(11L), new CreateExerciseCommand(null, "Barbell squat",
                "LEGS", "QUADRICEPS", "INTERMEDIATE", "BARBELL", "Squat down", "Brace core",
                "Knees track toes", "Stop if pain", "legs,squat"));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.name()).isEqualTo("Barbell squat");
        verify(exercises).save(any());
    }

    @Test
    void platformAdminCannotCreateExerciseInsideSelectedTenant() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        TenantContextHolder.set(11L);
        try {
            assertThatThrownBy(() -> new DefaultExerciseService(exercises, mock(AuditRecorder.class)).create(platform(),
                    new CreateExerciseCommand(null, "Barbell squat", "LEGS", "QUADRICEPS", "INTERMEDIATE",
                            "BARBELL", "Squat down", "Brace core", "Knees track toes", "Stop if pain", "legs,squat")))
                    .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
            verify(exercises, never()).save(any());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void rejectsDuplicateNameInsideSameTenant() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(exercises.existsByTenantIdAndName(11L, "Barbell squat")).thenReturn(true);

        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateExerciseCommand(null, "Barbell squat",
                "LEGS", "QUADRICEPS", "INTERMEDIATE", "BARBELL", "Squat down", "Brace core",
                "Knees track toes", "Stop if pain", "legs,squat")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void crossTenantFindIsNotAllowed() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(exercises.findByTenantIdAndId(12L, 8L)).thenReturn(Optional.empty());

        ExerciseService service = new DefaultExerciseService(exercises, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.find(admin(12L), 8L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verify(exercises).findByTenantIdAndId(12L, 8L);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("exercise:read", "exercise:write"), 0L, "token");
    }

    private static CurrentActor platform() {
        return new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "token");
    }

    private static CurrentActor coach(long tenantId) {
        return new CurrentActor(2L, tenantId, Set.of(RoleCode.COACH),
                Set.of("exercise:read", "exercise:write"), 0L, "coach-token");
    }

    private static CurrentActor member(long tenantId) {
        return new CurrentActor(3L, tenantId, Set.of(RoleCode.MEMBER),
                Set.of("exercise:read"), 0L, "member-token");
    }
}
