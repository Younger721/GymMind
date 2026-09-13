package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.model.ExerciseVideo;
import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;

public record ExerciseVideoView(Long id, Long tenantId, Long exerciseId, String title, String platform,
                                String videoUrl, String thumbnailUrl, String objectKey, String author,
                                int duration, String description, ExerciseVideoSourceType sourceType) {
    public static ExerciseVideoView from(ExerciseVideo value) {
        return new ExerciseVideoView(value.getId(), value.getTenantId(), value.getExerciseId(), value.getTitle(),
                value.getPlatform(), value.getVideoUrl(), value.getThumbnailUrl(), value.getObjectKey(),
                value.getAuthor(), value.getDuration(), value.getDescription(), value.getSourceType());
    }
}
