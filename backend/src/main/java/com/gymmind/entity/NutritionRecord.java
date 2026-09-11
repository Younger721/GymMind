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
@Table(name = "nutrition_records")
@EntityListeners(AuditingEntityListener.class)
public class NutritionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, length = 20)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(nullable = false, length = 200)
    private String foodName;

    @Column
    private Double calories;

    @Column
    private Double protein; // g

    @Column
    private Double carbs; // g

    @Column
    private Double fats; // g

    @Column
    private Double servingSize;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
