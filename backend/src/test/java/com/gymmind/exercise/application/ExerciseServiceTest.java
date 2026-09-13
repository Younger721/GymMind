package com.gymmind.exercise.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExerciseServiceTest {
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
}
