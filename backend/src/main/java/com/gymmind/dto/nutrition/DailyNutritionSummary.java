package com.gymmind.dto.nutrition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNutritionSummary {

    private LocalDate date;
    private Double totalCalories;
    private Double totalProtein;
    private Double totalCarbs;
    private Double totalFats;
    private Double targetCalories;
    private Double targetProtein;
    private Double targetCarbs;
    private Double targetFats;
}
