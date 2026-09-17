package com.gymmind.tenancy.application;

import com.gymmind.tenancy.domain.model.TenantQuota;

public record TenantQuotaView(
        Long tenantId,
        int maxAgents,
        int usedAgents,
        int maxAiCallsPerMonth,
        long usedAiCallsThisMonth,
        int maxKnowledgeDocuments,
        long usedKnowledgeDocuments,
        boolean aiModuleEnabled,
        boolean knowledgeModuleEnabled) {

    public static TenantQuotaView of(TenantQuota quota, long usedAgents, long usedAiCalls, long usedDocs) {
        return new TenantQuotaView(
                quota.getTenantId(),
                quota.getMaxAgents(),
                (int) usedAgents,
                quota.getMaxAiCallsPerMonth(),
                usedAiCalls,
                quota.getMaxKnowledgeDocuments(),
                usedDocs,
                quota.isAiModuleEnabled(),
                quota.isKnowledgeModuleEnabled());
    }
}
