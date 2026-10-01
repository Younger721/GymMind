package com.gymmind.ai.harness;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.ai.chat.application.AiRoleProfileResolver;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiHarnessPluginTest {

    @Test
    void tenantPluginSupportsPlatformActorOnlyWithSelectedTenant() {
        AiHarnessPlugin plugin = new AiHarnessPlugin() {
            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return new AiHarnessPluginDescriptor("tenant-tool", "Tenant tool", "", "test",
                        "tool:read", true, false);
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {}
        };
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        TenantContextHolder.clear();
        assertThat(plugin.supports(platform)).isFalse();
        TenantContextHolder.set(11L);
        try {
            assertThat(plugin.supports(platform)).isTrue();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void knowledgeSearchSupportsPlatformActorWithoutTenantContext() {
        KnowledgeSearchService search = (actor, query) -> List.of();
        AiHarnessPlugin plugin = new com.gymmind.ai.harness.plugins.KnowledgeSearchHarnessPlugin(search);
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        TenantContextHolder.clear();

        assertThat(plugin.supports(platform)).isTrue();
    }

    @Test
    void platformRoleProfileKeepsKnowledgeSearchEnabled() {
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        assertThat(new AiRoleProfileResolver().resolve(platform).enabledPlugins())
                .contains("knowledge_search");
    }
}
