package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;

public record CreateExerciseVideoCommand(Long tenantId, Long exerciseId, String title, String platform,
                                         String videoUrl, String objectKey, String author, int duration,
                                         String description, ExerciseVideoSourceType sourceType) {}
