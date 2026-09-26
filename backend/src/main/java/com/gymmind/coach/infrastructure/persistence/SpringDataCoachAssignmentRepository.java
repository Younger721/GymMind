package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.CoachAssignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;
interface SpringDataCoachAssignmentRepository extends JpaRepository<CoachAssignment, Long> {
    Optional<CoachAssignment> findByTenantIdAndCoachIdAndMemberIdAndStatus(Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status);
    boolean existsByTenantIdAndCoachIdAndMemberIdAndStatus(Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status);
    @Query("select a.memberId from CoachAssignment a where a.tenantId = :tenantId and a.coachId = :coachId and a.status = com.gymmind.coach.domain.model.CoachAssignmentStatus.ACTIVE")
    List<Long> findMemberIdsByTenantIdAndCoachId(@Param("tenantId") Long tenantId, @Param("coachId") Long coachId);
    long deleteByTenantIdAndCoachIdAndMemberIdAndStatus(Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status);
    Optional<CoachAssignment> findByIdAndTenantId(Long id, Long tenantId);
}
