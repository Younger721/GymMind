package com.gymmind.member.infrastructure.persistence;

import com.gymmind.member.domain.model.MemberTenant;
import com.gymmind.member.domain.repository.MemberTenantRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
class JpaMemberTenantRepository implements MemberTenantRepository {
    private final SpringDataMemberTenantRepository delegate;
    JpaMemberTenantRepository(SpringDataMemberTenantRepository delegate) { this.delegate = delegate; }
    public MemberTenant save(MemberTenant membership) { return delegate.save(membership); }
    public Optional<MemberTenant> findByTenantIdAndMemberId(Long tenantId, Long memberId) { return delegate.findByTenantIdAndMemberId(tenantId, memberId); }
    public boolean existsByTenantIdAndMemberId(Long tenantId, Long memberId) { return delegate.existsByTenantIdAndMemberId(tenantId, memberId); }
    public List<MemberTenant> findAllByMemberId(Long memberId) { return delegate.findAllByMemberId(memberId); }
}
