package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository class JpaCoachAssignmentRepository implements CoachAssignmentRepository {
    private final SpringDataCoachAssignmentRepository delegate;
    JpaCoachAssignmentRepository(SpringDataCoachAssignmentRepository delegate) { this.delegate = delegate; }
    public CoachAssignment save(CoachAssignment assignment) { return delegate.save(assignment); }
    public Optional<CoachAssignment> findActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId) { return delegate.findByTenantIdAndCoachIdAndMemberIdAndActiveTrue(tenantId, coachId, memberId); }
    public boolean existsByTenantIdAndCoachIdAndMemberIdAndActive(Long tenantId, Long coachId, Long memberId) { return delegate.existsByTenantIdAndCoachIdAndMemberIdAndActiveTrue(tenantId, coachId, memberId); }
    public void deleteActiveByTenantIdAndCoachIdAndMemberId(Long tenantId, Long coachId, Long memberId) { delegate.deleteByTenantIdAndCoachIdAndMemberIdAndActiveTrue(tenantId, coachId, memberId); }
}
