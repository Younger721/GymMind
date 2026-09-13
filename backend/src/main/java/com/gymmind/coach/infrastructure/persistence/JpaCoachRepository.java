package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository class JpaCoachRepository implements CoachRepository {
    private final SpringDataCoachRepository delegate;
    JpaCoachRepository(SpringDataCoachRepository delegate) { this.delegate = delegate; }
    public Coach save(Coach coach) { return delegate.save(coach); }
    public Optional<Coach> findByTenantIdAndId(Long tenantId, Long id) { return delegate.findByTenantIdAndId(tenantId, id); }
    public Optional<Coach> findByTenantIdAndUserId(Long tenantId, Long userId) { return delegate.findByTenantIdAndUserId(tenantId, userId); }
}
