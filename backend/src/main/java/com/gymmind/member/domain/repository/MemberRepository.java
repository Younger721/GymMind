package com.gymmind.member.domain.repository;

import com.gymmind.member.domain.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findByTenantIdAndId(Long tenantId, Long id);
    Optional<Member> findByTenantIdAndUserId(Long tenantId, Long userId);
    Optional<Member> findByTenantIdAndMemberNumber(Long tenantId, String memberNumber);
    boolean existsByTenantIdAndPhone(Long tenantId, String phone);
    Page<Member> findAllByTenantId(Long tenantId, Pageable pageable);
}
