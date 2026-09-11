package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户收藏动作实体类
 */
@Data
@Entity
@Table(name = "user_favorite_exercises")
public class UserFavoriteExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 动作ID
     */
    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    /**
     * 收藏时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
