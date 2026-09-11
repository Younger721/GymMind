package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout_records")
@EntityListeners(AuditingEntityListener.class)
public class WorkoutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate workoutDate;

    @Column(nullable = false, length = 100)
    private String exerciseName;

    @Column(length = 50)
    private String muscleGroup;

    @Column
    private Integer sets;

    @Column
    private Integer reps;

    @Column
    private Double weight; // kg

    @Column
    private Integer duration; // seconds

    @Column
    private Integer rpe; // Rate of Perceived Exertion (1-10)

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
