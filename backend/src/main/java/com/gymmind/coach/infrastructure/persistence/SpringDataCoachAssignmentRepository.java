package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.CoachAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface SpringDataCoachAssignmentRepository extends JpaRepository<CoachAssignment, Long> {
    Optional<CoachAssignment> findByTenantIdAndCoachIdAndMemberIdAndActiveTrue(Long tenantId, Long coachId, Long memberId);
    boolean existsByTenantIdAndCoachIdAndMemberIdAndActiveTrue(Long tenantId, Long coachId, Long memberId);
    long deleteByTenantIdAndCoachIdAndMemberIdAndActiveTrue(Long tenantId, Long coachId, Long memberId);
}
