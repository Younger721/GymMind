package com.gymmind.member.infrastructure.persistence;

import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
class JpaMemberRepository implements MemberRepository {
    private final SpringDataMemberRepository delegate;
    JpaMemberRepository(SpringDataMemberRepository delegate) { this.delegate = delegate; }
    public Member save(Member member) { return delegate.save(member); }
    public Optional<Member> findByTenantIdAndId(Long tenantId, Long id) { return delegate.findByTenantIdAndId(tenantId, id); }
    public Optional<Member> findByTenantIdAndUserId(Long tenantId, Long userId) { return delegate.findByTenantIdAndUserId(tenantId, userId); }
    public Optional<Member> findByTenantIdAndMemberNumber(Long tenantId, String number) { return delegate.findByTenantIdAndMemberNumber(tenantId, number); }
    public boolean existsByTenantIdAndPhone(Long tenantId, String phone) { return delegate.existsByTenantIdAndPhone(tenantId, phone); }
    public Page<Member> findAllByTenantId(Long tenantId, Pageable pageable) { return delegate.findAllByTenantId(tenantId, pageable); }
    public Page<Member> searchByTenantId(Long tenantId, String query, Pageable pageable) { return delegate.findByTenantIdAndFullNameContainingIgnoreCaseOrTenantIdAndMemberNumberContainingIgnoreCase(tenantId, query, tenantId, query, pageable); }
    public long countByTenantId(Long tenantId) { return delegate.countByTenantId(tenantId); }
    public long countByTenantIdAndStatus(Long tenantId, com.gymmind.member.domain.model.MemberStatus status) { return delegate.countByTenantIdAndStatus(tenantId,status); }
}
