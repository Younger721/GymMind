package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.ai.application.AiUsageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaAiUsageRepository implements AiUsageRepository {
    private final SpringDataAiUsageRepository delegate;

    public JpaAiUsageRepository(SpringDataAiUsageRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public AiUsageRecord save(AiUsageRecord record) {
        if (record == null || record.tenantId() == null) {
            throw new IllegalArgumentException("tenant-scoped usage record required");
        }
        return delegate.save(AiUsageEntity.from(record)).toRecord();
    }

    @Override
    public List<AiUsageRecord> findAllByTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        return delegate.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(AiUsageEntity::toRecord).toList();
    }
}
