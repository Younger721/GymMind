package com.gymmind.tenancy.application;

public record UpdateTenantQuotaCommand(
        int maxAgents,
        int maxAiCallsPerMonth,
        int maxKnowledgeDocuments,
        boolean aiModuleEnabled,
        boolean knowledgeModuleEnabled) {
}
