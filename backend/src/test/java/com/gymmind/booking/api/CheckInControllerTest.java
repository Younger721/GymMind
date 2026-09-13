package com.gymmind.booking.api;

import com.gymmind.booking.application.CheckInService;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class CheckInControllerTest {
    @Test
    void checkInDelegatesMemberAndCourseToService() {
        CheckInService service = mock(CheckInService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);

        CheckInController controller = new CheckInController(service, actors);
        controller.checkIn(new CheckInController.CheckInRequest(41L, 4L));

        verify(service).checkIn(actor, 41L, 4L);
    }
}
