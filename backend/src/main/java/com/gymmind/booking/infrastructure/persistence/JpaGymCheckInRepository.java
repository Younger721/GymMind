package com.gymmind.booking.infrastructure.persistence;

import com.gymmind.booking.domain.model.GymCheckIn;
import com.gymmind.booking.domain.repository.GymCheckInRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
class JpaGymCheckInRepository implements GymCheckInRepository {
    private final SpringDataGymCheckInRepository delegate;
    JpaGymCheckInRepository(SpringDataGymCheckInRepository delegate) { this.delegate = delegate; }
    public GymCheckIn save(GymCheckIn value) { return delegate.save(value); }
    public List<GymCheckIn> findByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to) {
        return delegate.findByTenantIdAndMemberIdAndCheckInAtBetween(tenantId, memberId, from, to);
    }
    public long countByTenantIdAndMemberIdAndCheckInAtBetween(Long tenantId, Long memberId, Instant from, Instant to) {
        return delegate.countByTenantIdAndMemberIdAndCheckInAtBetween(tenantId, memberId, from, to);
    }
}
