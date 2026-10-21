package com.gymmind.tenancy.application;

import com.gymmind.tenancy.domain.model.TenantQuota;

public record TenantQuotaView(
        Long tenantId,
        int maxAgents,
        int usedAgents,
        int maxAiCallsPerMonth,
        long usedAiCallsThisMonth,
        long maxAiTokensPerMonth,
        long usedAiTokensThisMonth,
        int maxKnowledgeDocuments,
        long usedKnowledgeDocuments,
        boolean aiModuleEnabled,
        boolean knowledgeModuleEnabled) {

    public static TenantQuotaView of(TenantQuota quota, long usedAgents, long usedAiCalls, long usedAiTokens, long usedDocs) {
        return new TenantQuotaView(
                quota.getTenantId(),
                1,
                (int) usedAgents,
                quota.getMaxAiCallsPerMonth(),
                usedAiCalls,
                quota.getMaxAiTokensPerMonth(),
                usedAiTokens,
                quota.getMaxKnowledgeDocuments(),
                usedDocs,
                quota.isAiModuleEnabled(),
                quota.isKnowledgeModuleEnabled());
    }
}
