package com.gymmind.booking.application; import com.gymmind.shared.security.CurrentActor; public interface BookingService{void book(CurrentActor a,BookCourseCommand c);}
