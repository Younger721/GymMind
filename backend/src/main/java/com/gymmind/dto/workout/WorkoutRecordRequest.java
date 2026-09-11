package com.gymmind.dto.workout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkoutRecordRequest {

    @NotNull(message = "Workout date is required")
    private LocalDate workoutDate;

    @NotBlank(message = "Exercise name is required")
    private String exerciseName;

    private String muscleGroup;

    private Integer sets;

    private Integer reps;

    private Double weight;

    private Integer duration;

    private Integer rpe;

    private String notes;
}
