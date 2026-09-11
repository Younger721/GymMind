package com.gymmind.dto.workout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutStatistics {

    private Integer workoutDays;
    private Double totalVolume;
    private LocalDate startDate;
    private LocalDate endDate;
}
