package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.repository.ExerciseVideoRepository;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VideoSearchService {
    private final ExerciseVideoRepository videos;
    public VideoSearchService(ExerciseVideoRepository videos) { this.videos = videos; }
    public List<ExerciseVideoView> search(CurrentActor actor, String query) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (tenantId == null || !actor.hasPermission("video:read")) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (query == null || query.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        return videos.searchByTenantId(tenantId, query.trim()).stream().map(ExerciseVideoView::from).toList();
    }
}
