package com.gymmind.exercise.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.net.URI;

@Entity
@Table(name = "exercise_video")
public class ExerciseVideo extends TenantScopedEntity {
    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;
    @Column(nullable = false, length = 256)
    private String title;
    @Column(nullable = false, length = 64)
    private String platform;
    @Column(name = "video_url", length = 1024)
    private String videoUrl;
    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;
    @Column(name = "object_key", length = 512)
    private String objectKey;
    @Column(nullable = false, length = 256)
    private String author;
    @Column(nullable = false)
    private int duration;
    @Column(nullable = false, length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private ExerciseVideoSourceType sourceType;

    protected ExerciseVideo() {}

    private ExerciseVideo(Long tenantId, Long exerciseId, String title, String platform, String videoUrl,
                          String thumbnailUrl, String objectKey, String author, int duration, String description,
                          ExerciseVideoSourceType sourceType) {
        super(tenantId);
        this.exerciseId = requireId(exerciseId, "exerciseId");
        this.title = required(title, "title");
        this.platform = required(platform, "platform");
        this.author = required(author, "author");
        this.description = required(description, "description");
        if (duration < 0) throw new IllegalArgumentException("duration must not be negative");
        this.duration = duration;
        this.sourceType = sourceType == null ? ExerciseVideoSourceType.THIRD_PARTY : sourceType;
        if (this.sourceType == ExerciseVideoSourceType.THIRD_PARTY) {
            this.videoUrl = httpUrl(videoUrl, "videoUrl");
            this.thumbnailUrl = optionalHttpUrl(thumbnailUrl, "thumbnailUrl");
            this.objectKey = null;
        } else {
            if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey must not be blank");
            this.objectKey = objectKey.trim();
            this.videoUrl = null;
            this.thumbnailUrl = optionalHttpUrl(thumbnailUrl, "thumbnailUrl");
        }
    }

    public static ExerciseVideo create(Long tenantId, Long exerciseId, String title, String platform, String videoUrl,
                                       String thumbnailUrl, String objectKey, String author, int duration,
                                       String description, ExerciseVideoSourceType sourceType) {
        return new ExerciseVideo(tenantId, exerciseId, title, platform, videoUrl, thumbnailUrl, objectKey,
                author, duration, description, sourceType);
    }

    public Long getExerciseId() { return exerciseId; }
    public String getTitle() { return title; }
    public String getPlatform() { return platform; }
    public String getVideoUrl() { return videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getObjectKey() { return objectKey; }
    public String getAuthor() { return author; }
    public int getDuration() { return duration; }
    public String getDescription() { return description; }
    public ExerciseVideoSourceType getSourceType() { return sourceType; }

    private static Long requireId(Long value, String field) {
        if (value == null || value <= 0) throw new IllegalArgumentException(field + " must be positive");
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String httpUrl(String value, String field) {
        String url = required(value, field);
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new IllegalArgumentException();
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " must be an absolute HTTP(S) URL", ex);
        }
    }

    private static String optionalHttpUrl(String value, String field) {
        return value == null || value.isBlank() ? null : httpUrl(value, field);
    }
}
