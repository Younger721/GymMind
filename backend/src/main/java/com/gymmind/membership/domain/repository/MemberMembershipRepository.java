package com.gymmind.membership.domain.repository;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MemberMembershipRepository {
    MemberMembership save(MemberMembership m);

    Page<MemberMembership> findAllByTenantIdAndMemberId(Long t, Long m, Pageable p);

    List<MemberMembership> findAllByTenantIdAndMemberIdAndStatus(Long t, Long m, MemberMembershipStatus status);

    Optional<MemberMembership> findActiveByTenantIdAndMemberId(Long t, Long m);

    Optional<MemberMembership> findByTenantIdAndId(Long tenantId, Long id);

    /** 存在停卡区间配置的会籍（供定时任务刷新状态） */
    java.util.List<MemberMembership> findAllWithFreezePeriod();
}
