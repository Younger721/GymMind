package com.gymmind.exercise.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface ExerciseService {
    ExerciseView create(CurrentActor actor, CreateExerciseCommand command);
    ExerciseView find(CurrentActor actor, Long id);
    List<ExerciseView> list(CurrentActor actor);
    void update(CurrentActor actor, Long id, ExerciseUpdateCommand command);
}
