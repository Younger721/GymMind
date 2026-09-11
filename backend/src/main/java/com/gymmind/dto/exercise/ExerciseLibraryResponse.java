package com.gymmind.dto.exercise;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动作库响应DTO
 */
@Data
public class ExerciseLibraryResponse {

    /**
     * 动作ID
     */
    private Long id;

    /**
     * 动作名称（中文）
     */
    private String name;

    /**
     * 动作名称（英文）
     */
    private String nameEn;

    /**
     * 动作类别
     */
    private String category;

    /**
     * 难度
     */
    private String difficulty;

    /**
     * 所需器械
     */
    private String equipment;

    /**
     * 视频URL
     */
    private String videoUrl;

    /**
     * 视频缩略图URL
     */
    private String videoThumbnail;

    /**
     * 视频时长（秒）
     */
    private Integer videoDuration;

    /**
     * 视频来源
     */
    private String videoSource;

    /**
     * 动作描述
     */
    private String description;

    /**
     * 目标肌群
     */
    private String targetMuscles;

    /**
     * 动作要领
     */
    private List<String> instructions;

    /**
     * 注意事项
     */
    private List<String> tips;

    /**
     * 观看次数
     */
    private Integer viewCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 是否已验证
     */
    private Boolean isVerified;

    /**
     * 是否已收藏（当前用户）
     */
    private Boolean isFavorited;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
