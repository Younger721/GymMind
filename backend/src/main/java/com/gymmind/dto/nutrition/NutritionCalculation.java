package com.gymmind.dto.nutrition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionCalculation {

    private Double bmi;
    private Double bmr;
    private Double tdee;
    private Double targetCalories;
    private Double targetProtein;
    private Double targetCarbs;
    private Double targetFats;
}
