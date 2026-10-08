package com.gymmind.workout.api;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.WorkoutPlanService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkoutPlanControllerTest {
    @Test
    void createDelegatesStructuredRequest() {
        WorkoutPlanService service = mock(WorkoutPlanService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);
        WorkoutPlanController controller = new WorkoutPlanController(service, actors);
        controller.create(new WorkoutPlanController.Request(41L, 77L, "Plan", "strength", LocalDate.now(),
                LocalDate.now().plusDays(7), "desc", List.of(new WorkoutPlanController.RequestItem(8L, 1, 3, 10, 60, 20, ""))));
        var captor = org.mockito.ArgumentCaptor.forClass(com.gymmind.workout.application.CreateWorkoutPlanCommand.class);
        verify(service).create(eq(actor), captor.capture());
        Assertions.assertEquals("MAIN", captor.getValue().items().get(0).phase());
        Assertions.assertEquals(0, captor.getValue().items().get(0).sortOrder());
    }
}
