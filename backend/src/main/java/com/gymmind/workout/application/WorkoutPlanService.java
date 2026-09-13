package com.gymmind.workout.application;
import com.gymmind.shared.security.CurrentActor;
public interface WorkoutPlanService { WorkoutPlanView create(CurrentActor actor, CreateWorkoutPlanCommand command); WorkoutPlanView saveValidatedDraft(CurrentActor actor, CreateWorkoutPlanCommand command); WorkoutPlanView find(CurrentActor actor, Long id); }
