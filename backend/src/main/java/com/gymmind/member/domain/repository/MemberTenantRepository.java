package com.gymmind.member.domain.repository;

import com.gymmind.member.domain.model.MemberTenant;
import java.util.List;
import java.util.Optional;

public interface MemberTenantRepository {
    MemberTenant save(MemberTenant membership);
    Optional<MemberTenant> findByTenantIdAndMemberId(Long tenantId, Long memberId);
    boolean existsByTenantIdAndMemberId(Long tenantId, Long memberId);
    List<MemberTenant> findAllByMemberId(Long memberId);
}
