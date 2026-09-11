package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout_plan_days")
public class WorkoutPlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, length = 100)
    private String dayName; // e.g., "Upper Body", "Rest Day"

    @Column(columnDefinition = "TEXT")
    private String exercises; // JSON format

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Boolean isRestDay;

    @Column(nullable = false)
    private Boolean isCompleted;
}
