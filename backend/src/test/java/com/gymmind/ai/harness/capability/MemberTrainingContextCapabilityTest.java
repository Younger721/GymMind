package com.gymmind.ai.harness.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.ai.harness.plugins.MemberTrainingContextCapability;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberTrainingContextCapabilityTest {
    @Test
    void memberCannotChooseAnotherMemberId() throws Exception {
        MemberRepository members = mock(MemberRepository.class);
        when(members.findByTenantIdAndUserId(7L, 11L)).thenReturn(Optional.empty());
        var capability = new MemberTrainingContextCapability(
                mock(WorkoutRecordRepository.class), mock(WorkoutPlanRepository.class), members,
                mock(CoachAssignmentService.class));

        var arguments = new ObjectMapper().readTree("{\"memberId\":999}");
        assertThatThrownBy(() -> capability.execute(
                new CurrentActor(11L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "token"),
                arguments, new ToolExecutionContext("trace", "训练情况", ignored -> { })))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coachMustHaveMemberAssignment() throws Exception {
        var assignments = mock(CoachAssignmentService.class);
        when(assignments.canAccess(any(), eq(22L))).thenReturn(false);
        var capability = new MemberTrainingContextCapability(
                mock(WorkoutRecordRepository.class), mock(WorkoutPlanRepository.class),
                mock(MemberRepository.class), assignments);

        assertThatThrownBy(() -> capability.execute(
                new CurrentActor(12L, 7L, Set.of(RoleCode.COACH), Set.of("ai:chat"), 0, "token"),
                new ObjectMapper().readTree("{\"memberId\":22}"),
                new ToolExecutionContext("trace", "会员训练", ignored -> { })))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
