package com.gymmind.booking.infrastructure.membership;

import com.gymmind.booking.application.port.MembershipEntitlementPort;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;

@Component
public class DefaultMembershipEntitlementPort implements MembershipEntitlementPort {

    private final MemberMembershipRepository memberships;
    private final Clock clock;

    public DefaultMembershipEntitlementPort(MemberMembershipRepository memberships, Clock clock) {
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public void reserve(Long tenantId, Long memberId) {
        MemberMembership membership = activeMembership(tenantId, memberId);
        if (membership.getRemainingCount() != null) {
            membership.consume();
            memberships.save(membership);
        }
    }

    @Override
    public void release(Long tenantId, Long memberId) {
        MemberMembership membership = activeMembership(tenantId, memberId);
        if (membership.getRemainingCount() != null) {
            membership.restore();
            memberships.save(membership);
        }
    }

    @Override
    public void consume(Long tenantId, Long memberId) {
        // Entitlement is held at booking time for count-based memberships.
        activeMembership(tenantId, memberId);
    }

    private MemberMembership activeMembership(Long tenantId, Long memberId) {
        Instant now = clock.instant();
        return memberships.findAllByTenantIdAndMemberIdAndStatus(tenantId, memberId, MemberMembershipStatus.ACTIVE)
                .stream()
                .filter(m -> usable(m, now))
                .max(Comparator.comparing(MemberMembership::getStartsAt))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT));
    }

    private static boolean usable(MemberMembership membership, Instant now) {
        if (membership.getRemainingCount() != null && membership.getRemainingCount() <= 0) {
            return false;
        }
        return !now.isBefore(membership.getStartsAt())
                && (membership.getExpiresAt() == null || now.isBefore(membership.getExpiresAt()));
    }
}
