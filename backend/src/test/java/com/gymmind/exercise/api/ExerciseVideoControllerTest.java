package com.gymmind.exercise.api;

import com.gymmind.exercise.application.ExerciseVideoService;
import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExerciseVideoControllerTest {
    @Test
    void createDelegatesToService() {
        ExerciseVideoService service = mock(ExerciseVideoService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);
        ExerciseVideoController controller = new ExerciseVideoController(service, actors);

        controller.create(new ExerciseVideoController.VideoRequest(8L, "Demo", "Bilibili",
                "https://bilibili.com/video/BV1", null, "Coach", 120, "desc",
                ExerciseVideoSourceType.THIRD_PARTY));

        verify(service).create(eq(actor), any());
    }
}
