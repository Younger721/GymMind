package com.gymmind.dto.exercise;

import lombok.Data;

import java.util.List;

/**
 * 创建/更新动作库请求DTO
 */
@Data
public class ExerciseLibraryRequest {

    /**
     * 动作名称（中文）
     */
    private String name;

    /**
     * 动作名称（英文）
     */
    private String nameEn;

    /**
     * 动作类别：CHEST/BACK/LEGS/SHOULDERS/ARMS/CORE/CARDIO
     */
    private String category;

    /**
     * 难度：BEGINNER/INTERMEDIATE/ADVANCED
     */
    private String difficulty;

    /**
     * 所需器械：BARBELL/DUMBBELL/MACHINE/BODYWEIGHT/CABLE
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
     * 视频来源：DOUYIN/TIKTOK/BILIBILI/YOUTUBE/UPLOAD
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
}
