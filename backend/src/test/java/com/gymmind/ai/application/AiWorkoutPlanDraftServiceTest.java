package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutHistoryQueryService;
import com.gymmind.workout.application.WorkoutHistorySummary;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AiWorkoutPlanDraftServiceTest {
    @Test
    void parsesAndValidatesModelPlanBeforeReturningDraft() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn("{\"name\":\"减脂计划\",\"goal\":\"减脂\",\"startDate\":\"2026-09-15\",\"endDate\":\"2026-09-29\",\"description\":\"每周三练\",\"items\":[{\"exerciseId\":1,\"dayOfWeek\":1,\"sets\":3,\"reps\":10,\"restSeconds\":60,\"weight\":0,\"notes\":\"\"}]}");
        var service = new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper());

        CreateWorkoutPlanCommand draft = service.generate(actor(), 42L, "我想减脂，每周训练三次");

        assertThat(draft.memberId()).isEqualTo(42L);
        assertThat(draft.items()).hasSize(1);
        assertThat(draft.items().get(0).sets()).isEqualTo(3);
    }

    @Test
    void rejectsModelPlanWithInvalidTrainingValues() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn("{\"name\":\"错误计划\",\"goal\":\"减脂\",\"startDate\":\"2026-09-15\",\"endDate\":\"2026-09-29\",\"description\":\"x\",\"items\":[{\"exerciseId\":1,\"dayOfWeek\":8,\"sets\":0,\"reps\":0,\"restSeconds\":-1,\"weight\":0,\"notes\":\"\"}]}");
        var service = new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper());

        assertThatThrownBy(() -> service.generate(actor(), 42L, "减脂"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void checksTenantAiQuotaBeforeGeneratingDraft() {
        var model = mock(ChatModelGateway.class);
        var quotas = mock(TenantQuotaService.class);
        doThrow(new com.gymmind.shared.error.BusinessException(com.gymmind.shared.error.ErrorCode.CONFLICT, "quota"))
                .when(quotas).ensureAiChatAllowed(7L);
        var service = new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper(), quotas);

        assertThatThrownBy(() -> service.generate(actor(), 42L, "减脂"))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verifyNoInteractions(model);
    }

    @Test
    void resolvesMemberTargetFromAuthenticatedMemberProfile() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn(validJson());
        var members = mock(MemberRepository.class);
        var member = mock(Member.class);
        when(member.getId()).thenReturn(42L);
        when(member.getUserId()).thenReturn(1L);
        when(members.findByTenantIdAndUserId(7L, 1L)).thenReturn(java.util.Optional.of(member));
        var service = service(model, members, mock(CoachRepository.class), mock(CoachAssignmentRepository.class),
                mock(WorkoutHistoryQueryService.class), mock(ExerciseRepository.class));

        var draft = service.generateDraft(actor(), null, "减脂", false);

        assertThat(draft.plan().memberId()).isEqualTo(42L);
    }

    @Test
    void rejectsMemberTargetIdThatDoesNotMatchAuthenticatedProfile() {
        var model = mock(ChatModelGateway.class);
        var members = mock(MemberRepository.class);
        var member = mock(Member.class);
        when(member.getId()).thenReturn(42L);
        when(member.getUserId()).thenReturn(1L);
        when(members.findByTenantIdAndUserId(7L, 1L)).thenReturn(java.util.Optional.of(member));
        var service = service(model, members, mock(CoachRepository.class), mock(CoachAssignmentRepository.class),
                mock(WorkoutHistoryQueryService.class), mock(ExerciseRepository.class));

        assertThatThrownBy(() -> service.generateDraft(actor(), 99L, "减脂", false))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(model);
    }

    @Test
    void injectsScopedHistoryOnlyWhenRequested() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn(validJson());
        var history = mock(WorkoutHistoryQueryService.class);
        when(history.summarize(eq(7L), eq(42L), any(), any(), eq(30)))
                .thenReturn(new WorkoutHistorySummary(2, "2026-09-20：深蹲 60.0kg x 8"));
        var service = service(model, null, mock(CoachRepository.class), mock(CoachAssignmentRepository.class), history,
                mock(ExerciseRepository.class));

        var draft = service.generateDraft(actor(), 42L, "减脂", true);

        assertThat(draft.history().included()).isTrue();
        assertThat(draft.history().recordCount()).isEqualTo(2);
        assertThat(draft.history().summary()).contains("深蹲");
        verify(model).complete(argThat(prompt -> prompt.contains("深蹲 60.0kg x 8")), anyList());
    }

    @Test
    void coachCannotTargetUnassignedMember() {
        var actor = new CurrentActor(8L, 7L, Set.of(RoleCode.COACH), Set.of("ai:plan"), 0, "token");
        var model = mock(ChatModelGateway.class);
        var coaches = mock(CoachRepository.class);
        var coach = mock(Coach.class);
        when(coach.getId()).thenReturn(11L);
        when(coaches.findByTenantIdAndUserId(7L, 8L)).thenReturn(java.util.Optional.of(coach));
        var assignments = mock(CoachAssignmentRepository.class);
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndActive(7L, 11L, 42L)).thenReturn(false);
        var service = service(model, null, coaches, assignments, mock(WorkoutHistoryQueryService.class),
                mock(ExerciseRepository.class));

        assertThatThrownBy(() -> service.generateDraft(actor, 42L, "减脂", false))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(model);
    }

    private DefaultAiWorkoutPlanDraftService service(ChatModelGateway model, MemberRepository members,
                                                       CoachRepository coaches, CoachAssignmentRepository assignments,
                                                       WorkoutHistoryQueryService history, ExerciseRepository exercises) {
        return new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper(), mock(TenantQuotaService.class),
                members, coaches, assignments, history, exercises);
    }

    private String validJson() {
        return "{\"name\":\"减脂计划\",\"goal\":\"减脂\",\"startDate\":\"2026-09-15\",\"endDate\":\"2026-09-29\",\"description\":\"每周三练\",\"items\":[{\"exerciseId\":1,\"dayOfWeek\":1,\"sets\":3,\"reps\":10,\"restSeconds\":60,\"weight\":0,\"notes\":\"\"}]}";
    }

    private CurrentActor actor() {
        return new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:plan"), 0, "token");
    }
}
