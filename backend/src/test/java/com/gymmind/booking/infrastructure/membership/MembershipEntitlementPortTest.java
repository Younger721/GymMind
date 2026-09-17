package com.gymmind.booking.infrastructure.membership;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipEntitlementPortTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void reserveConsumesCountBasedEntitlement() {
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MemberMembership membership = MemberMembership.create(11L, 41L, 8L, 3,
                FIXED_NOW.minusSeconds(3600), FIXED_NOW.plusSeconds(3600));
        when(memberships.findAllByTenantIdAndMemberIdAndStatus(11L, 41L, MemberMembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(memberships.save(any(MemberMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultMembershipEntitlementPort port = new DefaultMembershipEntitlementPort(memberships, CLOCK);
        port.reserve(11L, 41L);

        assertThat(membership.getRemainingCount()).isEqualTo(2);
        verify(memberships).save(membership);
    }

    @Test
    void releaseRestoresCountBasedEntitlement() {
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MemberMembership membership = MemberMembership.create(11L, 41L, 8L, 2,
                FIXED_NOW.minusSeconds(3600), FIXED_NOW.plusSeconds(3600));
        when(memberships.findAllByTenantIdAndMemberIdAndStatus(11L, 41L, MemberMembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(memberships.save(any(MemberMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultMembershipEntitlementPort port = new DefaultMembershipEntitlementPort(memberships, CLOCK);
        port.release(11L, 41L);

        assertThat(membership.getRemainingCount()).isEqualTo(3);
    }

    @Test
    void reserveFailsWhenNoActiveMembership() {
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        when(memberships.findAllByTenantIdAndMemberIdAndStatus(11L, 41L, MemberMembershipStatus.ACTIVE))
                .thenReturn(List.of());

        DefaultMembershipEntitlementPort port = new DefaultMembershipEntitlementPort(memberships, CLOCK);

        assertThatThrownBy(() -> port.reserve(11L, 41L))
                .isInstanceOf(BusinessException.class);
    }
}
