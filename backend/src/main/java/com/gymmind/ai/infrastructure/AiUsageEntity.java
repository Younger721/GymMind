package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_usage_record")
class AiUsageEntity extends AuditableEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
    @Column(name = "user_id", updatable = false)
    private Long userId;
    @Column(nullable = false, length = 100, updatable = false)
    private String model;
    @Column(name = "input_tokens", nullable = false, updatable = false)
    private int inputTokens;
    @Column(name = "output_tokens", nullable = false, updatable = false)
    private int outputTokens;
    @Column(name = "latency_ms", nullable = false, updatable = false)
    private long latencyMs;
    @Column(name = "provider_request_id", length = 200, updatable = false)
    private String providerRequestId;
    @Column(nullable = false, length = 32, updatable = false)
    private String status;

    protected AiUsageEntity() {
    }

    static AiUsageEntity from(AiUsageRecord record) {
        AiUsageEntity entity = new AiUsageEntity();
        entity.tenantId = record.tenantId();
        entity.userId = record.userId();
        entity.model = record.model();
        entity.inputTokens = record.inputTokens();
        entity.outputTokens = record.outputTokens();
        entity.latencyMs = record.latencyMs();
        entity.providerRequestId = record.providerRequestId();
        entity.status = record.status();
        return entity;
    }

    AiUsageRecord toRecord() {
        return new AiUsageRecord(tenantId, userId, model, inputTokens, outputTokens,
                latencyMs, providerRequestId, status, null);
    }
}
