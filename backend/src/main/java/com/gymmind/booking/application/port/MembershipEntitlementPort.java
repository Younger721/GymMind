package com.gymmind.booking.application.port;

/**
 * Bridges booking lifecycle with member membership entitlements.
 */
public interface MembershipEntitlementPort {

    /** Validates the member can book and holds entitlement when count-based. */
    void reserve(Long tenantId, Long memberId);

    /** Returns held entitlement when a booking is cancelled before check-in. */
    void release(Long tenantId, Long memberId);

    /** Finalizes entitlement consumption at check-in. */
    void consume(Long tenantId, Long memberId);
}
