package com.gymmind.tenancy.application;

import com.gymmind.shared.security.CurrentActor;

public interface TenantQuotaService {
    TenantQuotaView getForTenant(CurrentActor actor);

    TenantQuotaView getForPlatform(CurrentActor actor, Long tenantId);

    TenantQuotaView updateForPlatform(CurrentActor actor, Long tenantId, UpdateTenantQuotaCommand command);

    void ensureAgentCreationAllowed(Long tenantId);

    void ensureAiChatAllowed(Long tenantId);

    void ensureAiChatAllowed(CurrentActor actor);

    void ensureAiModuleEnabled(Long tenantId);

    void ensureKnowledgeModuleEnabled(Long tenantId);

    void ensureKnowledgeUploadAllowed(Long tenantId);
}
