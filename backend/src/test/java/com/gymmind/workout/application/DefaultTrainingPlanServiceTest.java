package com.gymmind.workout.application;

import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.model.WorkoutPlanTemplateItem;
import com.gymmind.workout.domain.model.MemberWorkoutPlan;
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
    void savesFreeTextTemplateItemsInTheirPhasesAndOrder() {
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

        service(templates, items, exercises).create(coach(), new CreateTemplateCommand("力量计划", "背", "力量", "说明", List.of(
                new CreateTemplateCommand.Item("肩部环绕", "WARMUP", 1, 2, 15, 30, 0, "热身"),
                new CreateTemplateCommand.Item("卧推", "MAIN", 2, 4, 8, 90, 30, "正式组"),
                new CreateTemplateCommand.Item("胸部拉伸", "STRETCH", 3, 1, 30, 0, 0, "拉伸"))));

        assertThat(saved).extracting(WorkoutPlanTemplateItem::getPhase).containsExactly("WARMUP", "MAIN", "STRETCH");
        assertThat(saved).extracting(WorkoutPlanTemplateItem::getSortOrder).containsExactly(1, 2, 3);
    }

    @Test
    void onlyCoachCanCreateOrPublishTemplates() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var service = service(templates, mock(SpringDataWorkoutPlanTemplateItemRepository.class), mock(ExerciseRepository.class));
        assertThatThrownBy(() -> service.create(admin(), new CreateTemplateCommand("计划", "力量", "目标", "说明",
                List.of(new CreateTemplateCommand.Item("卧推", "MAIN", 1, 3, 10, 60, 0, "")))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(templates);
        assertThatThrownBy(() -> service.publish(admin(), 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void publishedTemplateCannotBePublishedAgain() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var template = new com.gymmind.workout.domain.model.WorkoutPlanTemplate(11L, 9L, "计划", "力量", "目标", "说明");
        template.publish();
        when(templates.findByTenantIdAndId(11L, 2L)).thenReturn(Optional.of(template));
        var service = service(templates, mock(SpringDataWorkoutPlanTemplateItemRepository.class), mock(ExerciseRepository.class));
        assertThatThrownBy(() -> service.publish(coach(), 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void selectingTemplateCopiesTextItemsToEditableMemberPlanWithoutScheduling() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var templateItems = mock(SpringDataWorkoutPlanTemplateItemRepository.class);
        var memberPlans = mock(SpringDataMemberWorkoutPlanRepository.class);
        var planRepo = mock(com.gymmind.workout.domain.repository.WorkoutPlanRepository.class);
        var planItems = mock(com.gymmind.workout.domain.repository.WorkoutPlanItemRepository.class);
        var schedules = mock(SpringDataMemberWorkoutScheduleRepository.class);
        var members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(21L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        var template = new com.gymmind.workout.domain.model.WorkoutPlanTemplate(11L, 31L, "力量", "健身", "增肌", "说明");
        template.publish();
        when(templates.findByTenantIdAndId(11L, 7L)).thenReturn(Optional.of(template));
        when(templateItems.findAllByTemplateIdOrderBySortOrder(any())).thenReturn(List.of(
                new WorkoutPlanTemplateItem(template, "卧推", "MAIN", 1, 4, 10, 60, 20, "暂停")));
        when(planRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(planItems.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new DefaultTrainingPlanService(templates, templateItems, memberPlans, schedules, members,
                mock(ExerciseRepository.class), planRepo, planItems);

        var created = service.select(memberActor(), 7L);

        assertThat(created).isInstanceOf(com.gymmind.workout.domain.model.WorkoutPlan.class);
        verify(planRepo).save(any());
        verify(planItems).save(argThat(i -> i.getExerciseNameSnapshot().equals("卧推") && i.getPhase().equals("MAIN")));
        verifyNoInteractions(schedules);
    }

    @Test
    void rejectsInvalidTemplatePhaseAndValues() {
        var templates = mock(SpringDataWorkoutPlanTemplateRepository.class);
        var service = service(templates, mock(SpringDataWorkoutPlanTemplateItemRepository.class), mock(ExerciseRepository.class));

        assertThatThrownBy(() -> service.create(coach(), new CreateTemplateCommand("力量计划", "背", "力量", "说明", List.of(
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

    @Test
    void schedulesMemberWorkoutPlanByWorkoutPlanId() {
        var memberPlans = mock(SpringDataMemberWorkoutPlanRepository.class);
        var planRepo = mock(com.gymmind.workout.domain.repository.WorkoutPlanRepository.class);
        var schedules = mock(SpringDataMemberWorkoutScheduleRepository.class);
        var members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(21L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        var plan = com.gymmind.workout.domain.model.WorkoutPlan.create(
                11L, 21L, null, "排月入门", "增肌力量",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusMonths(1), "说明");
        when(planRepo.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(plan));
        when(schedules.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new DefaultTrainingPlanService(mock(SpringDataWorkoutPlanTemplateRepository.class),
                mock(SpringDataWorkoutPlanTemplateItemRepository.class), memberPlans, schedules, members,
                mock(ExerciseRepository.class), planRepo, mock(com.gymmind.workout.domain.repository.WorkoutPlanItemRepository.class));

        var scheduled = (com.gymmind.workout.domain.model.MemberWorkoutSchedule) service.schedule(
                memberActor(), 5L, java.time.LocalDate.of(2026, 10, 14));

        assertThat(scheduled.getWorkoutPlanId()).isEqualTo(5L);
        assertThat(scheduled.getMemberPlanId()).isNull();
        verifyNoInteractions(memberPlans);
    }

    @Test
    void calendarReturnsWorkoutPlanNameForSchedule() {
        var schedules = mock(SpringDataMemberWorkoutScheduleRepository.class);
        var planRepo = mock(com.gymmind.workout.domain.repository.WorkoutPlanRepository.class);
        var members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(21L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        var schedule = com.gymmind.workout.domain.model.MemberWorkoutSchedule.forWorkoutPlan(
                11L, 5L, 21L, java.time.LocalDate.of(2026, 10, 8));
        when(schedules.findAllByTenantIdAndMemberIdAndScheduledDateBetween(
                11L, 21L, java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(schedule));
        var plan = com.gymmind.workout.domain.model.WorkoutPlan.create(
                11L, 21L, null, "胸部基础力量", "增肌",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusMonths(1), "说明");
        when(planRepo.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(plan));
        var service = new DefaultTrainingPlanService(mock(SpringDataWorkoutPlanTemplateRepository.class),
                mock(SpringDataWorkoutPlanTemplateItemRepository.class), mock(SpringDataMemberWorkoutPlanRepository.class),
                schedules, members, mock(ExerciseRepository.class), planRepo,
                mock(com.gymmind.workout.domain.repository.WorkoutPlanItemRepository.class));

        var views = service.calendar(memberActor(), java.time.LocalDate.of(2026, 10, 1),
                java.time.LocalDate.of(2026, 10, 31));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).planName()).isEqualTo("胸部基础力量");
        assertThat(views.get(0).goal()).isEqualTo("增肌");
        assertThat(views.get(0).workoutPlanId()).isEqualTo(5L);
    }

    @Test
    void legacyScheduleUsesMappedPersonalPlan() {
        var memberPlans = mock(SpringDataMemberWorkoutPlanRepository.class);
        var plans = mock(com.gymmind.workout.domain.repository.WorkoutPlanRepository.class);
        var schedules = mock(SpringDataMemberWorkoutScheduleRepository.class);
        var members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(21L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        var legacy = new MemberWorkoutPlan(11L, 21L, 3L, "旧计划", 1);
        try {
            var field = MemberWorkoutPlan.class.getDeclaredField("workoutPlanId");
            field.setAccessible(true);
            field.set(legacy, 42L);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        when(memberPlans.findByTenantIdAndId(11L, 7L)).thenReturn(Optional.of(legacy));
        when(schedules.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new DefaultTrainingPlanService(mock(SpringDataWorkoutPlanTemplateRepository.class),
                mock(SpringDataWorkoutPlanTemplateItemRepository.class), memberPlans, schedules, members,
                mock(ExerciseRepository.class), plans, mock(com.gymmind.workout.domain.repository.WorkoutPlanItemRepository.class));

        var scheduled = (com.gymmind.workout.domain.model.MemberWorkoutSchedule) service.schedule(memberActor(), 7L, java.time.LocalDate.now());
        assertThat(scheduled.getWorkoutPlanId()).isEqualTo(42L);
        assertThat(scheduled.getMemberPlanId()).isNull();
    }

    private static DefaultTrainingPlanService service(SpringDataWorkoutPlanTemplateRepository templates,
            SpringDataWorkoutPlanTemplateItemRepository items, ExerciseRepository exercises) {
        return new DefaultTrainingPlanService(templates, items, mock(SpringDataMemberWorkoutPlanRepository.class),
                mock(SpringDataMemberWorkoutScheduleRepository.class), mock(MemberRepository.class), exercises,
                mock(com.gymmind.workout.domain.repository.WorkoutPlanRepository.class),
                mock(com.gymmind.workout.domain.repository.WorkoutPlanItemRepository.class));
    }

    private static CurrentActor admin() {
        return new CurrentActor(9L, 11L, Set.of(RoleCode.GYM_ADMIN), Set.of("workout:write"), 0L, "admin-token");
    }

    private static CurrentActor coach() {
        return new CurrentActor(9L, 11L, Set.of(RoleCode.COACH), Set.of("workout:write"), 0L, "coach-token");
    }

    private static CurrentActor memberActor() {
        return new CurrentActor(9L, 11L, Set.of(RoleCode.MEMBER), Set.of("workout:read", "workout:write"), 0L, "member-token");
    }
}
