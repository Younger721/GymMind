package com.gymmind.workout.application;

/**
 * Privacy-filtered workout history context intended for an AI prompt.
 */
public record WorkoutHistorySummary(int recordCount, String promptText) {
    public WorkoutHistorySummary {
        if (recordCount < 0) {
            throw new IllegalArgumentException("recordCount must not be negative");
        }
        promptText = promptText == null ? "" : promptText;
    }
}
