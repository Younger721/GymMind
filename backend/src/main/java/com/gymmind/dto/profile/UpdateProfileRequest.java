package com.gymmind.dto.profile;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    // 身体数据
    @Min(value = 50, message = "Height must be at least 50 cm")
    private Double height;

    @Min(value = 20, message = "Weight must be at least 20 kg")
    private Double weight;

    private Double targetWeight;

    @Min(value = 0, message = "Body fat percentage must be positive")
    private Double bodyFatPercentage;

    // 健身目标
    private String fitnessGoal; // WEIGHT_LOSS, MUSCLE_GAIN, MAINTAIN, STRENGTH

    // 经验水平
    private String experienceLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    // 训练频率
    @Min(value = 0, message = "Weekly workout days must be positive")
    private Integer weeklyWorkoutDays;

    @Min(value = 0, message = "Daily workout minutes must be positive")
    private Integer dailyWorkoutMinutes;

    // 器械偏好
    private List<String> availableEquipment;

    // 饮食偏好
    private String dietaryPreference; // OMNIVORE, VEGETARIAN, VEGAN

    private List<String> foodAllergies;

    private List<String> foodDislikes;

    // 健康状况
    private String healthConditions;

    private String injuries;
}
