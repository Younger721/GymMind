package com.gymmind.booking.application;

import com.gymmind.shared.security.CurrentActor;

public interface BookingService {

    BookingView book(CurrentActor actor, BookCourseCommand command);

    BookingView cancel(CurrentActor actor, Long bookingId);

    BookingView confirm(CurrentActor actor, Long bookingId);

    java.util.List<BookingView> list(CurrentActor actor);
}
