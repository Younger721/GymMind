package com.gymmind.ai.application;

import com.gymmind.workout.application.CreateWorkoutPlanCommand;

/** Editable plan draft and the privacy-filtered history context used to create it. */
public record AiWorkoutPlanDraftResponse(CreateWorkoutPlanCommand plan, History history) {
    public record History(boolean included, int recordCount, String summary) {
        public History {
            if (recordCount < 0) {
                throw new IllegalArgumentException("recordCount must not be negative");
            }
            summary = summary == null ? "" : summary;
        }
    }
}
