package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.member.domain.repository.MemberTenantRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.membership.domain.model.MembershipChangeType;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipChangeRequestRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipChangeRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void memberCanCreateFreezeRequest() {
        MembershipChangeRequestRepository requests = mock(MembershipChangeRequestRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        MemberTenantRepository tenants = mock(MemberTenantRepository.class);
        MemberMembership membership = activeMembership();
        when(memberships.findByTenantIdAndId(11L, 100L)).thenReturn(Optional.of(membership));
        when(requests.existsByTenantIdAndMembershipIdAndStatus(11L, 100L, MembershipChangeRequestStatus.PENDING))
                .thenReturn(false);
        com.gymmind.member.domain.model.Member member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 50L)).thenReturn(Optional.of(member));
        when(requests.save(any(MembershipChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipChangeRequestService service = new DefaultMembershipChangeRequestService(
                requests, memberships, members, tenants, mock(AuditRecorder.class), CLOCK);

        MembershipChangeRequestView view = service.create(memberActor(), new CreateMembershipChangeCommand(
                MembershipChangeType.FREEZE, 100L,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22),
                null, "出差"));

        assertThat(view.type()).isEqualTo(MembershipChangeType.FREEZE);
        assertThat(view.status()).isEqualTo(MembershipChangeRequestStatus.PENDING);
    }

    @Test
    void gymAdminApprovesTransfer() {
        MembershipChangeRequestRepository requests = mock(MembershipChangeRequestRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        MemberTenantRepository tenants = mock(MemberTenantRepository.class);
        MemberMembership membership = activeMembership();
        MembershipChangeRequest pending = MembershipChangeRequest.createTransfer(
                11L, 100L, 41L, 50L, 42L, "转卡");
        when(requests.findByTenantIdAndId(11L, 9L)).thenReturn(Optional.of(pending));
        when(memberships.findByTenantIdAndId(11L, 100L)).thenReturn(Optional.of(membership));
        when(tenants.existsByTenantIdAndMemberId(11L, 42L)).thenReturn(true);
        when(requests.save(any(MembershipChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any(MemberMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipChangeRequestService service = new DefaultMembershipChangeRequestService(
                requests, memberships, members, tenants, mock(AuditRecorder.class), CLOCK);

        MembershipChangeRequestView view = service.approve(gymAdmin(2L), 9L, "同意");

        assertThat(view.status()).isEqualTo(MembershipChangeRequestStatus.APPROVED);
        verify(memberships).save(any(MemberMembership.class));
    }

    @Test
    void platformAdminCannotApprove() {
        MembershipChangeRequestService service = new DefaultMembershipChangeRequestService(
                mock(MembershipChangeRequestRepository.class),
                mock(MemberMembershipRepository.class),
                mock(MemberRepository.class),
                mock(MemberTenantRepository.class),
                mock(AuditRecorder.class),
                CLOCK);

        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("membership:change:approve"), 0L, "token");

        assertThatThrownBy(() -> service.approve(platform, 9L, "同意"))
                .isInstanceOf(BusinessException.class);
    }

    /** 平台管理员进入会籍审批页时应能查看所选门店的列表，不再因缺少会员档案而报 404。 */
    @Test
    void platformAdminCanViewTenantList() {
        MembershipChangeRequestRepository requests = mock(MembershipChangeRequestRepository.class);
        MembershipChangeRequest pending = MembershipChangeRequest.createFreeze(
                11L, 100L, 41L, 50L,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), "出差");
        when(requests.findAllByTenantIdOrderByIdDesc(11L)).thenReturn(List.of(pending));

        MembershipChangeRequestService service = new DefaultMembershipChangeRequestService(
                requests,
                mock(MemberMembershipRepository.class),
                mock(MemberRepository.class),
                mock(MemberTenantRepository.class),
                mock(AuditRecorder.class),
                CLOCK);

        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("membership:change:read"), 0L, "token");

        // 平台管理员无自身租户，租户上下文由 PlatformTenantContextFilter 注入
        TenantContextHolder.set(11L);
        try {
            List<MembershipChangeRequestView> rows = service.list(platform, null);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).status()).isEqualTo(MembershipChangeRequestStatus.PENDING);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void coachCannotCreateRequest() {
        MembershipChangeRequestService service = new DefaultMembershipChangeRequestService(
                mock(MembershipChangeRequestRepository.class),
                mock(MemberMembershipRepository.class),
                mock(MemberRepository.class),
                mock(MemberTenantRepository.class),
                mock(AuditRecorder.class),
                CLOCK);

        CurrentActor coach = new CurrentActor(3L, 11L, Set.of(RoleCode.COACH),
                Set.of("membership:change:request"), 0L, "token");

        assertThatThrownBy(() -> service.create(coach, new CreateMembershipChangeCommand(
                MembershipChangeType.FREEZE, 100L,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22),
                null, "出差"))).isInstanceOf(BusinessException.class);
    }

    private static MemberMembership activeMembership() {
        return MemberMembership.create(
                11L, 41L, 8L, 10, Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"));
    }

    private static CurrentActor memberActor() {
        return new CurrentActor(50L, 11L, Set.of(RoleCode.MEMBER),
                Set.of("membership:change:request", "membership:change:read", "membership:change:cancel"),
                0L, "token");
    }

    private static CurrentActor gymAdmin(long userId) {
        return new CurrentActor(userId, 11L, Set.of(RoleCode.GYM_ADMIN),
                Set.of("membership:change:approve", "membership:change:read"), 0L, "token");
    }
}
