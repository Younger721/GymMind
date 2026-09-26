package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;
@Repository class JpaCoachAssignmentRepository implements CoachAssignmentRepository {
    private final SpringDataCoachAssignmentRepository delegate;
    JpaCoachAssignmentRepository(SpringDataCoachAssignmentRepository delegate) { this.delegate = delegate; }
    public CoachAssignment save(CoachAssignment assignment) { return delegate.save(assignment); }
    public Optional<CoachAssignment> findActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId) { return delegate.findByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.ACTIVE); }
    public boolean existsByTenantIdAndCoachIdAndMemberIdAndActive(Long tenantId, Long coachId, Long memberId) { return delegate.existsByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.ACTIVE); }
    public List<Long> findActiveMemberIdsByTenantIdAndCoachId(Long tenantId, Long coachId) { return delegate.findMemberIdsByTenantIdAndCoachId(tenantId, coachId); }
    public void deleteActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId) { delegate.deleteByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.ACTIVE); }
    public Optional<CoachAssignment> findByIdAndTenantId(Long id, Long tenantId) { return delegate.findByIdAndTenantId(id, tenantId); }
    public Optional<CoachAssignment> findPendingByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId) { return delegate.findByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.PENDING); }
    public boolean existsByTenantIdAndCoachIdAndMemberIdAndStatus(Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status) { return delegate.existsByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, status); }
}
