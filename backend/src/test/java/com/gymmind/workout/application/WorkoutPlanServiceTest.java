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
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(mock(com.gymmind.exercise.domain.model.Exercise.class)));
        when(assignments.canAccess(any(), eq(41L))).thenReturn(true);
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("workout:write"), 0L, "token");
    }
}
