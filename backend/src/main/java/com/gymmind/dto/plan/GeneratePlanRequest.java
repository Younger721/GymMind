package com.gymmind.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePlanRequest {

    private String goal; // MUSCLE_GAIN, WEIGHT_LOSS, STRENGTH, ENDURANCE, GENERAL_FITNESS

    private Integer durationWeeks; // 4, 8, 12

    private Integer workoutsPerWeek; // 3, 4, 5, 6

    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    private String equipment; // GYM, HOME, MINIMAL

    private Integer sessionDuration; // minutes: 30, 45, 60, 90

    private String[] focusAreas; // chest, back, legs, arms, core, cardio

    private String[] injuries; // knee, back, shoulder, etc.

    private String preferredTime; // MORNING, AFTERNOON, EVENING
}
