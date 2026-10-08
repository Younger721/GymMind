package com.gymmind.workout.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.repository.WorkoutPlanItemRepository;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkoutPlanServiceTest {
    @Test
    void createsValidatedPlanForAssignedCoach() {
        WorkoutPlanRepository plans = mock(WorkoutPlanRepository.class);
        WorkoutPlanItemRepository items = mock(WorkoutPlanItemRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        CoachAssignmentService assignments = mock(CoachAssignmentService.class);
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(mock(com.gymmind.member.domain.model.Member.class)));
        var exercise = mock(com.gymmind.exercise.domain.model.Exercise.class);
        when(exercise.isActive()).thenReturn(true);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(exercise));
        when(assignments.canAccess(any(), eq(41L))).thenReturn(true);
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        java.util.List<com.gymmind.workout.domain.model.WorkoutPlanItem> savedItems = new java.util.ArrayList<>();
        when(items.save(any())).thenAnswer(invocation -> { var saved = invocation.<com.gymmind.workout.domain.model.WorkoutPlanItem>getArgument(0); savedItems.add(saved); return saved; });
        when(items.findAllByPlanId(any())).thenAnswer(invocation -> savedItems);

        WorkoutPlanService service = new DefaultWorkoutPlanService(plans, items, exercises, members, assignments,
                mock(AuditRecorder.class));
        WorkoutPlanView view = service.create(admin(11L), new CreateWorkoutPlanCommand(null, 41L, 77L,
                "Strength", "muscle gain", LocalDate.now(), LocalDate.now().plusDays(30), "notes",
                java.util.List.of(new WorkoutPlanItemCommand(8L, 1, 4, 10, 90, 20.0, "controlled"))));

        assertThat(view.tenantId()).isEqualTo(11L);
        verify(plans).save(any());
        verify(items).save(any());
    }

    @Test
    void rejectsInvalidSetRepRestValues() {
        WorkoutPlanService service = new DefaultWorkoutPlanService(mock(WorkoutPlanRepository.class),
                mock(WorkoutPlanItemRepository.class), mock(ExerciseRepository.class), mock(MemberRepository.class),
                mock(CoachAssignmentService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateWorkoutPlanCommand(null, 41L, null,
                "Bad", "goal", LocalDate.now(), LocalDate.now().plusDays(1), "notes",
                java.util.List.of(new WorkoutPlanItemCommand(8L, 1, 0, 0, -1, 0.0, "")))))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void memberCanSaveOwnValidatedDraft() {
        WorkoutPlanRepository plans = mock(WorkoutPlanRepository.class);
        WorkoutPlanItemRepository items = mock(WorkoutPlanItemRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        CoachAssignmentService assignments = mock(CoachAssignmentService.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(member));
        var exercise = mock(com.gymmind.exercise.domain.model.Exercise.class);
        when(exercise.isActive()).thenReturn(true);
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(exercise));
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        java.util.List<com.gymmind.workout.domain.model.WorkoutPlanItem> savedItems = new java.util.ArrayList<>();
        when(items.save(any())).thenAnswer(invocation -> { var saved = invocation.<com.gymmind.workout.domain.model.WorkoutPlanItem>getArgument(0); savedItems.add(saved); return saved; });
        when(items.findAllByPlanId(any())).thenAnswer(invocation -> savedItems);

        WorkoutPlanService service = new DefaultWorkoutPlanService(plans, items, exercises, members, assignments,
                mock(AuditRecorder.class));
        WorkoutPlanView view = service.saveValidatedDraft(memberActor(11L, 9L), validCommand(41L));

        assertThat(view.memberId()).isEqualTo(41L);
        verify(assignments, never()).canAccess(any(), anyLong());
        verify(plans).save(any());
    }

    @Test
    void memberCannotSaveDraftForAnotherMember() {
        MemberRepository members = mock(MemberRepository.class);
        var ownMember = mock(com.gymmind.member.domain.model.Member.class);
        when(ownMember.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(ownMember));
        WorkoutPlanRepository plans = mock(WorkoutPlanRepository.class);
        WorkoutPlanService service = new DefaultWorkoutPlanService(plans, mock(WorkoutPlanItemRepository.class),
                mock(ExerciseRepository.class), members, mock(CoachAssignmentService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.saveValidatedDraft(memberActor(11L, 9L), validCommand(42L)))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verifyNoInteractions(plans);
    }

    @Test
    void memberPlanItemsPreservePhaseAndOrder() {
        WorkoutPlanRepository plans = mock(WorkoutPlanRepository.class);
        WorkoutPlanItemRepository items = mock(WorkoutPlanItemRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 9L)).thenReturn(Optional.of(member));
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(member));
        var exercise = mock(com.gymmind.exercise.domain.model.Exercise.class);
        when(exercise.isActive()).thenReturn(true);
        when(exercise.getName()).thenReturn("深蹲");
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(exercise));
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        java.util.List<com.gymmind.workout.domain.model.WorkoutPlanItem> savedItems = new java.util.ArrayList<>();
        when(items.save(any())).thenAnswer(invocation -> { var saved = invocation.<com.gymmind.workout.domain.model.WorkoutPlanItem>getArgument(0); savedItems.add(saved); return saved; });
        when(items.findAllByPlanId(any())).thenAnswer(invocation -> savedItems);

        WorkoutPlanService service = new DefaultWorkoutPlanService(plans, items, exercises, members,
                mock(CoachAssignmentService.class), mock(AuditRecorder.class));
        WorkoutPlanView view = service.createForCurrentMember(new CurrentActor(9L, 11L, Set.of(RoleCode.MEMBER),
                        Set.of("workout:write"), 0L, "member-token"),
                new CreateWorkoutPlanCommand(null, null, null, "全身", "力量", LocalDate.now(),
                        LocalDate.now().plusDays(30), "训练", java.util.List.of(
                        new WorkoutPlanItemCommand(8L, 1, 2, 15, 30, 0, "动态热身", "WARMUP", 1),
                        new WorkoutPlanItemCommand(8L, 1, 3, 10, 90, 20, "主训练", "MAIN", 2))));

        assertThat(view.items()).extracting(WorkoutPlanItemView::phase)
                .containsExactly("WARMUP", "MAIN");
        assertThat(view.items()).extracting(WorkoutPlanItemView::sortOrder)
                .containsExactly(1, 2);
    }

    @Test
    void oldMemberPlanCommandDefaultsToMainPhase() {
        WorkoutPlanItemCommand item = new WorkoutPlanItemCommand(8L, 1, 3, 10, 60, 0, "旧请求");

        assertThat(item.phase()).isEqualTo("MAIN");
        assertThat(item.sortOrder()).isZero();
    }

    @Test
    void rejectsUnsupportedPhase() {
        WorkoutPlanService service = new DefaultWorkoutPlanService(mock(WorkoutPlanRepository.class),
                mock(WorkoutPlanItemRepository.class), mock(ExerciseRepository.class), mock(MemberRepository.class),
                mock(CoachAssignmentService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateWorkoutPlanCommand(null, 41L, null,
                "Bad phase", "goal", LocalDate.now(), LocalDate.now().plusDays(1), "notes",
                java.util.List.of(new WorkoutPlanItemCommand(8L, 1, 3, 10, 60, 0, "", "COOLDOWN", 1)))))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    private static CreateWorkoutPlanCommand validCommand(long memberId) {
        return new CreateWorkoutPlanCommand(null, memberId, null, "Strength", "muscle gain", LocalDate.now(),
                LocalDate.now().plusDays(30), "notes",
                java.util.List.of(new WorkoutPlanItemCommand(8L, 1, 4, 10, 90, 20.0, "controlled")));
    }

    private static CurrentActor memberActor(long tenantId, long userId) {
        return new CurrentActor(userId, tenantId, Set.of(RoleCode.MEMBER), Set.of("ai:plan"), 0L, "member-token");
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("workout:write"), 0L, "token");
    }
}
