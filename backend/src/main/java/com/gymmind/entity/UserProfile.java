package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // 身体数据
    @Column
    private Double height; // cm

    @Column
    private Double weight; // kg

    @Column
    private Double targetWeight; // kg

    @Column
    private Double bodyFatPercentage;

    // 健身目标
    @Column(length = 50)
    private String fitnessGoal; // WEIGHT_LOSS, MUSCLE_GAIN, MAINTAIN, STRENGTH

    // 经验水平
    @Column(length = 20)
    private String experienceLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    // 训练频率
    @Column
    private Integer weeklyWorkoutDays;

    @Column
    private Integer dailyWorkoutMinutes;

    // 器械偏好
    @Column(length = 500)
    private String availableEquipment; // JSON array: ["barbell", "dumbbell", "machine"]

    // 饮食偏好
    @Column(length = 100)
    private String dietaryPreference; // OMNIVORE, VEGETARIAN, VEGAN

    @Column(length = 500)
    private String foodAllergies; // JSON array

    @Column(length = 500)
    private String foodDislikes; // JSON array

    // 健康状况
    @Column(columnDefinition = "TEXT")
    private String healthConditions;

    @Column(columnDefinition = "TEXT")
    private String injuries;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
