package com.gymmind.coach.domain.repository;

import com.gymmind.coach.domain.model.CoachAssignment;
import java.util.Optional;

public interface CoachAssignmentRepository {
    CoachAssignment save(CoachAssignment assignment);
    Optional<CoachAssignment> findActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId);
    boolean existsByTenantIdAndCoachIdAndMemberIdAndActive(Long tenantId, Long coachId, Long memberId);
    void deleteActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId);
}
