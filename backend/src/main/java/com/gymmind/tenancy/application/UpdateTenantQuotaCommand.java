package com.gymmind.tenancy.application;

public record UpdateTenantQuotaCommand(
        int maxAgents,
        int maxAiCallsPerMonth,
        long maxAiTokensPerMonth,
        int maxKnowledgeDocuments,
        boolean aiModuleEnabled,
        boolean knowledgeModuleEnabled) {
}
