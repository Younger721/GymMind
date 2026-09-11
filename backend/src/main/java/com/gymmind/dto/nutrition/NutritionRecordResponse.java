package com.gymmind.dto.nutrition;

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
public class NutritionRecordResponse {

    private Long id;
    private LocalDate recordDate;
    private String mealType;
    private String foodName;
    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fats;
    private Double servingSize;
    private String notes;
    private LocalDateTime createdAt;
}
