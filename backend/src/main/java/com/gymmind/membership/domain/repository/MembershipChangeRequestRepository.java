package com.gymmind.membership.domain.repository;

import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;

import java.util.List;
import java.util.Optional;

public interface MembershipChangeRequestRepository {
    MembershipChangeRequest save(MembershipChangeRequest request);

    Optional<MembershipChangeRequest> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndMembershipIdAndStatus(Long tenantId, Long membershipId,
                                                     MembershipChangeRequestStatus status);

    List<MembershipChangeRequest> findAllByTenantIdOrderByIdDesc(Long tenantId);

    List<MembershipChangeRequest> findAllByTenantIdAndStatusOrderByIdDesc(Long tenantId,
                                                                            MembershipChangeRequestStatus status);

    List<MembershipChangeRequest> findAllByTenantIdAndMemberIdOrderByIdDesc(Long tenantId, Long memberId);
}
