package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.model.ExerciseVideo;
import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;
import com.gymmind.exercise.domain.repository.ExerciseVideoRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoSearchServiceTest {
    @Test void searchesOnlyCurrentTenant() {
        var repo = mock(ExerciseVideoRepository.class);
        var video = ExerciseVideo.create(7L, 2L, "Squat tutorial", "youtube", "https://example.com/v", null, null, "coach", 60, "desc", ExerciseVideoSourceType.THIRD_PARTY);
        when(repo.searchByTenantId(7L, "squat")).thenReturn(List.of(video));
        var actor = new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("video:read"), 0, "t");
        assertThat(new VideoSearchService(repo).search(actor, "squat")).extracting(ExerciseVideoView::title).containsExactly("Squat tutorial");
        verify(repo).searchByTenantId(7L, "squat");
    }
    @Test void rejectsPlatformActor() {
        var service = new VideoSearchService(mock(ExerciseVideoRepository.class));
        assertThatThrownBy(() -> service.search(new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of("video:read"), 0, "t"), "x")).isInstanceOf(RuntimeException.class);
    }
}
