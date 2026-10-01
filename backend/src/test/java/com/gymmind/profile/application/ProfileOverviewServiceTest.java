package com.gymmind.profile.application;

import com.gymmind.booking.domain.repository.GymCheckInRepository;
import com.gymmind.booking.domain.model.GymCheckIn;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileOverviewServiceTest {

    @Test
    void memberOverviewIncludesCheckInAndPackageData() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        GymCheckInRepository checkIns = mock(GymCheckInRepository.class);
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MembershipPackageRepository packages = mock(MembershipPackageRepository.class);
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "member", "hash", "Member", RoleCode.MEMBER);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 7L);
        Member member = Member.create(10L, 7L, "M-001", "Member", "13800000000");
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", 12L);
        MemberMembership membership = MemberMembership.create(10L, 12L, 21L, null,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));
        MembershipPackage packageValue = MembershipPackage.create(10L, "月卡-基础", "TIME", null, 30,
                BigDecimal.ZERO, "CNY");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(members.findByTenantIdAndUserId(10L, 7L)).thenReturn(Optional.of(member));
        when(checkIns.countByTenantIdAndMemberIdAndCheckInAtBetween(eq(10L), eq(12L), any(), any())).thenReturn(4L);
        when(checkIns.findByTenantIdAndMemberIdAndCheckInAtBetween(eq(10L), eq(12L), any(), any())).thenReturn(List.of(
                GymCheckIn.create(10L, 12L, Instant.parse("2026-09-05T01:00:00Z"), 3L, null),
                GymCheckIn.create(10L, 12L, Instant.parse("2026-09-05T08:00:00Z"), 3L, null),
                GymCheckIn.create(10L, 12L, Instant.parse("2026-09-18T01:00:00Z"), 3L, null)));
        when(memberships.findActiveByTenantIdAndMemberId(10L, 12L)).thenReturn(Optional.of(membership));
        when(packages.findByTenantIdAndId(10L, 21L)).thenReturn(Optional.of(packageValue));

        ProfileOverviewService service = new DefaultProfileOverviewService(users, members, checkIns, memberships, packages);
        ProfileOverview overview = service.overview(new CurrentActor(7L, 10L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "token"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(overview.checkInCount()).isEqualTo(4L);
        assertThat(overview.checkInDates()).containsExactly(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 18));
        assertThat(overview.member()).isNotNull();
        assertThat(overview.member().checkInToken()).isEqualTo(member.getCheckInToken());
        assertThat(overview.membership().packageName()).isEqualTo("月卡-基础");
        assertThat(overview.membership().membershipType()).isEqualTo("月卡");
    }

    @Test
    void nonMemberOverviewOmitsMemberData() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        UserAccount user = UserAccount.tenantUser(10L, "coach@example.com", "coach", "hash", "Coach", RoleCode.COACH);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 8L);
        when(users.findById(8L)).thenReturn(Optional.of(user));

        ProfileOverviewService service = new DefaultProfileOverviewService(users, mock(MemberRepository.class),
                mock(GymCheckInRepository.class), mock(MemberMembershipRepository.class), mock(MembershipPackageRepository.class));

        ProfileOverview overview = service.overview(new CurrentActor(8L, 10L, Set.of(RoleCode.COACH), Set.of(), 0L, "token"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(overview.member()).isNull();
        assertThat(overview.membership()).isNull();
        assertThat(overview.checkInCount()).isZero();
    }

}
