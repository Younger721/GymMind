package com.gymmind.exercise.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.exercise.domain.model.ExerciseVideo;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.exercise.domain.repository.ExerciseVideoRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DefaultExerciseVideoService implements ExerciseVideoService {
    private final ExerciseVideoRepository videos;
    private final ExerciseRepository exercises;
    private final AuditRecorder audit;

    public DefaultExerciseVideoService(ExerciseVideoRepository videos, ExerciseRepository exercises, AuditRecorder audit) {
        this.videos = videos;
        this.exercises = exercises;
        this.audit = audit;
    }

    @Override
    @Transactional
    public ExerciseVideoView create(CurrentActor actor, CreateExerciseVideoCommand command) {
        require(actor, "video:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        exercises.findByTenantIdAndId(tenantId, command.exerciseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ExerciseVideo saved;
        try {
            String objectKey = command.sourceType() == com.gymmind.exercise.domain.model.ExerciseVideoSourceType.INTERNAL
                    ? requireTenantObjectKey(tenantId, command.objectKey()) : command.objectKey();
            saved = videos.save(ExerciseVideo.create(tenantId, command.exerciseId(), command.title(),
                    command.platform(), command.videoUrl(), null, objectKey, command.author(), command.duration(),
                    command.description(), command.sourceType()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        audit.record(new AuditEvent("EXERCISE_VIDEO_CREATED", "EXERCISE_VIDEO", saved.getId(), AuditResult.SUCCESS,
                "exercise-video-service", Map.of("resourceName", saved.getTitle())));
        return ExerciseVideoView.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseVideoView find(CurrentActor actor, Long id) {
        require(actor, "video:read");
        ExerciseVideo video = videos.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return ExerciseVideoView.from(video);
    }

    @Override
    @Transactional
    public void delete(CurrentActor actor, Long id) {
        require(actor, "video:write");
        ExerciseVideo video = videos.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        videos.delete(video);
        audit.record(new AuditEvent("EXERCISE_VIDEO_DELETED", "EXERCISE_VIDEO", id, AuditResult.SUCCESS,
                "exercise-video-service", Map.of("resourceName", video.getTitle())));
    }

    private static String requireTenantObjectKey(Long tenantId, String objectKey) {
        String prefix = "tenant/" + tenantId + "/exercise-videos/";
        if (objectKey == null || !objectKey.startsWith(prefix) || objectKey.length() <= prefix.length()) {
            throw new IllegalArgumentException("Object key is outside tenant namespace");
        }
        return objectKey;
    }

    private static void require(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
    }
}
