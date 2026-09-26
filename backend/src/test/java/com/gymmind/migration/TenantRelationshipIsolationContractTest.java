package com.gymmind.migration;

import com.gymmind.coach.domain.model.CoachAssignmentStatus;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TenantRelationshipIsolationContractTest {
    @Test
    void relationQueriesAlwaysCarryTenantId() {
        CoachAssignmentRepository repository = mock(CoachAssignmentRepository.class);
        when(repository.existsByTenantIdAndCoachIdAndMemberIdAndStatus(11L, 3L, 7L, CoachAssignmentStatus.ACTIVE)).thenReturn(true);
        when(repository.existsByTenantIdAndCoachIdAndMemberIdAndStatus(12L, 3L, 7L, CoachAssignmentStatus.ACTIVE)).thenReturn(false);

        assertThat(repository.existsByTenantIdAndCoachIdAndMemberIdAndStatus(11L, 3L, 7L, CoachAssignmentStatus.ACTIVE)).isTrue();
        assertThat(repository.existsByTenantIdAndCoachIdAndMemberIdAndStatus(12L, 3L, 7L, CoachAssignmentStatus.ACTIVE)).isFalse();
        verify(repository).existsByTenantIdAndCoachIdAndMemberIdAndStatus(eq(11L), eq(3L), eq(7L), eq(CoachAssignmentStatus.ACTIVE));
        verify(repository).existsByTenantIdAndCoachIdAndMemberIdAndStatus(eq(12L), eq(3L), eq(7L), eq(CoachAssignmentStatus.ACTIVE));
    }
}
