package com.gymmind.ai.application;

import java.util.List;

public interface AiUsageRepository {
    AiUsageRecord save(AiUsageRecord record);

    default List<AiUsageRecord> findAll() {
        return List.of();
    }

    default List<AiUsageRecord> findAllByTenantId(Long tenantId) {
        return List.of();
    }
}
