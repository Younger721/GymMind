package com.gymmind.workout.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface WorkoutPlanService {
    WorkoutPlanView create(CurrentActor actor, CreateWorkoutPlanCommand command);
    WorkoutPlanView saveValidatedDraft(CurrentActor actor, CreateWorkoutPlanCommand command);
    WorkoutPlanView find(CurrentActor actor, Long id);
    List<WorkoutPlanView> list(CurrentActor actor);
    WorkoutPlanView publish(CurrentActor actor, Long id);
}
