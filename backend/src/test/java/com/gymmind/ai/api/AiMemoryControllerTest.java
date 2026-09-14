package com.gymmind.ai.api;

import com.gymmind.ai.application.AiMemoryService;
import com.gymmind.ai.application.AiMemoryType;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AiMemoryControllerTest {
    @Test
    void writeAndDeleteDelegateTheCurrentActor() {
        AiMemoryService service = mock(AiMemoryService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);
        AiMemoryController controller = new AiMemoryController(service, actors);

        controller.write(new AiMemoryController.WriteRequest(AiMemoryType.GOAL, "weight", "lose weight"));
        controller.delete(AiMemoryType.GOAL, "weight");

        verify(service).write(actor, AiMemoryType.GOAL, "weight", "lose weight");
        verify(service).delete(actor, AiMemoryType.GOAL, "weight");
    }
}
