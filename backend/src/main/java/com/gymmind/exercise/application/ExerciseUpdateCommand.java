package com.gymmind.exercise.application;

public record ExerciseUpdateCommand(String name, String category, String targetMuscle, String difficulty,
                                    String equipment, String description, String steps, String commonMistakes,
                                    String safetyNotes, String tags) {}
