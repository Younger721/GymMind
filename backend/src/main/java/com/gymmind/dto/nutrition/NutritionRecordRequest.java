package com.gymmind.dto.nutrition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NutritionRecordRequest {

    @NotNull(message = "Record date is required")
    private LocalDate recordDate;

    @NotBlank(message = "Meal type is required")
    private String mealType;

    @NotBlank(message = "Food name is required")
    private String foodName;

    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fats;
    private Double servingSize;
    private String notes;
}
