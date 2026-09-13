package com.gymmind.membership.infrastructure.persistence;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataMemberMembershipRepository extends JpaRepository<MemberMembership, Long> {
    Page<MemberMembership> findAllByTenantIdAndMemberId(Long t, Long m, Pageable p);

    List<MemberMembership> findAllByTenantIdAndMemberIdAndStatus(Long t, Long m, MemberMembershipStatus status);
}
