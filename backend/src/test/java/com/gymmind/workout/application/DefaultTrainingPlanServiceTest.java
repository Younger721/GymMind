package com.gymmind.workout.application;

import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.model.WorkoutPlanTemplateItem;
import com.gymmind.workout.infrastructure.persistence.SpringDataMemberWorkoutPlanRepository;
import com.gymmind.workout.infrastructure.persistence.SpringDataMemberWorkoutScheduleRepository;
import com.gymmind.workout.infrastructure.persistence.SpringDataWorkoutPlanTemplateItemRepository;
import com.gymmind.workout.infrastructure.persistence.SpringDataWorkoutPlanTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultTrainingPlanServiceTest {
    @Test
    void savesTemplateItemsInTheirPhasesAndOrder() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var items = mock(SpringDataWorkoutPlanTemplateItemRepository.class);
        var exercises = mock(ExerciseRepository.class);
        var exercise = mock(com.gymmind.exercise.domain.model.Exercise.class);
        when(exercise.isActive()).thenReturn(true);
        when(exercise.getName()).thenReturn("深蹲");
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(exercise));
        when(templates.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<WorkoutPlanTemplateItem> saved = new ArrayList<>();
        when(items.save(any())).thenAnswer(invocation -> {
            var item = invocation.<WorkoutPlanTemplateItem>getArgument(0);
            saved.add(item);
            return item;
        });

        service(templates, items, exercises).create(admin(), new CreateTemplateCommand("力量计划", "背", "力量", "说明", List.of(
                new CreateTemplateCommand.Item(8L, null, "WARMUP", 1, 2, 15, 30, 0, "热身"),
                new CreateTemplateCommand.Item(8L, null, "MAIN", 2, 4, 8, 90, 30, "正式组"),
                new CreateTemplateCommand.Item(8L, null, "STRETCH", 3, 1, 30, 0, 0, "拉伸"))));

        assertThat(saved).extracting(WorkoutPlanTemplateItem::getPhase).containsExactly("WARMUP", "MAIN", "STRETCH");
        assertThat(saved).extracting(WorkoutPlanTemplateItem::getSortOrder).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsInvalidTemplatePhaseAndValues() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var service = service(templates, mock(SpringDataWorkoutPlanTemplateItemRepository.class), mock(ExerciseRepository.class));

        assertThatThrownBy(() -> service.create(admin(), new CreateTemplateCommand("力量计划", "背", "力量", "说明", List.of(
                new CreateTemplateCommand.Item(8L, null, "COOLDOWN", 1, 0, 10, -1, 0, "错误")))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(templates);
    }

    @Test
    void oldTemplateItemRequestDefaultsToMainPhaseAndGeneratedOrder() {
        var item = new CreateTemplateCommand.Item(8L, null, null, 0, 3, 10, 60, 0, "旧客户端动作");

        assertThat(item.phase()).isEqualTo("MAIN");
        assertThat(item.sortOrder()).isZero();
    }

    private static DefaultTrainingPlanService service(SpringDataWorkoutPlanTemplateRepository templates,
            SpringDataWorkoutPlanTemplateItemRepository items, ExerciseRepository exercises) {
        return new DefaultTrainingPlanService(templates, items, mock(SpringDataMemberWorkoutPlanRepository.class),
                mock(SpringDataMemberWorkoutScheduleRepository.class), mock(MemberRepository.class), exercises);
    }

    private static CurrentActor admin() {
        return new CurrentActor(9L, 11L, Set.of(RoleCode.GYM_ADMIN), Set.of("workout:write"), 0L, "admin-token");
    }
}
