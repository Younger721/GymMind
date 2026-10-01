package com.gymmind.tenancy.application;

import com.gymmind.shared.security.CurrentActor;

/**
 * Construction helpers for application services that are instantiated directly
 * by focused unit tests. The Spring managed constructors receive the real
 * {@link TenantQuotaService} bean and therefore never use this adapter.
 */
public final class TenantQuotaServices {

    private static final TenantQuotaService UNBOUNDED = new UnboundedTenantQuotaService();

    private TenantQuotaServices() {
    }

    /**
     * Returns a non-Spring compatibility implementation for legacy constructors.
     * It deliberately leaves quota checks open because those constructors are
     * retained only for isolated tests that do not assemble tenancy services.
     */
    public static TenantQuotaService unboundedForLegacyConstruction() {
        return UNBOUNDED;
    }

    private static final class UnboundedTenantQuotaService implements TenantQuotaService {
        @Override
        public TenantQuotaView getForTenant(CurrentActor actor) {
            throw unsupported();
        }

        @Override
        public TenantQuotaView getForPlatform(CurrentActor actor, Long tenantId) {
            throw unsupported();
        }

        @Override
        public TenantQuotaView updateForPlatform(CurrentActor actor, Long tenantId,
                                                  UpdateTenantQuotaCommand command) {
            throw unsupported();
        }

        @Override
        public void ensureAgentCreationAllowed(Long tenantId) {
            // Compatibility constructors do not assemble quota persistence.
        }

        @Override
        public void ensureAiChatAllowed(Long tenantId) {
            // Compatibility constructors do not assemble quota persistence.
        }

        @Override
        public void ensureKnowledgeUploadAllowed(Long tenantId) {
            // Compatibility constructors do not assemble quota persistence.
        }

        // These methods are also present when the module-level quota API is
        // enabled. Keeping them here makes the adapter forward-compatible with
        // that interface extension without affecting current callers.
        @Override
        public void ensureAiModuleEnabled(Long tenantId) {
            // Compatibility constructors do not assemble quota persistence.
        }

        @Override
        public void ensureKnowledgeModuleEnabled(Long tenantId) {
            // Compatibility constructors do not assemble quota persistence.
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Quota administration requires the Spring-managed service");
        }
    }
}
