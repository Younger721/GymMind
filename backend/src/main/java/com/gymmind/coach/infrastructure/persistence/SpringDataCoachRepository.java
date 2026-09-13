package com.gymmind.coach.infrastructure.persistence;
import com.gymmind.coach.domain.model.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface SpringDataCoachRepository extends JpaRepository<Coach, Long> {
    Optional<Coach> findByTenantIdAndId(Long tenantId, Long id);
    Optional<Coach> findByTenantIdAndUserId(Long tenantId, Long userId);
}
