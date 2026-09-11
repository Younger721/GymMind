package com.gymmind.dto.challenge;

import lombok.Data;

@Data
public class CreateChallengeRequest {
    private String name;
    private String description;
    private String goalType; // WORKOUT_COUNT, TOTAL_TIME, TOTAL_CALORIES, DISTANCE
    private Integer goalValue;
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private String imageUrl;
    private String difficulty; // EASY, MEDIUM, HARD
}
