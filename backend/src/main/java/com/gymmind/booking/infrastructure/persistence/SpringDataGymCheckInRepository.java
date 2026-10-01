package com.gymmind.booking.infrastructure.persistence;

import com.gymmind.booking.domain.model.GymCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

interface SpringDataGymCheckInRepository extends JpaRepository<GymCheckIn, Long> {
    List<GymCheckIn> findByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to);
    long countByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to);
}
