package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challenges")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String goalType; // WORKOUT_COUNT, TOTAL_TIME, TOTAL_CALORIES, DISTANCE

    @Column(nullable = false)
    private Integer goalValue;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status; // UPCOMING, ACTIVE, COMPLETED

    @Column(nullable = false)
    private Integer participantCount;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String difficulty; // EASY, MEDIUM, HARD

    @CreationTimestamp
    private LocalDateTime createdAt;
}
