package com.gymmind.booking.application; import com.gymmind.shared.security.CurrentActor; public interface CheckInService{void checkIn(CurrentActor actor,Long memberId,Long courseId);}
