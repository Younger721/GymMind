package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 动作库实体类
 * 用于存储健身动作的视频和详细信息
 */
@Data
@Entity
@Table(name = "exercise_library")
public class ExerciseLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 动作名称（中文）
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 动作名称（英文）
     */
    @Column(name = "name_en", length = 200)
    private String nameEn;

    /**
     * 动作类别：CHEST/BACK/LEGS/SHOULDERS/ARMS/CORE/CARDIO
     */
    @Column(nullable = false, length = 100)
    private String category;

    /**
     * 难度：BEGINNER/INTERMEDIATE/ADVANCED
     */
    @Column(nullable = false, length = 50)
    private String difficulty;

    /**
     * 所需器械：BARBELL/DUMBBELL/MACHINE/BODYWEIGHT/CABLE
     */
    @Column(length = 100)
    private String equipment;

    /**
     * 视频文件URL
     */
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    /**
     * 视频缩略图URL
     */
    @Column(name = "video_thumbnail", length = 500)
    private String videoThumbnail;

    /**
     * 视频时长（秒）
     */
    @Column(name = "video_duration")
    private Integer videoDuration;

    /**
     * 视频来源：DOUYIN/TIKTOK/BILIBILI/YOUTUBE/UPLOAD
     */
    @Column(name = "video_source", length = 50)
    private String videoSource;

    /**
     * 动作描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 目标肌群
     */
    @Column(name = "target_muscles", length = 200)
    private String targetMuscles;

    /**
     * 动作要领（JSON格式：["步骤1", "步骤2", ...]）
     */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /**
     * 注意事项（JSON格式：["提示1", "提示2", ...]）
     */
    @Column(columnDefinition = "TEXT")
    private String tips;

    /**
     * 观看次数
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /**
     * 点赞次数
     */
    @Column(name = "like_count")
    private Integer likeCount = 0;

    /**
     * 状态：ACTIVE/INACTIVE
     */
    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    /**
     * 是否已验证（专业认证）
     */
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    /**
     * 创建者用户ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
