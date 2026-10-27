package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ai_usage_record")
class AiUsageEntity extends AuditableEntity {
    @Column(name = "tenant_id", updatable = false)
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
        Integer normalizedInput = inputTokens;
        Integer normalizedOutput = outputTokens;
        String normalizedSource = usageSource;
        if (totalTokens == null && Objects.equals(inputTokens, 0) && Objects.equals(outputTokens, 0)
                && (usageSource == null || usageSource.isBlank())) {
            normalizedInput = null;
            normalizedOutput = null;
            normalizedSource = "UNAVAILABLE";
        }
        return new AiUsageRecord(tenantId, userId, model, normalizedInput, normalizedOutput,
                latencyMs, providerRequestId, status, null, provider, feature, totalTokens,
                normalizedSource, estimatedCost, currency, getCreatedAt());
    }
}
