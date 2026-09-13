package com.gymmind.coach.application;
import com.gymmind.shared.security.CurrentActor;
public interface CoachAssignmentService {
    void assign(CurrentActor actor, AssignCoachCommand command);
    void unassign(CurrentActor actor, Long coachId, Long memberId);
    boolean canAccess(CurrentActor actor, Long memberId);
}
