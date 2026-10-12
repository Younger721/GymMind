package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoachAssignmentServiceTest {
    @Test
    void coachCanAccessOnlyActivelyAssignedMemberInSameTenant() {
        CoachRepository coaches = mock(CoachRepository.class);
        CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
        Coach coach = mock(Coach.class);
        when(coach.getId()).thenReturn(3L);
        when(coach.getUserId()).thenReturn(31L);
        when(coaches.findByTenantIdAndUserId(11L, 31L)).thenReturn(Optional.of(coach));
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(eq(11L), anyLong(), eq(41L), any())).thenReturn(true);
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(eq(12L), anyLong(), eq(41L), any())).thenReturn(false);
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class));

        assertThat(service.canAccess(coachActor(11L, 31L), 41L)).isTrue();
        assertThat(service.canAccess(coachActor(12L, 31L), 41L)).isFalse();
    }

    @Test
    void assignUsesActorTenantAndCanBeUnassigned() {
        CoachRepository coaches = mock(CoachRepository.class);
        CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
        var memberships = mock(com.gymmind.member.domain.repository.MemberTenantRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.of(Coach.create(11L, 31L, "C-001", "Coach", "13900000000")));
        when(memberships.existsByTenantIdAndMemberId(11L, 41L)).thenReturn(true);
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndActive(11L, 3L, 41L)).thenReturn(true);
        when(assignments.findActiveByTenantIdAndCoachIdAndMemberId(11L, 3L, 41L)).thenReturn(Optional.empty());
        when(assignments.save(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class), null, memberships);

        service.assign(admin(11L), new AssignCoachCommand(null, 3L, 41L));
        verify(assignments).save(any(CoachAssignment.class));
        service.unassign(admin(11L), 3L, 41L);
        verify(assignments).deleteActiveByTenantIdAndCoachIdAndMemberId(11L, 3L, 41L);
    }

    @Test
    void memberCanRequestAndCoachCanAccept() {
        CoachRepository coaches = mock(CoachRepository.class);
        CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.of(Coach.create(11L, 31L, "C-001", "Coach", "13900000000")));
        var members = mock(com.gymmind.member.domain.repository.MemberRepository.class);
        var memberships = mock(com.gymmind.member.domain.repository.MemberTenantRepository.class);
        var member = com.gymmind.member.domain.model.Member.create(41L, "Member");
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", 1L);
        when(members.findByUserId(41L)).thenReturn(Optional.of(member));
        when(memberships.existsByTenantIdAndMemberId(11L, 1L)).thenReturn(true);
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(eq(11L), eq(3L), eq(1L), any())).thenReturn(false);
        when(assignments.save(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class), members, memberships);

        service.request(memberActor(11L, 41L), 3L);
        verify(assignments).save(any(CoachAssignment.class));
    }

    @Test
    void assignRejectsMemberOutsideTenant() {
        CoachRepository coaches = mock(CoachRepository.class);
        CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
        var memberships = mock(com.gymmind.member.domain.repository.MemberTenantRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.of(Coach.create(11L, 31L, "C-001", "Coach", "13900000000")));
        when(memberships.existsByTenantIdAndMemberId(11L, 99L)).thenReturn(false);
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class), null, memberships);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assign(admin(11L), new AssignCoachCommand(null, 3L, 99L)))
                .isInstanceOf(BusinessException.class);
        verify(assignments, never()).save(any());
    }

    @Test
    void coachCannotRequest() {
        CoachAssignmentService service = new DefaultCoachAssignmentService(mock(CoachRepository.class), mock(CoachAssignmentRepository.class), mock(AuditRecorder.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.request(coachActor(11L, 31L), 3L))
                .isInstanceOf(BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) { return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("coach:assign"), 0L, "token"); }
    private static CurrentActor coachActor(long tenantId, long userId) { return new CurrentActor(userId, tenantId, Set.of(RoleCode.COACH), Set.of("coach:read"), 0L, "token"); }
    private static CurrentActor memberActor(long tenantId, long userId) { return new CurrentActor(userId, tenantId, Set.of(RoleCode.MEMBER), Set.of(), 0L, "token"); }
}
