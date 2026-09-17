package com.gymmind.booking.api;

import com.gymmind.booking.application.BookCourseCommand;
import com.gymmind.booking.application.BookingService;
import com.gymmind.booking.application.BookingView;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingControllerTest {

    @Test
    void bookDelegatesToService() {
        BookingService service = mock(BookingService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);
        when(service.book(actor, new BookCourseCommand(null, 41L, 4L)))
                .thenReturn(new BookingView(1L, 11L, 41L, 4L, BookingStatus.CONFIRMED));

        BookingController controller = new BookingController(service, actors);
        controller.book(new BookingController.BookRequest(41L, 4L));

        verify(service).book(actor, new BookCourseCommand(null, 41L, 4L));
    }

    @Test
    void cancelDelegatesToService() {
        BookingService service = mock(BookingService.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        CurrentActor actor = mock(CurrentActor.class);
        when(actors.requireCurrent()).thenReturn(actor);

        BookingController controller = new BookingController(service, actors);
        controller.cancel(99L);

        verify(service).cancel(actor, 99L);
    }
}
