package com.gymmind.ai.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataAiUsageRepository extends JpaRepository<AiUsageEntity, Long> {
    List<AiUsageEntity> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
