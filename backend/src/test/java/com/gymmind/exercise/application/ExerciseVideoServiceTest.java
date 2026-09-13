package com.gymmind.exercise.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.exercise.domain.repository.ExerciseVideoRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExerciseVideoServiceTest {
    @Test
    void createsThirdPartyVideoForTenantOwnedExercise() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        ExerciseVideoRepository videos = mock(ExerciseVideoRepository.class);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                com.gymmind.exercise.domain.model.Exercise.create(11L, "Squat", "LEGS", "QUADS",
                        "BEGINNER", "BODYWEIGHT", "desc", "steps", "mistakes", "safety", "legs")));
        when(videos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseVideoService service = new DefaultExerciseVideoService(videos, exercises, mock(AuditRecorder.class));
        ExerciseVideoView view = service.create(admin(11L), new CreateExerciseVideoCommand(null, 8L, "Squat demo",
                "Bilibili", "https://www.bilibili.com/video/BV1", null, "Coach", 120, "desc",
                ExerciseVideoSourceType.THIRD_PARTY));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.videoUrl()).startsWith("https://");
    }

    @Test
    void rejectsNonHttpThirdPartyUrl() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        ExerciseVideoRepository videos = mock(ExerciseVideoRepository.class);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                com.gymmind.exercise.domain.model.Exercise.create(11L, "Squat", "LEGS", "QUADS",
                        "BEGINNER", "BODYWEIGHT", "desc", "steps", "mistakes", "safety", "legs")));
        ExerciseVideoService service = new DefaultExerciseVideoService(videos, exercises, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateExerciseVideoCommand(null, 8L, "Demo",
                "Other", "ftp://example.com/video", null, "Coach", 120, "desc",
                ExerciseVideoSourceType.THIRD_PARTY)))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void rejectsInternalObjectKeyFromAnotherTenant() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        ExerciseVideoRepository videos = mock(ExerciseVideoRepository.class);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                com.gymmind.exercise.domain.model.Exercise.create(11L, "Squat", "LEGS", "QUADS",
                        "BEGINNER", "BODYWEIGHT", "desc", "steps", "mistakes", "safety", "legs")));
        ExerciseVideoService service = new DefaultExerciseVideoService(videos, exercises, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateExerciseVideoCommand(null, 8L, "Internal",
                "Internal", null, "tenant/12/exercise-videos/a.mp4", "Gym", 120, "desc",
                ExerciseVideoSourceType.INTERNAL)))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void crossTenantVideoFindReturnsNotFound() {
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        ExerciseVideoRepository videos = mock(ExerciseVideoRepository.class);
        when(videos.findByTenantIdAndId(12L, 99L)).thenReturn(Optional.empty());
        ExerciseVideoService service = new DefaultExerciseVideoService(videos, exercises, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.find(admin(12L), 99L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("video:read", "video:write"), 0L, "token");
    }
}
