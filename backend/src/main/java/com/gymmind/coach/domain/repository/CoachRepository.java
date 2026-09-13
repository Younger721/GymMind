package com.gymmind.coach.domain.repository;

import com.gymmind.coach.domain.model.Coach;
import java.util.Optional;

public interface CoachRepository {
    Coach save(Coach coach);
    Optional<Coach> findByTenantIdAndId(Long tenantId, Long id);
    Optional<Coach> findByTenantIdAndUserId(Long tenantId, Long userId);
}
