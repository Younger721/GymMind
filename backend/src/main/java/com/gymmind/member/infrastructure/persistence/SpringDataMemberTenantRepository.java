package com.gymmind.member.infrastructure.persistence;

import com.gymmind.member.domain.model.MemberTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface SpringDataMemberTenantRepository extends JpaRepository<MemberTenant, Long> {
    Optional<MemberTenant> findByTenantIdAndMemberId(Long tenantId, Long memberId);
    boolean existsByTenantIdAndMemberId(Long tenantId, Long memberId);
    List<MemberTenant> findAllByMemberId(Long memberId);
}
