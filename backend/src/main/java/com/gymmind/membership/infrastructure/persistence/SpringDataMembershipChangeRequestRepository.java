package com.gymmind.membership.infrastructure.persistence;

import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataMembershipChangeRequestRepository extends JpaRepository<MembershipChangeRequest, Long> {

    Optional<MembershipChangeRequest> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndMembershipIdAndStatus(Long tenantId, Long membershipId,
                                                      MembershipChangeRequestStatus status);

    List<MembershipChangeRequest> findAllByTenantIdOrderByIdDesc(Long tenantId);

    List<MembershipChangeRequest> findAllByTenantIdAndStatusOrderByIdDesc(Long tenantId,
                                                                          MembershipChangeRequestStatus status);

    List<MembershipChangeRequest> findAllByTenantIdAndMemberIdOrderByIdDesc(Long tenantId, Long memberId);
}
