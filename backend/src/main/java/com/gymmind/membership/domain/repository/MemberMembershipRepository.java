package com.gymmind.membership.domain.repository;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberMembershipRepository {
    MemberMembership save(MemberMembership m);

    Page<MemberMembership> findAllByTenantIdAndMemberId(Long t, Long m, Pageable p);

    List<MemberMembership> findAllByTenantIdAndMemberIdAndStatus(Long t, Long m, MemberMembershipStatus status);
}
