package com.gymmind.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private Long userId;

    // 身体数据
    private Double height;
    private Double weight;
    private Double targetWeight;
    private Double bodyFatPercentage;
    private String gender;
    private Integer age;

    // 健身目标
    private String fitnessGoal;

    // 经验水平
    private String experienceLevel;

    // 训练频率
    private Integer weeklyWorkoutDays;
    private Integer dailyWorkoutMinutes;

    // 器械偏好
    private List<String> availableEquipment;

    // 饮食偏好
    private String dietaryPreference;
    private List<String> foodAllergies;
    private List<String> foodDislikes;

    // 健康状况
    private String healthConditions;
    private String injuries;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
