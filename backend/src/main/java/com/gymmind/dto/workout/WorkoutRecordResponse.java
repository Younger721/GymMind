package com.gymmind.dto.workout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutRecordResponse {

    private Long id;
    private LocalDate workoutDate;
    private String exerciseName;
    private String muscleGroup;
    private Integer sets;
    private Integer reps;
    private Double weight;
    private Integer duration;
    private Integer rpe;
    private String notes;
    private LocalDateTime createdAt;
}
