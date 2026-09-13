package com.gymmind.exercise.api;

import com.gymmind.exercise.application.ExerciseService;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ExerciseControllerTest {
    @Test
    void createDelegatesToTenantScopedService() {
        ExerciseService service = mock(ExerciseService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);

        ExerciseController controller = new ExerciseController(service, actors);
        ExerciseController.ExerciseRequest request = new ExerciseController.ExerciseRequest(
                "Squat", "LEGS", "QUADRICEPS", "BEGINNER", "BODYWEIGHT", "desc", "steps",
                "mistakes", "safety", "legs");

        controller.create(request);

        verify(service).create(eq(actor), any());
    }
}
