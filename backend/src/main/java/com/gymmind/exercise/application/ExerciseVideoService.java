package com.gymmind.exercise.application;

import com.gymmind.shared.security.CurrentActor;

public interface ExerciseVideoService {
    ExerciseVideoView create(CurrentActor actor, CreateExerciseVideoCommand command);
    ExerciseVideoView find(CurrentActor actor, Long id);
    void delete(CurrentActor actor, Long id);
}
