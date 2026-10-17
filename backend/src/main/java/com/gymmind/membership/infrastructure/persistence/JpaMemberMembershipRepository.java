package com.gymmind.membership.infrastructure.persistence;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaMemberMembershipRepository implements MemberMembershipRepository {
    private final SpringDataMemberMembershipRepository d;

    JpaMemberMembershipRepository(SpringDataMemberMembershipRepository d) { this.d = d; }

    public MemberMembership save(MemberMembership m) { return d.save(m); }

    public Page<MemberMembership> findAllByTenantIdAndMemberId(Long t, Long m, Pageable p) {
        return d.findAllByTenantIdAndMemberId(t, m, p);
    }

    public List<MemberMembership> findAllByTenantIdAndMemberIdAndStatus(Long t, Long m, MemberMembershipStatus status) {
        return d.findAllByTenantIdAndMemberIdAndStatus(t, m, status);
    }

    public Optional<MemberMembership> findActiveByTenantIdAndMemberId(Long t, Long m) {
        return d.findFirstByTenantIdAndMemberIdAndStatusOrderByExpiresAtDesc(
                t, m, MemberMembershipStatus.ACTIVE);
    }

    @Override
    public Optional<MemberMembership> findByTenantIdAndId(Long tenantId, Long id) {
        return d.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public List<MemberMembership> findAllWithFreezePeriod() {
        return d.findAllByFreezeUntilIsNotNull();
    }
}
