package com.gymmind.membership.infrastructure.persistence;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
