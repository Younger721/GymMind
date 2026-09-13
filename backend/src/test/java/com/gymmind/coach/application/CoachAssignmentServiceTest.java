package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
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
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndActive(eq(11L), anyLong(), eq(41L))).thenReturn(true);
        when(assignments.existsByTenantIdAndCoachIdAndMemberIdAndActive(eq(12L), anyLong(), eq(41L))).thenReturn(false);
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class));

        assertThat(service.canAccess(coachActor(11L, 31L), 41L)).isTrue();
        assertThat(service.canAccess(coachActor(12L, 31L), 41L)).isFalse();
    }

    @Test
    void assignUsesActorTenantAndCanBeUnassigned() {
        CoachRepository coaches = mock(CoachRepository.class);
        CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
        when(coaches.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.of(Coach.create(11L, 31L, "C-001", "Coach", "13900000000")));
        when(assignments.findActiveByTenantIdAndCoachIdAndMemberId(11L, 3L, 41L)).thenReturn(Optional.empty());
        when(assignments.save(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CoachAssignmentService service = new DefaultCoachAssignmentService(coaches, assignments, mock(AuditRecorder.class));

        service.assign(admin(11L), new AssignCoachCommand(null, 3L, 41L));
        verify(assignments).save(any(CoachAssignment.class));
        service.unassign(admin(11L), 3L, 41L);
        verify(assignments).deleteActiveByTenantIdAndCoachIdAndMemberId(11L, 3L, 41L);
    }

    private static CurrentActor admin(long tenantId) { return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("coach:assign"), 0L, "token"); }
    private static CurrentActor coachActor(long tenantId, long userId) { return new CurrentActor(userId, tenantId, Set.of(RoleCode.COACH), Set.of("coach:read"), 0L, "token"); }
}
