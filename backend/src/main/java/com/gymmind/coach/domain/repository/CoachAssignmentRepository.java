package com.gymmind.coach.domain.repository;

import com.gymmind.coach.domain.model.CoachAssignment;
import java.util.Optional;
import java.util.List;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;

public interface CoachAssignmentRepository {
    CoachAssignment save(CoachAssignment assignment);
    Optional<CoachAssignment> findActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId);
    boolean existsByTenantIdAndCoachIdAndMemberIdAndActive(Long tenantId, Long coachId, Long memberId);
    List<Long> findActiveMemberIdsByTenantIdAndCoachId(Long tenantId, Long coachId);
    void deleteActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId);
    Optional<CoachAssignment> findByIdAndTenantId(Long id, Long tenantId);
    Optional<CoachAssignment> findPendingByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId);
    boolean existsByTenantIdAndCoachIdAndMemberIdAndStatus(Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status);
}
