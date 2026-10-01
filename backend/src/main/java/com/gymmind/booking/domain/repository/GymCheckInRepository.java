package com.gymmind.booking.domain.repository;

import com.gymmind.booking.domain.model.GymCheckIn;
import java.time.Instant;
import java.util.List;

public interface GymCheckInRepository {
    GymCheckIn save(GymCheckIn checkIn);
    List<GymCheckIn> findByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to);
    long countByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to);
}
