package com.gymmind.workout.api;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.WorkoutRecordService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkoutRecordControllerTest {
    @Test
    void createDelegatesRecordRequest() {
        WorkoutRecordService service = mock(WorkoutRecordService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);
        WorkoutRecordController controller = new WorkoutRecordController(service, actors);
        controller.create(new WorkoutRecordController.Request(null, LocalDate.now(), 30, 200, "done",
                List.of(new WorkoutRecordController.SetRequest(8L, 1, 10, 10, 0, true))));
        verify(service).create(eq(actor), any());
    }
}
