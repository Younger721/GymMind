package com.gymmind.exercise.application;

import com.gymmind.shared.security.CurrentActor;

public interface ExerciseService {
    ExerciseView create(CurrentActor actor, CreateExerciseCommand command);
    ExerciseView find(CurrentActor actor, Long id);
    void update(CurrentActor actor, Long id, ExerciseUpdateCommand command);
}
