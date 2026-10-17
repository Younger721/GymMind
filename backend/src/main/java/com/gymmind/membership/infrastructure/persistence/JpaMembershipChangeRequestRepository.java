package com.gymmind.membership.infrastructure.persistence;

import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.membership.domain.repository.MembershipChangeRequestRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaMembershipChangeRequestRepository implements MembershipChangeRequestRepository {

    private final SpringDataMembershipChangeRequestRepository delegate;

    JpaMembershipChangeRequestRepository(SpringDataMembershipChangeRequestRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public MembershipChangeRequest save(MembershipChangeRequest request) {
        return delegate.save(request);
    }

    @Override
    public Optional<MembershipChangeRequest> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public boolean existsByTenantIdAndMembershipIdAndStatus(Long tenantId, Long membershipId,
                                                            MembershipChangeRequestStatus status) {
        return delegate.existsByTenantIdAndMembershipIdAndStatus(tenantId, membershipId, status);
    }

    @Override
    public List<MembershipChangeRequest> findAllByTenantIdOrderByIdDesc(Long tenantId) {
        return delegate.findAllByTenantIdOrderByIdDesc(tenantId);
    }

    @Override
    public List<MembershipChangeRequest> findAllByTenantIdAndStatusOrderByIdDesc(Long tenantId,
                                                                                 MembershipChangeRequestStatus status) {
        return delegate.findAllByTenantIdAndStatusOrderByIdDesc(tenantId, status);
    }

    @Override
    public List<MembershipChangeRequest> findAllByTenantIdAndMemberIdOrderByIdDesc(Long tenantId, Long memberId) {
        return delegate.findAllByTenantIdAndMemberIdOrderByIdDesc(tenantId, memberId);
    }
}
