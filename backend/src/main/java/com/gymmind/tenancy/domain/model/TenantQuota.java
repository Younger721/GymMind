package com.gymmind.tenancy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_tenant_quota")
public class TenantQuota {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "max_agents", nullable = false)
    private int maxAgents = 5;

    @Column(name = "max_ai_calls_per_month", nullable = false)
    private int maxAiCallsPerMonth = 1000;

    @Column(name = "max_knowledge_documents", nullable = false)
    private int maxKnowledgeDocuments = 100;

    @Column(name = "ai_module_enabled", nullable = false)
    private boolean aiModuleEnabled = true;

    @Column(name = "knowledge_module_enabled", nullable = false)
    private boolean knowledgeModuleEnabled = true;

    protected TenantQuota() {
    }

    private TenantQuota(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        this.tenantId = tenantId;
    }

    public static TenantQuota defaults(Long tenantId) {
        return new TenantQuota(tenantId);
    }

    public void update(int maxAgents, int maxAiCallsPerMonth, int maxKnowledgeDocuments,
                       boolean aiModuleEnabled, boolean knowledgeModuleEnabled) {
        if (maxAgents < 1 || maxAiCallsPerMonth < 1 || maxKnowledgeDocuments < 1) {
            throw new IllegalArgumentException("quota limits must be positive");
        }
        this.maxAgents = maxAgents;
        this.maxAiCallsPerMonth = maxAiCallsPerMonth;
        this.maxKnowledgeDocuments = maxKnowledgeDocuments;
        this.aiModuleEnabled = aiModuleEnabled;
        this.knowledgeModuleEnabled = knowledgeModuleEnabled;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public int getMaxAgents() {
        return maxAgents;
    }

    public int getMaxAiCallsPerMonth() {
        return maxAiCallsPerMonth;
    }

    public int getMaxKnowledgeDocuments() {
        return maxKnowledgeDocuments;
    }

    public boolean isAiModuleEnabled() {
        return aiModuleEnabled;
    }

    public boolean isKnowledgeModuleEnabled() {
        return knowledgeModuleEnabled;
    }
}
