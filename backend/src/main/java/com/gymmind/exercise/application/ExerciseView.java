package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.model.Exercise;

public record ExerciseView(Long id, Long tenantId, String name, String category, String targetMuscle,
                           String difficulty, String equipment, String description, String steps,
                           String commonMistakes, String safetyNotes, String tags) {
    public static ExerciseView from(Exercise value) {
        return new ExerciseView(value.getId(), value.getTenantId(), value.getName(), value.getCategory(),
                value.getTargetMuscle(), value.getDifficulty(), value.getEquipment(), value.getDescription(),
                value.getSteps(), value.getCommonMistakes(), value.getSafetyNotes(), value.getTags());
    }
}
