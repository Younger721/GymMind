package com.gymmind.ai.api;

import com.gymmind.ai.application.AiWorkoutPlanDraftService;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutPlanService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiWorkoutPlanControllerTest {
    @Test
    void generateAndSavePersistsValidatedDraft() {
        var draftService = mock(AiWorkoutPlanDraftService.class);
        var plans = mock(WorkoutPlanService.class);
        var actors = mock(CurrentActorProvider.class);
        var actor = mock(CurrentActor.class);
        var draft = new CreateWorkoutPlanCommand(7L, 9L, null, "plan", "goal",
                LocalDate.now(), LocalDate.now().plusDays(1), "desc", List.of());
        when(actors.requireCurrent()).thenReturn(actor);
        when(draftService.generate(actor, 9L, "request")).thenReturn(draft);
        var controller = new AiWorkoutPlanController(draftService, actors, plans);
        controller.generateAndSave(new AiWorkoutPlanController.DraftRequest(9L, "request"));
        verify(plans).saveValidatedDraft(actor, draft);
    }
}
