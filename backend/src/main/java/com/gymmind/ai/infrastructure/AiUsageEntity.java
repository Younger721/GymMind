package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_usage_record")
class AiUsageEntity extends AuditableEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
    @Column(name = "user_id", updatable = false)
    private Long userId;
    @Column(nullable = false, length = 100, updatable = false)
    private String model;
    @Column(name = "input_tokens", updatable = false)
    private Integer inputTokens;
    @Column(name = "output_tokens", updatable = false)
    private Integer outputTokens;
    @Column(name = "total_tokens", updatable = false)
    private Integer totalTokens;
    @Column(name = "latency_ms", nullable = false, updatable = false)
    private long latencyMs;
    @Column(name = "provider_request_id", length = 200, updatable = false)
    private String providerRequestId;
    @Column(nullable = false, length = 32, updatable = false)
    private String status;
    @Column(length = 32, updatable = false)
    private String provider;
    @Column(length = 64, updatable = false)
    private String feature;
    @Column(name = "usage_source", length = 32, updatable = false)
    private String usageSource;
    @Column(name = "estimated_cost", precision = 18, scale = 6, updatable = false)
    private BigDecimal estimatedCost;
    @Column(length = 8, updatable = false)
    private String currency;

    protected AiUsageEntity() {
    }

    static AiUsageEntity from(AiUsageRecord record) {
        AiUsageEntity entity = new AiUsageEntity();
        entity.tenantId = record.tenantId();
        entity.userId = record.userId();
        entity.model = record.model();
        entity.inputTokens = record.inputTokens();
        entity.outputTokens = record.outputTokens();
        entity.totalTokens = record.totalTokens();
        entity.latencyMs = record.latencyMs();
        entity.providerRequestId = record.providerRequestId();
        entity.status = record.status();
        entity.provider = record.provider();
        entity.feature = record.feature();
        entity.usageSource = record.usageSource();
        entity.estimatedCost = record.estimatedCost();
        entity.currency = record.currency();
        return entity;
    }

    AiUsageRecord toRecord() {
        return new AiUsageRecord(tenantId, userId, model, inputTokens, outputTokens,
                latencyMs, providerRequestId, status, null, provider, feature, totalTokens,
                usageSource, estimatedCost, currency, getCreatedAt());
    }
}
