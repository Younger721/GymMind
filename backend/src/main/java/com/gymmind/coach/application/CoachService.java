package com.gymmind.coach.application;
import com.gymmind.shared.security.CurrentActor;
public interface CoachService {
    CoachView create(CurrentActor actor, CreateCoachCommand command);
    CoachView find(CurrentActor actor, Long coachId);
    void update(CurrentActor actor, Long coachId, String fullName, String phone);
    void suspend(CurrentActor actor, Long coachId);
}
