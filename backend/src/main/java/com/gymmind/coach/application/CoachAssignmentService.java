package com.gymmind.coach.application;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.coach.domain.model.CoachAssignment;
import java.util.List;
public interface CoachAssignmentService {
    void assign(CurrentActor actor, AssignCoachCommand command);
    void unassign(CurrentActor actor, Long coachId, Long memberId);
    void request(CurrentActor actor, Long coachId);
    void accept(CurrentActor actor, Long relationId);
    void reject(CurrentActor actor, Long relationId);
    void cancel(CurrentActor actor, Long relationId);
    void end(CurrentActor actor, Long relationId);
    boolean canAccess(CurrentActor actor, Long memberId);
    List<CoachAssignment> list(CurrentActor actor, com.gymmind.coach.domain.model.CoachAssignmentStatus status);
}
