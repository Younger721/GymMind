package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String planName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String goal; // MUSCLE_GAIN, WEIGHT_LOSS, STRENGTH, ENDURANCE

    @Column(nullable = false)
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(nullable = false)
    private Integer durationWeeks;

    @Column(nullable = false)
    private Integer workoutsPerWeek;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String planContent; // JSON format

    @Column(nullable = false)
    private String status; // ACTIVE, COMPLETED, PAUSED

    @Column(nullable = false)
    private Boolean isAiGenerated;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
