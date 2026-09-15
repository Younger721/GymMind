package com.gymmind.member.infrastructure.persistence;

import com.gymmind.member.domain.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SpringDataMemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByTenantIdAndId(Long tenantId, Long id);
    Optional<Member> findByTenantIdAndUserId(Long tenantId, Long userId);
    Optional<Member> findByTenantIdAndMemberNumber(Long tenantId, String memberNumber);
    boolean existsByTenantIdAndPhone(Long tenantId, String phone);
    Page<Member> findAllByTenantId(Long tenantId, Pageable pageable);
    Page<Member> findByTenantIdAndFullNameContainingIgnoreCaseOrTenantIdAndMemberNumberContainingIgnoreCase(Long tenantId, String fullName, Long sameTenantId, String memberNumber, Pageable pageable);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, com.gymmind.member.domain.model.MemberStatus status);
}
